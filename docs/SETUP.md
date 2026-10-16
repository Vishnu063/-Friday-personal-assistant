# Phone setup

## Install

Use the latest successful **Android APK** workflow artifact. It is a ZIP containing `app-debug.apk`, not a Play Store release. Check that its run succeeded before downloading. Install the APK on an ARM64 Android 8+ phone and open Friday.

A debug APK is for personal evaluation. Android may warn about sideloading; grant installation permission only to the downloader you trust, then disable it afterward. There is no payment or AI provider sign-in inside Friday.

## AI setup

Settings shows your phone, Android version and approximate installed RAM. Open the model catalogue, choose a small **text-chat `.litertlm`** bundle supported by LiteRT-LM 0.18.0, follow its provider's licence/access instructions, download it, then import it with Friday's file picker. Do not rename GGUF or `.task` files to `.litertlm`.

Model downloads may be large or gated by a provider login, and mobile data charges may apply. No model is bundled or automatically downloaded. Import uses a streaming copy into private storage. Runtime compatibility is checked when you first chat, not merely by importing the file. A successful import does not guarantee the model can run on that phone.

Try a short message. If loading fails, keep using basic tools, remove the incompatible model, and try a smaller supported model. The CPU backend is used. Start with 1B parameters or less; suitability still depends on architecture, quantisation and RAM. Android may terminate an app that exceeds memory limits. Never promise all phones can run a selected model.

## Voice

Tap the microphone; read the provider disclosure; grant microphone permission. Choose English or Hindi in Settings. TTS voices/language packs are managed in Android settings. A provider without on-device support can send audio/text to its own servers. Android's offline preference is a preference, not a guarantee of offline processing. Friday 0.2 requests internet permission only for the explicit model downloader; chat and screen data are not uploaded.

Toggle **Conversation**, then tap the mic to begin. After a spoken reply finishes, listening starts again while Friday stays foreground. Tap mic to interrupt speech, or disable Conversation to stop the loop. Recognition errors require a manual retry. There is no wake word or background listening. ChatGPT-like naturalness/latency is not guaranteed by this system-service implementation.

## Screen reading

1. Tools → **Read my screen** → read disclosure → open Android Accessibility settings.
2. Enable **Friday screen tools** and configure its accessibility shortcut/button. Some sideloaded Android versions require **App info → menu → Allow restricted settings** before accessibility access can be enabled. Menu names vary; never disable unrelated security controls.
3. Return to Friday, tap **Read my screen** again, confirm the one-screen disclosure, then authenticate with your device PIN/password or fingerprint.
4. Within 60 seconds open the desired app and tap Android's Friday accessibility shortcut/button.
5. Friday opens with a temporary text preview. Read aloud, explain locally, or discard. The preview and private explanation are not written to chat history. They remain in process memory until dismissed/cleared or the process exits.

The service does not continuously watch text events. It ignores the Friday app and Android System UI; it skips password nodes and only traverses visible accessible text. This is not a guarantee that all sensitive text is excluded. Review before use. Protected or custom-rendered apps may expose nothing; import a screenshot you chose if that is appropriate.

## Lock and power

Enable screen tools first. Tools → **Lock my phone** or **Power menu** prompts for device authentication. Device credentials must already be configured in Android. On Android 8 screen-lock global actions are unsupported; use the physical button. On Android 9+ device/vendor policy can still block actions.

Power menu shows Android's UI. You choose **Power off** yourself. A spoken password is not accepted as an authentication bypass.

## Reminders

Add a task, optionally choose a future date/time, save and allow notification permission. “Remind me in 10 minutes to study” also works. Reminders use inexact alarms, so battery controls can delay them. Future pending reminders are restored on normal reboot; overdue tasks remain in the list without a catch-up alert. Android force-stop prevents alarms until the app is opened/restarted; this app does not override that.

## Documents and photos

Use Tools to import PDF/text or an image, or launch the phone camera for a preview image. Select the OCR language in Settings first. Documents are capped at 10 MB, the first 15 PDF pages, and 12,000 extracted characters. AI summaries use only a bounded excerpt; longer documents are not fully covered. Scanned PDFs need image OCR. Inspect extracted text before using it as a resume or asking for a summary.

## Data

Settings → Export saves personal data as plaintext JSON to a location you choose. Restore is available in Settings; see [v0.2 guide](VERSION_0_2.md). Erase personal data clears the local database and saved resume; remove the model separately. Uninstall removes Friday's private data, but not exported files or copies held by selected external apps.
