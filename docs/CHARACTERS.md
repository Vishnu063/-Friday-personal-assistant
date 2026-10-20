# Friday characters (0.7)

Choose **Settings → Friday character**. There are ten original vector cartoon characters: boys Kai, Leo, Avi, Noah and Finn; girls Mia, Lily, Tara, Nia and Zoe. Use **Surprise me** to pick a different character at random. Your choice is saved locally and appears on Home and Chat. Artwork is bundled as drawing code, with a gentle bob/blink when Android animations are enabled; it does not download assets or load an AI model.

## Floating character

1. Select a character.
2. Tap **Allow display over other apps** and grant Friday that special access in Android Settings.
3. Enable Friday notifications so the persistent **Stop** control is available.
4. Return to Friday and tap **Start floating character** while the app is open.
5. Press Home/minimize Friday. Drag the character to move it. Tap the character to open Friday Chat; type or tap the mic there.

**Hide**, **Stop** in the ongoing notification, or **Stop floating character** in Friday ends the session. Swiping Friday out of Recents also requests a stop. Force-stop ends it. Start it again from Friday for the next session: no boot receiver or automatic background restart is added.

It is a shortcut to the same Friday app and saved character, not an independent assistant or microphone. It does not display message content, read other apps, activate listening, keep a model loaded or bypass Friday's app lock. The separate background-voice setting is unchanged. The overlay hides while the display is off or the device is locked. Permission revocation requests service shutdown; it can be granted again in Settings.

Android and phone manufacturers may hide overlays over sensitive/protected screens or stop the service to save battery. No claim of uninterrupted presence on every device is made. It does not appear on a lock screen. This feature uses Android's special-use foreground-service type with an ongoing notification; any future Play Store distribution needs the corresponding foreground-service declaration/review.

## Phone acceptance checks

- Select each character, restart the app and confirm the saved choice. Check Home and Chat.
- Deny overlay/notification access: no crash and no hidden running shortcut. Allow access and start while Friday is visible.
- Minimize, drag to all edges, rotate and tap to open Chat. Tap without dragging should open; dragging should not open Chat.
- Enable app lock; minimize and tap the character. Friday must require normal unlock and must not begin listening.
- Turn the display off and lock/unlock: the character should be absent while locked and return after unlock.
- Use Hide, the notification Stop, Recents removal, force-stop, and overlay-permission revocation. Confirm the window disappears and no session restarts by itself.
- Test small screens, large fonts, TalkBack, gesture navigation and Android 8/12/14/15 where available. Check reduced-motion behavior, long-session battery use and manufacturer battery controls.

CI tests exercise selection persistence/fallback, permission refusal, position bounds and simulated service lifecycle. A successful CI build does not certify actual overlay placement, animation appearance, accessibility or manufacturer background behavior; those checks require a real phone.
