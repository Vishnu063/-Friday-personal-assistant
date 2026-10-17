# Friday 0.3 — simpler and lighter

Home has Talk, Type, My day, Notes and Tools. The bottom bar has Home, Tasks and Tools; Settings and all existing career, coach, project and screen tools remain accessible.

## Everyday use

- Say/type “What time is it?”, “Show my notes”, “What do I have today?” or “Help”. These requests, notes, tasks and phone commands run without loading AI.
- In Settings, pin Talk or Note to your launcher. Talk opens chat; tap the mic to start listening.
- Use another app's Share menu → Friday for text, PDFs or images. Shared material is temporary and reviewed before saving or summarising. It never becomes a phone command automatically.
- Enable App lock in Settings using Android's device authentication. Friday relocks when it leaves the foreground. Unlock with the device PIN/password or supported biometrics. Lock also blocks screenshots and recent-app previews. Each phone control still requires fresh authorisation.
- Export an encrypted backup with a password of at least 10 characters. Choose a strong unique password and keep it safe: it cannot be recovered. Restore asks for the password, validates the contents and shows a merge preview. Older JSON backups remain supported; plaintext export is an advanced option.

## Memory policy

Compact mode is on by default: 2,048 model context tokens, replies capped at 256 tokens, and question text capped at 3,200 characters. Balanced mode uses 4,096 / 512 / 6,000. A smaller context may omit older conversation details and shorten answers.

The AI model loads only for AI chat or a compatibility test. It releases after 45 seconds idle, on backgrounding or a memory-pressure signal; generation is stopped before release. Reopening AI may therefore take longer. The model file stays on storage. Memory savings depend on the native runtime and phone and have not been measured on physical hardware.

Saved lists query up to 200 entries when opened; chat starts with 40 messages and trash with 50 entries. SQL search can find older saved items (up to 200 results; practice 100). Refine search terms if a query reaches its result limit. AI retrieves up to four relevant preferences from the latest 64, with a bounded character budget; it does not load every note into a prompt. No background listening; recognition and speech engines are created on demand and released after use/backgrounding.

## Security scope

App lock is a UI access gate, not database encryption. Data remains in Android app-private storage. Encrypted export uses AES-256-GCM, a fresh random salt/nonce, and PBKDF2-HMAC-SHA256 with 210,000 iterations. Passwords are not saved; working character arrays are cleared, but managed-runtime copies cannot be guaranteed wiped. Backup files exclude models, permissions and lock settings. Losing your password means losing access to that encrypted backup.

Phone-specific biometric, Share, launcher, speech and AI behaviour still needs device testing. No guarantee of ChatGPT-level voice or model quality. Stable release signing still requires owner-provided private secrets.
