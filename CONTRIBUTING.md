# Contributing to Friday

Keep changes focused and explain the resulting user behaviour. Build with JDK 17, SDK 35, Gradle 9.3.1:

```bash
gradle testDebugUnitTest lintDebug assembleDebug
```

Never route model output or imported content into phone actions. Keep platform authentication for lock, power and screen-read grants. Do not add background capture or credentials to logs. Permissions must have an in-app explanation and graceful refusal behaviour. Add meaningful command-boundary tests for new actions. Validate native inference, voice and accessibility on physical devices and record what was tested. Do not claim untested device compatibility.

Do not commit signing keys, personal data, AI weights or large generated binaries. Review dependency/model licences before integrating speech engines. Accessibility behaviour changes require updated privacy docs and device checklist.
