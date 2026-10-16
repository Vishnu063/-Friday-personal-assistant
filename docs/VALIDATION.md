# Validation record

## Friday 0.2.0 — 17 October 2026 (India)

- APK source commit: `035251f8a8ad09ae67b001fefbb506f678db46e0`.
- [Successful workflow](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/38962566082).
- Command: `gradle testDebugUnitTest lintDebug assembleDebug --stacktrace`.
- Result: **success**. JVM unit tests, Android lint, APK assembly and artifact upload passed.
- [Download v0.2 APK ZIP](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/38962566082/artifacts/12072599894) (about 45 MB compressed; GitHub login may be required).
- ZIP SHA-256 reported by GitHub: `62f8ec56b22458b45ca3f829ccd9721cd2c430733562f5f16ae02602edea3e8f`.
- [Test/lint reports](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/38962566082/artifacts/12072864161).

Tests cover direct action boundaries and spoken-password rejection; reminder units/bounds and dial syntax; backup format/length/role validation and deep-nesting rejection; model URL validation; transparent interview checklist feedback; and Robolectric SQLite tests for preserving v1 data, excluding deleted memory from exports/context, restoring trash and repeated-merge deduplication.

Unzip and install `app-debug.apk`. Debug signing identities can differ between CI runs: export before uninstalling an old preview, then restore in v0.2. This is not a consistently release-signed build. Artifacts are retained for 30 days; rerun Android APK after expiry.

## Release-signing status

The manual signed-release workflow is prepared, including missing-secret guards, step-limited secret exposure and temporary-key cleanup. Its YAML was parsed successfully. **No private signing key or GitHub signing secret was provisioned, and no signed-release run was performed.** Follow [private key setup](RELEASE_SIGNING.md) to enable stable signing and future in-place updates.

## Physical-device status

No physical phone was connected. AI latency/quality, actual download/cancellation on mobile networks, speech recognition/interruptions, OCR accuracy, accessibility behaviour, battery/reminder delivery and small-screen/large-font layout still need the [device checklist](DEVICE_TESTING.md). Robolectric tests do not substitute for these checks.

This is an installable preview with tested repository logic, not a guarantee of production readiness or ChatGPT-equivalent voice.

## Previous version

[v0.1 successful build](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/38960112049) remains an earlier reference. Prefer the v0.2 artifact above.
