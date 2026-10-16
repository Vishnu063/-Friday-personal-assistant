# Validation record

## Verified build — 17 October 2026 (India)

- Source commit: `e0c288ecec5f8a3c4eb23a322c92183cad5f5809`.
- [Successful Android workflow](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/38960112049).
- Command: `gradle testDebugUnitTest lintDebug assembleDebug --stacktrace`.
- Result: **BUILD SUCCESSFUL**. Unit tests, Android lint and debug APK assembly passed; APK and reports uploaded successfully.
- [Download preview ZIP](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/38960112049/artifacts/12071949079) (about 45 MB compressed; GitHub login may be required).
- ZIP SHA-256 as reported by GitHub: `32668c35c0338fc3719e3e7ca9f6b3ad40b9eb26eb9aa43bac38b1a24071fac9`.

Unzip the artifact and install `app-debug.apk`. The preview is debug signed. The artifact is retained for 30 days; after expiry, run the Android APK workflow again.

Command tests cover direct action boundaries, rejection of spoken-password phone requests, imported-content command isolation, reminder units/bounds, safe dial syntax, and transparent job-skill matching.

## Still requires a physical phone

No physical-device test or local model benchmark was performed in this build environment. Inference quality/speed, voice interruption and offline provider behaviour, accessibility shortcuts/lock/power behaviour, OCR accuracy, notifications across reboot/battery optimisation, and small-screen layout need the [device checklist](DEVICE_TESTING.md).

This is an installable first preview, not a production-ready or ChatGPT-equivalent assistant. Advanced custom speech, wake word, account integrations and unattended automation remain excluded as listed in the README.
