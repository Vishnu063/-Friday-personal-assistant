# Optional live LinkedIn checks (experimental)

Friday 0.6 can connect to a **read-only companion on your own computer** to verify its account, look up a profile and check available conversation results. This avoids CSV imports for that check. It is not an official LinkedIn integration, does not collect a LinkedIn password in Friday and **does not send messages**.

The companion uses [bcharleson/linkedincli](https://github.com/bcharleson/linkedincli), reviewed at revision `c091ba1a59bcbfc2f95737293c3fdb954ab2f909` (source package version 0.1.6; that version is not published on npm). Its session stays on the computer. Its maintainers require live sessions to run locally rather than on cloud VMs/EC2. Node.js is needed there; no Node runtime or model is added to the Android APK. The computer must be awake for live checks. Voice, tasks and other phone tools still work without it.

## One-time computer setup

Use Linux or macOS with Node.js 24 or newer and Python 3.10+ (the bridge uses POSIX subprocess controls). Build the reviewed revision locally on that computer (Git and npm are also required):

```bash
git clone https://github.com/bcharleson/linkedincli.git "$HOME/.local/share/friday-linkedin-cli"
git -C "$HOME/.local/share/friday-linkedin-cli" checkout c091ba1a59bcbfc2f95737293c3fdb954ab2f909
(cd "$HOME/.local/share/friday-linkedin-cli" && npm ci && npm run build)
chmod +x "$HOME/.local/share/friday-linkedin-cli/dist/index.js"
"$HOME/.local/share/friday-linkedin-cli/dist/index.js" login
```

Complete the CLI's own local session setup. Never paste LinkedIn cookies/passwords into ChatGPT, Friday, GitHub issues, a cloud runner or this repository. If LinkedIn requires verification or refuses the session, stop and use LinkedIn directly; this integration does not bypass challenges or add fingerprint-changing transports.

From the Friday checkout, start the bridge:

```bash
python3 -m companion.linkedin_bridge --cli "$HOME/.local/share/friday-linkedin-cli/dist/index.js"
```

It binds to **127.0.0.1:8770 only** and creates a private token file at `~/.config/friday/linkedin-bridge-token`. The token is not printed. Each authenticated request can invoke only three fixed read commands: `profile me`, `profile view <slug>` and `messaging conversation-with <id>`. No arbitrary CLI commands or `/send` endpoint exist. No session values or raw upstream responses enter its HTTP output. Access logs are disabled; timeouts, malformed output and response size limits stop the check.

Make the loopback service reachable from your phone through **your own trusted HTTPS reverse proxy/VPN setup** with a certificate Android trusts. Keep the backend bound to loopback and keep the companion token private. The project does not deploy a public service, change firewall rules, turn off certificate checks or create a tunnel automatically. This network setup remains an owner step; a phone cannot reach the computer's loopback address directly.

## Connect Friday once

1. Open **Tools → Career dashboard → LinkedIn referral assistant → Optional live LinkedIn check**.
2. Enter the companion's HTTPS origin (for example `https://your-companion-host`) and the **companion token**, not a LinkedIn credential. No URL paths/queries/embedded credentials are accepted.
3. Tap **Connect and verify account** and verify the displayed account. Friday stores the companion token encrypted using Android Keystore, separately from its backups and AI prompts.
4. Paste one LinkedIn profile URL and tap **Check available chat history**. Profile identity and the connected account must match the response.

A found conversation means **exclude the person**. A zero-result lookup says **not found, unverified**, never “safe to send.” Archived/deleted history and unsupported API coverage may be missing; errors, incomplete paging, changed accounts and unknown shapes must not be treated as no previous contact. This result never enables automatic sending or overrides CSV/locally recorded contact states. Sending and final live-history review stay in LinkedIn.

**Disconnect Friday** removes this phone's saved companion connection. It does not log the computer out of LinkedIn or revoke a shared server token; stop the bridge/rotate its token separately if needed. **Erase personal data** also removes the saved phone connection. Phone app data loss/reinstall loses local duplicate tracking as described in the referral guide.

## Why sending is absent

The reviewed CLI retries some 5xx/network failures in its shared transport, including paths used for writes. Its participant lookup does not establish complete coverage of archived/deleted conversations. Adding a password screen, interpreting every empty/error response as “never chatted,” or routing message sends through this transport would not meet the requested exclusion/duplicate rule. A future send adapter needs independently verified history scope, no ambiguous write retry, durable submission/read-back handling and a live owner-account trial. No automatic sends were enabled or performed here.

## Verification

Python tests exercise actual local HTTP authentication, rejected send routes, strict identity/history parsing, subprocess timeouts, oversized/malformed output and secret redaction using fixtures. Android tests validate HTTPS-origin binding, account/recipient mismatch rejection and conservative history states. These tests do not authenticate a live LinkedIn account or prove compatibility with its changing unofficial endpoints. Pairing/Keystore behavior, the owner's HTTPS setup and a live account check still require a device/computer trial.
