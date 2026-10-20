"""Loopback, authenticated, read-only bridge to a local linkedincli installation."""
import argparse
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import re
import secrets
import selectors
import signal
import subprocess
import time
from urllib.parse import urlsplit

MAX_OUTPUT = 1024 * 1024


def profile_slug(value):
    if not isinstance(value, str):
        raise ValueError('Use a LinkedIn profile URL.')
    p = urlsplit(value)
    if p.scheme != 'https' or p.hostname not in {'linkedin.com', 'www.linkedin.com'} or p.username or p.password or p.port or p.query or p.fragment or not re.fullmatch(r'/in/[A-Za-z0-9][A-Za-z0-9_%-]*/?', p.path):
        raise ValueError('Use a plain HTTPS LinkedIn /in/ profile URL.')
    return p.path.strip('/').split('/')[1]


class Unavailable(ValueError):
    pass


class Cli:
    def __init__(self, executable='linkedin', timeout=30):
        self.executable, self.timeout = executable, timeout

    def call(self, *args):
        # A fixed allowlist, no arbitrary commands, no writes, and no session material in the API.
        if not (args == ('profile', 'me') or (len(args) == 3 and args[:2] in [('profile', 'view'), ('messaging', 'conversation-with')])):
            raise Unavailable('This bridge only supports profile/history reads.')
        env = {k: v for k, v in os.environ.items() if k not in {'LINKEDIN_HTTP', 'LINKEDIN_FROM_CHROME'}}
        deadline = time.monotonic() + self.timeout
        raw = bytearray()
        try:
            with subprocess.Popen([self.executable, *args], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=env, start_new_session=True) as proc:
                try:
                    with selectors.DefaultSelector() as selector:
                        selector.register(proc.stdout, selectors.EVENT_READ)
                        while True:
                            if time.monotonic() >= deadline:
                                raise Unavailable('LinkedIn read timed out. Check the local session; no message was sent.')
                            if not selector.select(.1):
                                continue
                            chunk = os.read(proc.stdout.fileno(), 65536)
                            if not chunk:
                                break
                            raw.extend(chunk)
                            if len(raw) > MAX_OUTPUT:
                                raise Unavailable('LinkedIn response exceeded the limit.')
                    if proc.wait(timeout=max(.1, deadline-time.monotonic())):
                        raise Unavailable('LinkedIn session/read failed. Check the companion computer; no message was sent.')
                finally:
                    if proc.poll() is None:
                        os.killpg(proc.pid, signal.SIGKILL)
                        proc.wait()
            data = json.loads(raw)
            if not isinstance(data, dict) or 'error' in data:
                raise Unavailable('LinkedIn returned an unsupported response.')
            return data
        except (OSError, subprocess.SubprocessError, UnicodeError, json.JSONDecodeError):
            raise Unavailable('Companion read failed. Check the installed CLI and session on your computer.') from None


def person(data, slug=None):
    # Accept explicit profile records only; never infer identity from arbitrary nested text.
    root = data.get('data', {})
    if not isinstance(root, dict): root = {}
    records = [data, root, data.get('miniProfile', {}), root.get('miniProfile', {}), *data.get('included', [])]
    matches = []
    for row in records:
        if not isinstance(row, dict):
            continue
        if slug is not None and str(row.get('publicIdentifier', '')).lower() != slug.lower():
            continue
        urn = row.get('entityUrn', '')
        name = ' '.join(str(row.get(k, '')).strip() for k in ('firstName', 'lastName')).strip()
        if re.fullmatch(r'urn:li:(?:fs_miniProfile|fsd_profile|fs_profile):[A-Za-z0-9_-]+', str(urn)) and name:
            matches.append(dict(name=name[:250], urn=urn.rsplit(':', 1)[1], slug=row.get('publicIdentifier', '')))
    unique = {p['urn']: p for p in matches}
    if len(unique) != 1:
        raise Unavailable('Profile identity could not be verified. Check LinkedIn manually.')
    return next(iter(unique.values()))


