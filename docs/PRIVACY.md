# Privacy and action boundaries

Friday's chat model runs on your phone. There is no cloud LLM client or chat-upload feature. **v0.2 adds internet permission for an optional model-file download that you explicitly review and start; third-party OCR SDK networking must also be considered.** Friday implements no background model download, chat/screen/resume uploader or app analytics client. However, Google ML Kit collects device/app information and SDK performance/usage metrics under its own terms. Its OCR image inputs and recognised outputs are processed on-device and are not sent to Google according to its disclosure: https://developers.google.com/ml-kit/android-data-disclosure and https://developers.google.com/ml-kit/terms. The app explains this before first OCR use. SDK initialisation behaviour and Android providers are outside the local chat model; this is not a zero-telemetry guarantee. Android speech/TTS, browsers and apps you open have their own provider policies.

## Private chat

The Chat privacy switch clears the displayed session, excludes new chat turns from SQLite history and excludes saved preferences/recent saved chat from those AI prompts. Turning privacy off restores the saved chat view. Private messages remain temporarily in process memory while displayed; this is not secure memory wiping. Selecting explicit Save actions still persists the selected note/task/preference/practice entry. Export contains persisted data only. Private document/screen explanations also remain excluded from saved history and subsequent context.

## Stored data and recovery

Private SQLite stores active notes/tasks/preferences/jobs/checkpoints/practice and up to 100 saved chat messages. Settings/resume and imported AI model stay in private app storage. Cloud backup is disabled. This is not an independently encrypted vault.

Delete moves an item to trash and cancels its task alarm. Undo/Restore recovers it and schedules future reminders. Trashed items are excluded from AI preferences and JSON backups; they remain locally recoverable until Empty trash or Erase personal data. Erase clears all items, trash, chats and resume; model removal is separate. Exported files and external-app copies need separate deletion.

JSON backups are plaintext. Restore validates size, schema, item kinds, lengths, chat roles and limits before any database mutation, previews the merge and imports in a transaction with duplicate checks. Models, keys, Android permissions, network endpoints and executable settings are never restored. A non-empty restored resume replaces the saved resume after confirmation. Future restored task reminders are scheduled only after confirmation, subject to Android permission and battery rules.

## Explicit model download

Friday's own model transfer starts only from Settings → Review download → Download. OCR SDK diagnostic networking is separate as disclosed above. Use trusted HTTPS model-file URLs and preferably a provider SHA-256. Credentials are not attached to requests. HTTPS redirects are bounded and revalidated; failed, cancelled, oversized or checksum-mismatched transfers are discarded. A completed download replaces the private imported model; actual runtime compatibility still needs the on-phone test. A missing expected hash means no independent integrity verification. Gated models should use a browser download + file import; no provider login/token handling is implemented. Model providers/CDNs see your IP and request URL. Mobile-data charges may apply.

## Voice

Voice starts after a user action. Optional background conversation uses a microphone foreground service with a visible Stop notification; app lock disables background mode. There is no wake word. Delayed recognition/playback callbacks are ignored after stop/new sessions. Strict offline mode requires on-device Android recognition and an installed offline TTS voice, otherwise the app asks you to type/install a voice. Availability of an Android language pack remains device/provider dependent. In normal mode providers may use the internet; read-aloud sends text to your selected TTS engine. This system-service voice is not a guarantee of ChatGPT voice quality.

## Screen and phone actions

Android Accessibility permission is persistent until revoked, but Friday reads text only after a fresh device-authenticated, 60-second one-shot grant and a user accessibility-shortcut tap. No text is read in screen-event callbacks. Password nodes are skipped; other sensitive visible text may still appear. Review/edit the preview before explaining or reading aloud. No screenshots, gestures, keylogging or automatic clicking are performed.

Lock and power-menu actions have an explicit preview followed by fresh Android PIN/password/strong-biometric confirmation. Power off still needs your tap in Android's menu. Spoken-password phone commands are rejected before chat persistence. Model output and imported files cannot enter the direct command router, and the model has no executable tools.

Calls/SMS open the dialer/composer for final user confirmation. Career search opens your browser; job postings are not automatically fetched or applied to. Project checkpoints are manually saved, not silently read from GitHub.

## v0.3 app lock, sharing and backups

App lock gates the UI using device authentication and blocks screenshots/recents. It is not database encryption. Android Share content is temporary until explicitly saved; it never enters the command router automatically. Encrypted exports use password-based AES-256-GCM with random salt/nonce and PBKDF2-HMAC-SHA256 (210,000 iterations). Passwords are not persisted and cannot be recovered. Plain JSON export remains available as an advanced option. Backup imports do not grant permissions or enable app lock. See [v0.3 details](VERSION_0_3.md).

## v0.4 dictation

Voice results are reviewed before submission by default. Partial/final text and edits remain temporary until Send; no command runs from the live transcript. Disabling review permits recognised text to enter the existing direct command router. Literal vocabulary replacements and optional English filler cleanup can alter text, so review is recommended. Language/spelling suggestions are sent to the selected Android recognizer as part of recognition; its policies apply.

Optional approved-transcript history is off by default, limited to 20 entries and 1 or 7 days. It uses private app preferences, is not independently encrypted, and is excluded from backup exports. Retention is pruned at startup/refresh/save, not by continuous background deletion. Private chat skips new transcript history and hides existing entries; explicit deletion is still available. Individual/all deletion and Erase personal data remove transcript history; Erase also removes custom vocabulary. This does not securely overwrite flash or delete external copies. Friday stores no raw microphone recording. Review drafts are cleared when app lock locks the app; ordinary backgrounding may retain a draft in process memory.

Audio focus is requested only during recognition if enabled and abandoned on result/error/cancel. It does not change global media volume. System recognition controls hardware routing. AI idle-unload options change RAM/startup tradeoffs, not data upload policy.


## Referral review (0.5)

User-selected LinkedIn CSV files are processed locally. Raw CSV and message bodies are not saved. Normalized participant names/profile URLs, imported connection details, import times and prepared/sent/excluded states are held in a separate private SQLite database. They are not sent to AI or included in Friday backups. Erase personal data clears this database. Reviewed drafts are copied to the system clipboard only after your confirmation; you send in LinkedIn. Friday does not read LinkedIn account sessions, log in, scrape conversations or click Send. Reinstalling/erasing data loses local duplicate protection; reimport history and check the live conversation before contacting anyone.
