# Friday — your personal Android assistant

[![Android APK](https://github.com/Vishnu063/-Friday-personal-assistant/actions/workflows/android.yml/badge.svg)](https://github.com/Vishnu063/-Friday-personal-assistant/actions/workflows/android.yml)

A calm, private space to talk, remember, plan and get things done. **No Ollama, OpenClaw, paid AI API key or always-on computer required.**

> **Early Android preview (0.4).** Native Kotlin + Jetpack Compose. The APK does not contain an AI model. Basic tools work immediately; AI chat needs a compatible model imported on the phone. Voice, model speed and accessibility behaviour need testing on your actual device. This is not a claim of ChatGPT-equivalent voice or reasoning.

[Download build](https://github.com/Vishnu063/-Friday-personal-assistant/actions/workflows/android.yml) · [Setup guide](docs/SETUP.md) · [Privacy](docs/PRIVACY.md) · [Device checklist](docs/DEVICE_TESTING.md) · [Architecture](docs/ARCHITECTURE.md)

**Verified v0.4 preview:** [download Friday 0.4.0 APK ZIP](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/39183421587/artifacts/12156913819). Tests, Android lint and APK build passed; [validation record](docs/VALIDATION.md). Physical-phone testing and private signing-key setup remain pending.

## New in 0.4

Editable voice review, live partial transcripts, tap/hold dictation, longer phrase collection, custom vocabulary, optional filler cleanup, bounded transcript history, recording feedback, audio-focus requests and AI idle memory controls. See [voice setup below](#voice-and-dictation-04). No new paid API.

## Previous additions in 0.3

Simpler Home, PIN/fingerprint app lock, Android Share reception, pinned Talk/Note shortcuts, password-encrypted backups, more fast commands, and compact AI context with idle/background unloading. Saved lists and preference retrieval are bounded. [v0.3 guide](docs/VERSION_0_3.md). Physical-phone RAM and battery benchmarks are pending.

## Previous additions in 0.2

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
| Voice | Hindi/English system recognition and TTS; mic interrupts playback; editable dictation review, live text when supported, optional foreground-service conversation loop |
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
| Data controls | Password-encrypted backup/restore, legacy JSON export/restore; clear chat, erase personal data, remove AI model separately |

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

- **Microphone:** asked only when you use voice. No background wake word. Optional background conversation requires explicit setup, an active foreground-service notification and a manual mic tap; app lock disables it. Android recognition/TTS providers may use the internet; Friday discloses that before first voice use. On supported Android 12+ devices Friday selects the on-device recognizer.
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

## Voice and dictation (0.4)

Open **Settings → Voice & personality**. These features use Android speech recognition and TTS, with no new paid API or extra AI cleanup call:

- **Review before sending** is on by default. Edit the recognised text, Send it, Discard it, or use **Add phrase** to collect more. Nothing in the review is executed before Send. Review also pauses hands-free conversation until you return to Friday and send it.
- **Live transcript** shows partial words when the speech provider supplies them. Final results can differ.
- **Tap mic** starts speech; tap again to finish. **Hold mic** starts on press and finishes on release. Complete the voice disclosure and microphone permission first, then hold again. Tap mode and Send remain available for accessibility.
- **Long dictation** collects phrases in an editable draft, up to 6,000 characters. Android may finish each phrase at a pause: use **Add phrase** to continue, then Send once. This is explicit phrase collection, not continuous background recording or automatic silence splitting.
- **Personal vocabulary:** one spelling per line, for example `Vishnu` or `Kubernetes`. Android 13+ can pass these suggestions to a supporting provider. For a known recurring error, add `cube er net ease => Kubernetes`. Replacements use literal whole-word boundaries, not fuzzy guessing. Review the result before sending.
- **Optional cleanup** removes `um`, `uh`, `erm`, and repeated spaces. It is not a complete grammar checker. The local AI already interprets imperfect grammar; no separate model inference is added.
- **Recording sounds** are optional. Friday stops its own TTS before listening. **Ask other media to pause** requests temporary exclusive audio focus; other apps and Android decide whether to comply. Friday does not change global volume.
- **Approved transcript history** is off by default. Opt into 1 or 7 days, with a hard maximum of 20 transcripts. Reuse opens a review, never executes immediately. Delete individual entries or all entries in Settings. Retention is pruned on app start, refresh, and save rather than by a background alarm. Private chat skips this history; existing saved transcripts can still be deleted. Erase personal data also clears transcripts and vocabulary. Transcripts are excluded from backup exports. Friday never stores raw microphone recordings; Android speech providers have separate policies.
- **AI memory controls:** Save RAM unloads the model after 5 idle seconds; Balanced after 45 seconds; Keep warm after 5 minutes. Background release is unchanged. Keeping the model warm reduces repeat startup delay at the cost of RAM; it cannot promise faster token generation.
- **Headsets:** connect a headset through the Android shortcut before recording. System speech recognition controls input routing. There is no reliable per-device microphone selector in Friday, and a Bluetooth connection alone does not guarantee the provider uses its microphone.

Both Hindi and English remain selectable per session. Offline recognition/voice availability depends on the installed Android provider and language packs. Handy's desktop models, Whisper, Parakeet, neural VAD, and new voice packs are **not bundled** in this update.

### Build and install the updated APK

For the supported CI build, open this repository's **Actions → Android APK**, choose the newest successful run for the new commit, then download **friday-android-preview**, extract it, and open `app-debug.apk`. GitHub artifact downloads require signing into GitHub; use your phone browser if the GitHub app does not show downloads. Only install an APK you built or trust. A successful older run does not include these changes.

Local development requires JDK 17, Android SDK platform 35 / build tools 36, and Gradle 9.3.1:

```bash
git clone https://github.com/Vishnu063/-Friday-personal-assistant.git
cd -- -Friday-personal-assistant
gradle testDebugUnitTest lintDebug assembleDebug --stacktrace
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. Use the same signing key when updating an existing install; uninstalling clears local data, so export important notes first. Test Hindi/English, headset routing, audio-focus behaviour, press/release, conversation review, and screen-off behaviour on your own phone: CI cannot verify microphone hardware or provider-specific features.
