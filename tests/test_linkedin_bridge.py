import json
from http.server import ThreadingHTTPServer
import os
from pathlib import Path
import tempfile
import threading
import unittest
from urllib import request, error
from companion.linkedin_bridge import Bridge, Cli, Unavailable, handler, history, person, profile_slug


def profile(slug, urn):
    return dict(publicIdentifier=slug, entityUrn='urn:li:fs_miniProfile:'+urn, firstName=slug, lastName='Example')


class FakeCli:
    def __init__(self): self.calls = []; self.prior = False
    def call(self, *args):
        self.calls.append(args)
        if args == ('profile', 'me'): return dict(data=dict(miniProfile=profile('owner', 'OWNER')))
        if args[:2] == ('profile', 'view'): return dict(included=[profile(args[2], 'TARGET')])
        return dict(data={'*elements': ['existing'] if self.prior else [], 'paging': dict(start=0, total=int(self.prior))})


class BridgeTests(unittest.TestCase):
    def test_verified_identity_and_conservative_history(self):
        cli = FakeCli(); bridge = Bridge(cli)
        result = bridge.check('https://www.linkedin.com/in/mira')
        self.assertEqual(result['state'], 'not_found_unverified')
        cli.prior = True
        self.assertEqual(bridge.check('https://www.linkedin.com/in/mira')['state'], 'prior_contact')
        self.assertTrue(all(c[:2] in [('profile','me'), ('profile','view'), ('messaging','conversation-with')] for c in cli.calls))
        for value in ({}, dict(data={}), dict(data={'elements': [], 'paging': {'total':0,'start':20}}), dict(data={'elements': [], 'paging': {'total':False,'start':0}})):
            with self.assertRaises(Unavailable): history(value)
        with self.assertRaises(Unavailable): person({'included':[profile('a','A'), profile('a','B')]}, 'a')
        with self.assertRaises(Unavailable): person({'included':[profile('other','B')]}, 'mira')

    def test_urls_and_write_commands_are_rejected(self):
        for value in ['https://evil.test/in/name', 'https://user:secret@www.linkedin.com/in/name', 'https://www.linkedin.com/in/name?secret=x', 'http://www.linkedin.com/in/name', None]:
            with self.assertRaises(ValueError): profile_slug(value)
        with self.assertRaises(Unavailable): Cli().call('messaging','send-new','recipient')

    def test_real_http_authentication_and_no_send_endpoint(self):
        cli = FakeCli()
        server = ThreadingHTTPServer(('127.0.0.1', 0), handler(Bridge(cli), 'test-token-which-is-private-and-long'))
        threading.Thread(target=server.serve_forever, daemon=True).start()
        self.addCleanup(server.server_close); self.addCleanup(server.shutdown)
        base = 'http://127.0.0.1:'+str(server.server_port)
        with self.assertRaises(error.HTTPError) as denied: request.urlopen(base+'/v1/account')
        self.assertEqual(denied.exception.code, 401); self.assertEqual(cli.calls, [])
        headers={'Authorization':'Bearer test-token-which-is-private-and-long', 'Content-Type':'application/json'}
        with request.urlopen(request.Request(base+'/v1/account', headers=headers)) as r:
            data=json.load(r); self.assertEqual(data['mode'],'read_only'); self.assertNotIn('token',str(data))
        with request.urlopen(request.Request(base+'/v1/check', data=json.dumps(dict(profile='https://www.linkedin.com/in/mira')).encode(), headers=headers)) as r:
            self.assertEqual(json.load(r)['state'],'not_found_unverified')
        before=len(cli.calls)
        with self.assertRaises(error.HTTPError) as absent: request.urlopen(request.Request(base+'/v1/send', data=b'{}',headers=headers))
        self.assertEqual(absent.exception.code,404); self.assertEqual(len(cli.calls),before)

    def test_real_subprocess_timeout_and_invalid_output_are_masked(self):
        with tempfile.TemporaryDirectory() as root:
            path=Path(root)/'fixture'
            for body in ['print("not JSON secret-cookie")', 'import time; time.sleep(2)', 'import sys; print("secret-cookie",file=sys.stderr); sys.exit(1)', 'print("x"*1100000)']:
                path.write_text('#!/usr/bin/env python3\n'+body+'\n'); path.chmod(0o700)
                with self.assertRaises(Unavailable) as failure: Cli(str(path), timeout=.2).call('profile','me')
                self.assertNotIn('secret-cookie', str(failure.exception))
            path.write_text('#!/usr/bin/env python3\nprint(\'{"name":"fixture"}\')\n'); path.chmod(0o700)
            self.assertEqual(Cli(str(path)).call('profile','me'), dict(name='fixture'))
