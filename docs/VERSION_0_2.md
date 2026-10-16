# Using Friday 0.2

## Upgrade

Version code is 2. Export your v0.1 data first. A preview APK from a different runner may use a different debug signing key; Android then requires uninstalling the old preview before installing. Restore the exported JSON afterward; import your model again. If signatures already match, Android can update in place and the database migrates automatically from v1 to v2.

Consistently signed updates require the private-key setup in [RELEASE_SIGNING.md](RELEASE_SIGNING.md). The prepared workflow does not mean signing secrets are already installed.

## Daily tools

- Chat → privacy switch: do not save new chat history or inject saved memory; explicit Save commands still store items.
- Delete → Undo immediately, or Settings → Recover deleted items. Empty trash is permanent and asks for confirmation.
- Settings → Export / Restore: choose a JSON backup, review counts and merge. Existing data stays; duplicates are skipped. Backups include active items/chats/resume, not models, trash or signing keys. Maximum restore size 8 MB, 2,000 items and 100 chat messages. Do not edit backups into executable commands; imported text is never routed as an action.
- Memory → search, edit, delete or restore preferences. Deleted preferences are not sent to AI.
- Tasks → overdue count, Reschedule/reopen and battery-settings link. Delivery remains approximate; use Diagnostics to schedule an approximately two-minute test notification.

## AI setup

Settings provides a RAM-based starting suggestion, not a promise that a model will run. Import a trusted compatible `.litertlm` text model and watch copy progress, or explicitly download from a direct HTTPS file URL. Supply the provider's expected SHA-256 whenever available. Gated model websites need browser download/import instead of a URL requiring login. Use Cancel while a copy/download is running; blocked network reads may take up to the read timeout to stop.

Run **Test model compatibility / speed** to check one short generation and display first-text/total elapsed time. This does not assess answer quality, heating, long-session stability or every supported model. Android low-memory mode blocks new inference; try a smaller model or close other apps.

## Voice

Settings → strict offline recognition and TTS prevents normal provider fallback when no on-device recognizer/offline voice is available. In normal mode Android's provider can use the internet. Adjust speech speed; tap mic to interrupt. Interview coach has **Speak your answer**, then lets you review recognised text before feedback. This is foreground voice, not continuous background listening or a custom neural voice.

## Your career and projects

Tools → **Career dashboard** defaults to Pune. Search in your browser, paste a job description, compare it with your saved resume, save company/role/link and update Saved/Applied/Interview/Offer/Closed. Add a dated follow-up task. Matching is a transparent keyword checklist, never a hiring probability; no application is submitted automatically.

Tools → **Interview coach** asks one practical Linux/Kubernetes/AWS/CI-CD/Terraform/security scenario at a time. Speak/type an answer, compare with a checklist and save for revision. Optional local AI feedback is a suggestion and may be wrong. Six offline scenarios are included; freeform local AI can extend practice.

Tools → **Project memory** stores repository link, last checkpoint and next task. Update after each session or add your next step to Tasks. It does not connect your GitHub account or automatically inspect repositories.

Tools → **Diagnostics** checks model readiness, permissions, speech services, battery/storage/RAM and provides a reminder-delivery test. No diagnostics upload is implemented. Real phone testing remains necessary.