def history(data):
    root = data.get('data', data)
    if not isinstance(root, dict):
        raise Unavailable('History format is unknown. Do not assume no previous chat.')
    elements = root.get('elements', root.get('*elements'))
    paging = root.get('paging')
    if not isinstance(elements, list) or not isinstance(paging, dict) or type(paging.get('total')) is not int or paging['total'] < 0:
        raise Unavailable('History is incomplete or unsupported. Check the chat manually.')
    if elements or paging['total'] > 0:
        return 'prior_contact'
    if paging.get('start') != 0:
        raise Unavailable('History begins partway through the results. Check manually.')
    return 'not_found_unverified'  # NOT permission to send: deleted/archived history can be missing.


class Bridge:
    def __init__(self, cli):
        self.cli = cli

    def account(self):
        account = person(self.cli.call('profile', 'me'))
        return dict(name=account['name'], account_id=account['urn'], mode='read_only')

    def check(self, profile):
        account = self.account()
        slug = profile_slug(profile)
        target = person(self.cli.call('profile', 'view', slug), slug)
        if target['urn'] == account['account_id']:
            raise Unavailable('Choose another person, not your own profile.')
        state = history(self.cli.call('messaging', 'conversation-with', target['urn']))
        return dict(profile='https://www.linkedin.com/in/'+slug, name=target['name'], account_id=account['account_id'], state=state,
                    checked_at=int(time.time()), message='Recorded conversation found: exclude this person.' if state == 'prior_contact' else 'No conversation found in available results. This does not prove no previous contact; verify archived/history in LinkedIn before copying.')


def handler(bridge, token):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args): pass
        def reply(self, status, value):
            raw = json.dumps(value).encode()
            self.send_response(status)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Content-Length', str(len(raw)))
            self.send_header('Cache-Control', 'no-store')
            self.end_headers(); self.wfile.write(raw)
        def authorized(self):
            return hmac.compare_digest(self.headers.get('Authorization', ''), 'Bearer '+token)
        def do_GET(self):
            if not self.authorized(): return self.reply(401, dict(error='Companion token required.'))
            if self.path != '/v1/account': return self.reply(404, dict(error='Not found.'))
            try: self.reply(200, bridge.account())
            except ValueError as exc: self.reply(502, dict(error=str(exc)))
            except Exception: self.reply(502, dict(error='Companion unavailable; no message was sent.'))
        def do_POST(self):
            if not self.authorized(): return self.reply(401, dict(error='Companion token required.'))
            if self.path != '/v1/check': return self.reply(404, dict(error='Read-only bridge; sending is not supported.'))
            try:
                length = int(self.headers.get('Content-Length', '0'))
                if not 0 < length <= 4096 or self.headers.get_content_type() != 'application/json':
                    raise ValueError('Send a small JSON object.')
                data = json.loads(self.rfile.read(length))
                if not isinstance(data, dict): raise ValueError('Send a JSON object.')
                self.reply(200, bridge.check(data.get('profile')))
            except ValueError as exc: self.reply(400, dict(error=str(exc)))
            except Exception: self.reply(502, dict(error='Companion read failed; check the local session.'))
    return Handler


def main():
    os.umask(0o077)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, default=8770)
    parser.add_argument('--cli', default='linkedin')
    parser.add_argument('--token-file', type=Path, default=Path('~/.config/friday/linkedin-bridge-token').expanduser())
    args = parser.parse_args()
    args.token_file.parent.mkdir(parents=True, exist_ok=True)
    if not args.token_file.exists():
        fd = os.open(args.token_file, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, 'w') as out: out.write(secrets.token_urlsafe(32))
    args.token_file.chmod(0o600)
    token = args.token_file.read_text().strip()
    if not 32 <= len(token) <= 256 or any(ord(c) < 33 or ord(c) > 126 for c in token): parser.error('Use a private ASCII token of at least 32 characters.')
    server = ThreadingHTTPServer(('127.0.0.1', args.port), handler(Bridge(Cli(args.cli)), token))
    print(f'Read-only companion: 127.0.0.1:{args.port}; token file: {args.token_file}. Expose only through your authenticated HTTPS setup.')
    try: server.serve_forever()
    except KeyboardInterrupt: pass
    finally: server.server_close()


if __name__ == '__main__': main()
