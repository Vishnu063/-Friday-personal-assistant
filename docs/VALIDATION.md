# Validation record

## Friday 0.4.0 — 19 October 2026 (UTC)

- Tested application source commit: `ce55c48819de529b851c63893de34f83a54feb3a` (PR #2). Subsequent documentation changes link this build; they do not change application source.
- [Successful workflow](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/39183421587).
- Command: `gradle testDebugUnitTest lintDebug assembleDebug --stacktrace`.
- Unit tests, lint, APK assembly and artifact uploads passed.
- [Download v0.4 APK ZIP](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/39183421587/artifacts/12156913819), 45,351,280 bytes compressed.
- GitHub artifact ZIP SHA-256: `21b61b8eeeb45977c1b72d99d8aa392ee2431658c87bfb48745d3aa18ad0897e`.
- [Test and lint reports](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/39183421587/artifacts/12156829209).

New tests cover Unicode word boundaries (including Hindi combining marks), literal custom replacements, preserved numbers, bounded text, preference persistence, opt-in/private-mode history, expiration, the 20-entry cap and deletion. Existing tests also passed.

Phone-specific voice quality, partial-result/vocabulary support, headsets, audio focus, hold/release, background sessions and model memory/latency are still pending the [device checklist](DEVICE_TESTING.md). No new neural speech model, full grammar checker, forced microphone selector or paid API is included. Export before migrating between debug signing identities. Artifacts expire after 30 days and require a GitHub login.

## Friday 0.3.0 — 17 October 2026 (UTC)

- APK source commit: `a97bb2b064f405163ea221ee3e02896857c5f249`.
- [Build workflow](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/39037607865).
- Command: `gradle testDebugUnitTest lintDebug assembleDebug --stacktrace`.
- Tests, Android lint, APK assembly and both artifact uploads passed.
- [Download v0.3 APK ZIP](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/39037607865/artifacts/12103280392) (45,307,426 bytes compressed; GitHub login may be required).
- ZIP SHA-256 reported by GitHub: `411cd9e566369798196157c9a4fbc624c3d8b51edf025cd4806492b7b6db4518`.
- [Test/lint reports](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/39037607865/artifacts/12103300348).

New regression tests cover encrypted Unicode backup round trips, randomisation, wrong passwords, ciphertext tampering, unsupported encryption settings, clearing working password arrays, bounded relevant preference selection, additional local commands, SQL wildcard escaping, older-record search, deleted-row exclusion and bounded memory/task queries. Existing tests also run.

Physical phone tests for app lock/authentication, external Share, pinned shortcuts, voice lifecycle, model unloading/reloading and RAM/battery measurements remain pending. The app lock is a UI gate rather than database encryption. No stable release key has been provisioned. Export before uninstalling an older debug preview; restore after installation. [v0.3 guide](VERSION_0_3.md).

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

[v0.1 successful build](https://github.com/Vishnu063/-Friday-personal-assistant/actions/runs/38960112049) remains an earlier reference. Prefer the v0.4 artifact above.
