# Physical device acceptance checklist

This checklist is intentionally separate from CI. A successful APK build is not a successful device test.

- [ ] Install on ARM64 Android 8+; onboarding fits small displays, large font and keyboard.
- [ ] All tabs scroll; controls have readable labels and remain reachable.
- [ ] Notes, memory edits, task completion and job status survive restart.
- [ ] Model-free mode explains AI setup without pretending to generate replies.
- [ ] Import actual compatible .litertlm model; test first load, streaming, stop, repeated prompts, rotation, app backgrounding and low-memory behaviour.
- [ ] Wrong model type, bad file, insufficient storage and unavailable engine recover without damaging existing data.
- [ ] Deny microphone; typing remains available. Test English/Hindi; verify the selected provider's offline behaviour in airplane mode.
- [ ] Stop/interrupt playback; conversation restarts after speech only in an active session; backgrounding stops capture/playback unless explicitly enabled with a visible foreground-service notification. App lock and the notification Stop action must stop the session.
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

## v0.2 regression checks

- [ ] Upgrade a populated v1 database without losing items.
- [ ] Private chat is absent from export, saved history and subsequent normal context. Explicit saves are documented.
- [ ] Delete/Undo/Trash/Restore preserve content and completion state; alarms cancel/reschedule correctly.
- [ ] Restore v1 and v2 backups; malformed files make no changes; duplicate merge is idempotent.
- [ ] Model download progress, cancel, no-space, checksum mismatch and failed model test recover clearly. Verify no personal payload is sent.
- [ ] Voice interruption ignores old playback callbacks; strict offline mode fails clearly when required language services are unavailable.
- [ ] Job stages/follow-ups, spoken interview answers/revision and editable project next steps survive restart.
- [ ] Screen/action preview text matches the selected action; cancelling never dispatches it.
- [ ] Signed releases reuse the privately configured key and a higher versionCode; debug-to-release migration restores exported data.

## v0.3 checks

- Enable app lock; background/reopen, rotate, open via Share/shortcut, cancel auth, and test device PIN/biometrics. No content or speech before unlock.
- Export/restore encrypted Unicode data, wrong password, modified file, cancelled picker; verify merge preview and legacy JSON restore.
- Share text, PDF and a large/rotated image; review before saving. Shared commands must remain inert.
- Pin both shortcuts on your launcher; mic must wait for a tap.
- Compare idle/background/active-AI RAM on the same phone/model. Verify 45-second release, background generation stop, reload and compact/balanced switching.
- Search an older note/task/project beyond 200 entries, and check literal percent/underscore searches.
- Test voice interruption, first-use TTS, conversation mode and backgrounding.

## v0.4 voice checks

- [ ] First-use disclosure/denied mic permission in tap and hold modes: no listening after releasing a permission-only press. Coach dictation still works after granting permission.
- [ ] Fast press/release before recognizer readiness, release after speech, second tap, provider error and app background/lock all release capture cleanly.
- [ ] Partial text does not execute commands; final review edits, Send, Discard and Add phrase route only the approved final text. Try Hindi and English names/numbers.
- [ ] Long dictation collects multiple phrases up to 6,000 characters; cancellation/provider timeout preserves collected phrases for Review collected text.
- [ ] Custom suggestions are provider-dependent; literal replacements do not change substrings/numbers unexpectedly.
- [ ] History off/private chat saves nothing; 1/7-day history is bounded, refresh prunes old entries, single deletion preserves others, and erase removes vocabulary/history. Check backup exclusion.
- [ ] Friday TTS stops before listening; test optional start/stop sounds and audio-focus request against another media player. Verify focus is released on errors/cancel as well as results.
- [ ] Connect/disconnect wired/Bluetooth headsets and document actual provider routing. There is no forced input selection.
- [ ] Compare 5s/45s/5min AI release, repeated prompt loading, background release, and changing the option during generation.


## 0.5 voice and referral checks

- Settings → Voice & personality: preview English and Hindi at slow/normal/fast rates. Check actual speaker volume, clarity and selected installed voice. Stop mid-sentence; ensure it stays stopped.
- Read a document longer than 4,000 characters. Confirm the ending is audible, then interrupt another long playback with the mic and verify no queued part plays over recording. Try stopping before the TTS engine finishes loading, then preview again.
- Stop the background-session notification while Friday is visible, then start a fresh mic request; the visible unlocked app must remain usable. Stop controls should also remove the session notification.
- Test a missing language pack, offline-only voice, denied microphone/audio focus, Bluetooth/wired input and returning from background. These are device/provider dependent.
- Import test connection/message CSV files under Career → LinkedIn referral assistant. Verify incoming/outgoing/group/blank-message exclusions, prepared/sent/excluded persistence after restart, and malformed-file errors without data loss. Check small screens/large fonts and the keyboard in draft review. Copy a harmless test draft; Friday must not send it.
- Verify Erase personal data clears referrals. A backup restore must not falsely report that the referral ledger was restored. Never bulk-test by sending real messages.
