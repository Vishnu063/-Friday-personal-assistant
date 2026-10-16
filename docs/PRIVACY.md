# Privacy and action boundaries

Friday has no analytics SDK, cloud LLM client or direct network permission. LiteRT-LM runs the imported model on-device. Android services (speech/TTS), the browser, model catalogue website, and apps you open are separate providers with their own data handling. No claim is made that every external provider is offline.

## Stored locally

- Up to 100 chat messages; up to 500 listed personal items (older items remain stored until erased).
- User-created notes, tasks, reminders, preferences and tracked application labels.
- Saved resume text, UI/language options, imported AI model.

Android private SQLite/preferences/files are used, cloud backup is disabled. This is not a separately encrypted vault. Explicit personal data exports are plaintext JSON. Device security and OS storage protections still matter.

## Temporary data

- Screen preview, selected document/OCR text, and private explanation exchanges live only in process memory and are excluded from database chat history and future chat context.
- Android process termination clears those in-memory fields; backgrounding alone does not guarantee immediate erasure.
- Selecting “Save as resume” explicitly persists extracted text. Read-aloud uses the selected Android TTS provider.

## Screen service

Android grants persistent Accessibility access, but Friday's own read capability requires a device authentication and a 60-second one-shot grant plus a user shortcut tap. No text is harvested in `onAccessibilityEvent`. Password fields are skipped; non-password sensitive text may still appear. No gestures, screenshots, keylogging, arbitrary clicking or automated interaction with other apps.

Lock/power actions are only dispatched after fresh platform credential/biometric approval. The model has no tools, cannot grant permission and cannot execute code. Browser/document/screen text and AI output cannot enter the deterministic command parser. Direct user voice recognition still has recognition errors; Android authentication protects sensitive actions.

There are no stored spoken passwords. Do not put account secrets in memory or chat. Use your Android PIN/fingerprint rather than “pass 50”. Calls and SMS are handed to the dialer/composer for final confirmation.

## Erasure and export

Clear chat deletes persisted chat history. Erase personal data deletes stored items/chats and saved resume and cancels known reminders, but leaves model/settings. Remove AI model deletes the private imported model copy. Uninstall removes app data. Exports and external app copies must be deleted separately. No restore is currently provided.

## Distribution

This is a sideloaded personal preview. Store distribution requires a separate review of accessibility disclosures, data safety, permissions, release signing and applicable distribution rules. No store approval is asserted.
