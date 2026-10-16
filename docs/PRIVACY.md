# Privacy and action boundaries — v0.2

Friday's chat model runs on your phone. There is no cloud LLM client or chat-upload feature. **v0.2 adds internet permission solely for an optional model-file download that you explicitly review and start.** No background model download, analytics client or chat/screen/resume upload is implemented. Android speech/TTS, browsers and apps you open have their own provider policies.

## Private chat

The Chat privacy switch clears the displayed session, excludes new chat turns from SQLite history and excludes saved preferences/recent saved chat from those AI prompts. Turning privacy off restores the saved chat view. Private messages remain temporarily in process memory while displayed; this is not secure memory wiping. Selecting explicit Save actions still persists the selected note/task/preference/practice entry. Export contains persisted data only. Private document/screen explanations also remain excluded from saved history and subsequent context.

## Stored data and recovery

Private SQLite stores active notes/tasks/preferences/jobs/checkpoints/practice and up to 100 saved chat messages. Settings/resume and imported AI model stay in private app storage. Cloud backup is disabled. This is not an independently encrypted vault.

Delete moves an item to trash and cancels its task alarm. Undo/Restore recovers it and schedules future reminders. Trashed items are excluded from AI preferences and JSON backups; they remain locally recoverable until Empty trash or Erase personal data. Erase clears all items, trash, chats and resume; model removal is separate. Exported files and external-app copies need separate deletion.

JSON backups are plaintext. Restore validates size, schema, item kinds, lengths, chat roles and limits before any database mutation, previews the merge and imports in a transaction with duplicate checks. Models, keys, Android permissions, network endpoints and executable settings are never restored. A non-empty restored resume replaces the saved resume after confirmation. Future restored task reminders are scheduled only after confirmation, subject to Android permission and battery rules.

## Explicit model download

Only Settings → Review download → Download starts network transfer. Use trusted HTTPS model-file URLs and preferably a provider SHA-256. Credentials are not attached to requests. HTTPS redirects are bounded and revalidated; failed, cancelled, oversized or checksum-mismatched transfers are discarded. A completed download replaces the private imported model; actual runtime compatibility still needs the on-phone test. A missing expected hash means no independent integrity verification. Gated models should use a browser download + file import; no provider login/token handling is implemented. Model providers/CDNs see your IP and request URL. Mobile-data charges may apply.

## Voice

Foreground voice only; no microphone service or wake word. Delayed recognition/playback callbacks are ignored after stop/new sessions. Strict offline mode requires on-device Android recognition and an installed offline TTS voice, otherwise the app asks you to type/install a voice. Availability of an Android language pack remains device/provider dependent. In normal mode providers may use the internet; read-aloud sends text to your selected TTS engine. This system-service voice is not a guarantee of ChatGPT voice quality.

## Screen and phone actions

Android Accessibility permission is persistent until revoked, but Friday reads text only after a fresh device-authenticated, 60-second one-shot grant and a user accessibility-shortcut tap. No text is read in screen-event callbacks. Password nodes are skipped; other sensitive visible text may still appear. Review/edit the preview before explaining or reading aloud. No screenshots, gestures, keylogging or automatic clicking are performed.

Lock and power-menu actions have an explicit preview followed by fresh Android PIN/password/strong-biometric confirmation. Power off still needs your tap in Android's menu. Spoken-password phone commands are rejected before chat persistence. Model output and imported files cannot enter the direct command router, and the model has no executable tools.

Calls/SMS open the dialer/composer for final user confirmation. Career search opens your browser; job postings are not automatically fetched or applied to. Project checkpoints are manually saved, not silently read from GitHub.
