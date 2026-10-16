# Architecture

| Component | Responsibility |
|---|---|
| MainActivity / Compose | Four primary tabs, settings, disclosures, system permission launchers, platform authentication |
| FridayModel | UI state, saved preferences, local data operations, serialized AI tasks |
| Commands | Anchored direct-user shortcuts; no parsing of model output, imported files or screen content |
| LocalBrain | LiteRT-LM CPU engine, streamed local chat, per-request bounded conversation, atomic model import, native resource release |
| Voice | Android speech recognition and TTS, foreground conversation loop, playback interruption |
| Store | Private SQLite notes/tasks/preferences/applications and bounded chat history |
| ScreenService | No-op screen events; explicit accessibility-button callback consumes a short authenticated grant |
| Reminders | Inexact AlarmManager reminders, notification receiver and future-alarm restoration on reboot |
| Documents | Bounded PDF/text extraction; ML Kit image OCR is launched by the UI |

The local model is never an execution authority. No model-defined tools are registered. A natural-language answer cannot call Android APIs. The direct-user command parser is a convenience layer ahead of a separate explicit permission/authentication boundary.

## Model and speech decisions

LiteRT-LM replaces the initially considered desktop Ollama/OpenClaw stack. The user requested a phone APK without a computer or paid inference API. A model file is imported separately to keep installation controllable. CPU is the baseline backend; GPU/NPU support requires future device-specific benchmarking.

VoiceStudio was reviewed as a reference, but its desktop Python backends are not dependencies. Friday uses Android recognition/TTS to ship a smaller first implementation. A custom fully offline neural speech stack, turn-taking/VAD/echo cancellation improvements and wake-word support are future work; system services do not provide guaranteed ChatGPT-level voice.

## Build

Pinned Android Gradle plugin 9.1.1, Gradle 9.3.1, JDK 17, built-in Kotlin upgraded to 2.4.21 and matching Compose compiler (required by LiteRT-LM 0.18 metadata), compile/target SDK 35. ARM64 packaging. The workflow builds a debug-signed preview, runs JVM command-boundary tests and Android lint, then uploads APK and reports. It cannot certify model latency, reminders or accessibility behaviour without a phone.
