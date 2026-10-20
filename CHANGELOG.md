# 0.4.0

## 0.5.0 — 20 October 2026

- Added local LinkedIn referral review: bounded CSV import, recorded-history exclusions in either direction, identity checks, editable drafts and persistent prepared/sent/excluded states. Actual sending stays in LinkedIn.
- Fixed speech-engine initialization being lost when stopped, retry after engine initialization failure and long-reply truncation. Added Unicode-safe chunk playback and cancellation coverage.
- Added voice preview, visible diagnostic status and Stop controls. Device audio quality remains unverified; automated speech tests use Android simulations.

- Reviewed/editable voice dictation by default; live partial text where the provider supports it.
- Tap/hold mic controls and explicit phrase collection for long dictation.
- Personal vocabulary biasing (provider dependent), literal replacements and optional filler cleanup.
- Optional 20-entry transcript history, retention, private-mode exclusion and deletion; no raw audio storage.
- Optional recording sounds/audio focus; 5-second/45-second/5-minute idle AI unloading.
- Document Android headset/provider limits; no bundled desktop voice model or new paid API.
- Regression coverage for text boundaries, literal replacements, bounds, persistence, privacy and retention.

# 0.3.0

- Simple Home and three main navigation destinations.
- Device-authenticated app lock, Share to Friday and launcher shortcuts.
- Authenticated password-encrypted backup/restore; legacy JSON support.
- Compact context, relevant bounded preferences, bounded lists and database search.
- Lazy AI/voice loading, idle/background AI release, additional deterministic commands.
- Added crypto, memory-budget, command and SQLite retrieval regression tests.

# Changelog

## 0.2.0 — Personal workflow and reliability update

- Private chat switch with no saved history or saved-memory injection; explicit Save actions remain explicit persistence.
- Recoverable deletion, immediate Undo, trash recovery, confirmed permanent trash deletion.
- Complete backup validation, restore preview, transactional merge/deduplication, future reminder restoration; v1 backups supported.
- Search/review/edit/forget notes and preferences. Deleted items excluded from inference and backup.
- Phone action preview before platform authentication; editable screen-text review before local explanation.
- Stale voice/TTS callback protection, strict offline-recognizer/TTS option, speech-speed control and interview answer dictation.
- Model import progress/cancellation; optional explicit HTTPS model download, redirects, optional expected SHA-256, disk bounds, cancellation and progress.
- Device RAM-based starting suggestion; explicit compatibility/latency test and low-memory refusal. No guarantee of all-model compatibility.
- Diagnostics for device memory/storage/battery, speech services, permissions and reminder delivery testing.
- Overdue tasks, reschedule/reopen, Android battery controls and future alarm recovery on app start/reboot.
- Career dashboard: user-selected location (Pune default), browser search, resume keyword comparison, company/role/URL/JD, five application stages, follow-up tasks.
- Interview coach: six practical scenarios, spoken answers, transparent checklist feedback, optional local AI review and saved revision notes.
- Project checkpoints: repository link, last progress, next task, editable checkpoint and add-next-step-to-task action; no automatic GitHub sync claimed.
- AI suggestion labels and stronger uncertainty instructions; no fabricated hiring probabilities or numeric confidence.
- v1 → v2 database migration preserving existing data.
- Prepared private-key signed-release workflow and signing guide. Private signing secrets have NOT been provisioned; stable signing still needs owner setup.

Physical-device acceptance remains required. This release does not claim ChatGPT-equivalent speech, unattended shutdown, autonomous job applications or unrestricted app control.
