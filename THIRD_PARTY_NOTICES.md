# Third-party components

Friday's original source is Apache-2.0. Review upstream dependency distributions for complete notices and transitive dependencies.

| Component | Upstream licence / role |
|---|---|
| AndroidX / Jetpack Compose | Apache-2.0; Android UI, lifecycle and authentication |
| Kotlin / kotlinx.coroutines | Apache-2.0; language/runtime and asynchronous work |
| Google LiteRT-LM | Apache-2.0; on-device model runtime (check bundled transitive notices) |
| Google ML Kit | Google SDK terms; text recognition SDK and bundled OCR assets |
| PDFBox Android / Apache PDFBox | Apache-2.0; PDF text extraction |
| JUnit | EPL-1.0; unit testing |

AI model files are not distributed in this repository or APK. Imported models retain their provider's licence and terms. Android speech/TTS providers and external apps are not redistributed by Friday. VoiceStudio is referenced for research; its source is not copied or linked into this app.


Optional external tool: [bcharleson/linkedincli](https://github.com/bcharleson/linkedincli), MIT licensed, is installed separately on the owner's computer for experimental read-only checks. It is not bundled into the APK. Friday's bridge is original standard-library code and calls a fixed set of its read commands. See docs/LINKEDIN_CONNECTION.md for reviewed revision and limitations.
