# Physical device acceptance checklist

This checklist is intentionally separate from CI. A successful APK build is not a successful device test.

- [ ] Install on ARM64 Android 8+; onboarding fits small displays, large font and keyboard.
- [ ] All tabs scroll; controls have readable labels and remain reachable.
- [ ] Notes, memory edits, task completion and job status survive restart.
- [ ] Model-free mode explains AI setup without pretending to generate replies.
- [ ] Import actual compatible .litertlm model; test first load, streaming, stop, repeated prompts, rotation, app backgrounding and low-memory behaviour.
- [ ] Wrong model type, bad file, insufficient storage and unavailable engine recover without damaging existing data.
- [ ] Deny microphone; typing remains available. Test English/Hindi; verify the selected provider's offline behaviour in airplane mode.
- [ ] Stop/interrupt playback; conversation restarts after speech only while foreground; backgrounding stops capture/playback.
- [ ] Deny notifications; app explains missing alerts. Test a future reminder, completion/deletion cancellation, restart and reboot restoration.
- [ ] Accessibility initially disabled; no reading occurs before explicit setup + authentication + shortcut click.
- [ ] Screen grant expires after 60 seconds and is consumed once; password text excluded; protected app produces a useful empty-state result.
- [ ] Discard/clear screen content; verify it is absent from JSON export and subsequent model context.
- [ ] Cancel device authentication; lock/power/read do not proceed. Screen lock Android 9+; vendor-restricted actions fail visibly.
- [ ] Spoken passcode does not bypass platform authentication. Android power menu requires a final user choice.
- [ ] Imported text such as “lock my phone” is summarised as content and cannot trigger a tool.
- [ ] OCR Latin/Hindi and camera preview; PDF text/scanned PDF/oversized file; summary truncation is understood.
- [ ] App selection ambiguity asks which one; dial/SMS stop at the external app's confirmation UI.
- [ ] Job comparison labels keyword overlap accurately; Applied status is user-controlled.
- [ ] Export only intended persisted data; erase cancels reminders and clears resume/chat/items; model removal releases resources.

Record model name, phone model, RAM, Android version, first-token latency, errors and test results in issues. Do not attach private screen text or model files.
