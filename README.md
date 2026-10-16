# Friday — your personal Android assistant

[![Android APK](https://github.com/Vishnu063/-Friday-personal-assistant/actions/workflows/android.yml/badge.svg)](https://github.com/Vishnu063/-Friday-personal-assistant/actions/workflows/android.yml)

A calm, private space to talk, remember, plan and get things done. **No Ollama, OpenClaw, paid AI API key or always-on computer required.**

> **Early Android preview (0.2).** Native Kotlin + Jetpack Compose. The APK does not contain an AI model. Basic tools work immediately; AI chat needs a compatible model imported on the phone. Voice, model speed and accessibility behaviour need testing on your actual device. This is not a claim of ChatGPT-equivalent voice or reasoning.

[Download build](https://github.com/Vishnu063/-Friday-personal-assistant/actions/workflows/android.yml) · [Setup guide](docs/SETUP.md) · [Privacy](docs/PRIVACY.md) · [Device checklist](docs/DEVICE_TESTING.md) · [Architecture](docs/ARCHITECTURE.md)

**Previous v0.1 verified preview:** [download the successful APK build](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/38960112049/artifacts/12071949079). Tests, lint and APK build passed; [validation record](docs/VALIDATION.md). Physical-phone testing remains pending.

## New in 0.2

Private chat, Undo/trash, validated backup restore, searchable memory, phone-action previews, editable screen review, stronger voice interruption handling, strict offline voice, speech speed, model progress/download/checksums, on-phone compatibility test, diagnostics and overdue-task recovery.

Personal tools now include a **career dashboard** (Pune by default), **voice interview coach** with revision notebook, and **project checkpoints** with a next-task button. [v0.2 guide](docs/VERSION_0_2.md) · [Changelog](CHANGELOG.md).

A [signed-release workflow](docs/RELEASE_SIGNING.md) is prepared; its private signing secrets still require owner setup. Debug builds alone do not guarantee updates without reinstalling.

**Network change:** v0.2 requests internet permission for an optional, explicit HTTPS model download. Google ML Kit may also transmit SDK diagnostic/usage metrics; its image inputs and OCR outputs remain on-device. Local chat, screen text and resume are never uploaded by that downloader. System speech providers and browser searches remain separate services.

## Get started on your phone

1. Open **Actions → Android APK → the latest successful run**.
2. Download **friday-android-preview**, unzip it, and install `app-debug.apk`. A GitHub login may be required for artifact downloads. Only allow app installation for the trusted downloader you use.
3. Open Friday. Save a note, task or preference—these work without a model.
4. For AI, go to **Settings → Browse compatible model catalogue**, obtain a compatible small `.litertlm` text-chat model, then tap **Import AI model**. Models are separate downloads and have their own licences/access terms.
5. Tap the microphone for voice. Enable screen tools or notifications only when you need them.

**Phone requirements:** Android 8+ (API 26), 64-bit ARM. Lock screen requires Android 9+ and accessibility support. The UI/basic tools do not need a GPU. Local AI memory and speed depend on the selected model and device; start with a quantised text model around 1B parameters or smaller and verify it works. Keep space for both the source model and imported copy. GGUF and MediaPipe `.task` files are not supported by this version.

## What is included

| Area | Available in the Android app |
|---|---|
| Chat | On-device LiteRT-LM inference, streaming responses, stop generation, bounded recent conversation context |
| Voice | Hindi/English system recognition and TTS; mic interrupts playback; optional foreground conversation loop |
| Personal memory | Explicitly saved, editable/deletable preferences; toggle whether to include them in AI prompts |
| Notes & tasks | Local notes, editable checklists, approximate notification reminders, future reminder restoration after reboot |
| Daily briefing | On-demand summary of your open tasks |
| Screen reading | Authenticate, open target app, tap Friday's Android accessibility shortcut; capture visible text once within 60 seconds; review, read aloud, explain or discard |
| Phone controls | Fresh device PIN/fingerprint check for lock or power menu; final shutdown confirmation is Android's own UI |
| Apps, calls & SMS | Open installed apps, open dialer, compose an SMS; final call/send stays in the selected phone app |
| Documents | Import PDF/text (10 MB max, first 15 PDF pages), review text, local summary and read-aloud; scanned PDFs need OCR |
| Camera & screenshots | Camera preview or selected image → on-device Latin/Hindi OCR → editable text |
| Job helper | Saved resume text, transparent skill keyword comparison, application tracker with Saved/Applied state |
| DevOps helper | Interview prompt and log-analysis chat; no shell access or automatic infrastructure changes |
| Web | Open an explicit query in your browser; the local model has no live search tool |
| Personalisation | Animated Friday orb, violet/mint accent, voice language and speech toggle |
| Data controls | Export local personal data as JSON; clear chat, erase personal data, remove AI model separately |

### Try these

```text
Friday, note buy groceries
Remember I prefer short answers
Add task practise Terraform
Remind me in 10 minutes to study
My day
Open YouTube
Search Kubernetes troubleshooting guide
Dial +91 1234567890
Lock my phone
Read my screen
Power menu
```

Command shortcuts currently use English, with a small set of Hindi phone commands. Hindi freeform chat depends on the imported model. Notes/tasks/reminders and explicit phone commands are deterministic and do not require AI.

## Permission controls

- **Microphone:** asked only when you use voice. No background wake word or microphone service. Android recognition/TTS providers may use the internet; Friday discloses that before first voice use. On supported Android 12+ devices Friday selects the on-device recognizer.
- **Notifications:** requested for reminders. Alarms are approximate; battery optimisation and denied permission can delay/prevent alerts. No exact-alarm special permission. v0.2 adds overdue views, rescheduling and a diagnostics delivery test.
- **Accessibility:** optional, explicitly disclosed before opening Android settings. No screenshot recording, gesture injection, automatic clicking, or event-based text collection. A fresh authenticated screen request expires in 60 seconds and is consumed once. Android password nodes are excluded, but other sensitive visible text can still appear: always review the preview.
- **Lock/power:** device credentials or strong biometrics authorise each request. A spoken code like `pass 50` is **not** a security credential. Android may restrict actions on individual devices. Friday opens the power menu; it cannot silently switch off a standard phone.
- **Files/photos:** Android's system picker grants access only to selected content. Camera opens your existing camera app; Friday has no camera permission.

The model receives **no executable tools**. Model responses and imported document/screen text never enter the command router. Only direct user chat submissions and explicit UI buttons can request phone actions. See [privacy and boundaries](docs/PRIVACY.md).

## Build from source

Use Android Studio with JDK 17, Android SDK 35 and Gradle 9.3.1, or install Gradle 9.3.1 and run:

```bash
git clone https://github.com/Vishnu063/-Friday-personal-assistant.git
cd -- -Friday-personal-assistant
gradle testDebugUnitTest lintDebug assembleDebug
```

The GitHub workflow installs the toolchain and uploads a debug-signed APK and test/lint reports. No signing secret is needed for the debug preview. The separate signed-release workflow requires owner-provided GitHub Secrets. CI debug signing keys may change between runners; an update signed with a different key requires uninstalling the old preview (which erases local app data). Export data first; JSON restore is available in Settings; export before migrating between signing identities. Use a stable private signing key for a long-term personal release. Do not commit signing keys.

## VoiceStudio relationship

[Vishnu063/VoiceStudio](https://github.com/Vishnu063/VoiceStudio) is useful research for speech pipelines, streaming and voice engines, but its desktop/Python runtime is not embedded here. Friday starts with Android speech services and does **not** copy VoiceStudio source. Future native speech adapters must be benchmarked on phones and respect both code/model licences. No claim is made that VoiceStudio's desktop engines run unchanged inside an APK.

## Not yet included

Always-on “Hey Friday”; a custom bundled neural voice; VoiceStudio integration; automatic job applications; email/calendar/account connectors; arbitrary control of other apps; shell execution; autonomous repairs; unattended shutdown; scheduled morning briefings; Play Store distribution; signing-key provisioning and autonomous updates.

No service runs continuously to read your screen. Protected screens and apps that do not expose accessibility text cannot be read; OCR is a separate, user-selected image workflow.

## Contribute

Read [CONTRIBUTING.md](CONTRIBUTING.md), run the build checks, and document device behaviour honestly. Keep permission requests explicit and preserve the separation between model text and actions. Report sensitive issues using [SECURITY.md](SECURITY.md).

Apache-2.0 for Friday's original code. Dependencies and model files retain their own licences; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
