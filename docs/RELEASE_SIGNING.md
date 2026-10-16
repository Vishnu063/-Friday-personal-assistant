# Stable updates with a private signing key

The signed-release workflow is prepared, but **no private key or signing secret has been provisioned by this project**. Debug APKs from separate CI runners do not have a stable signing identity. Do not promise in-place updates between preview APKs.

## One-time owner setup

1. On a trusted computer with JDK tools, create a dedicated release key. Keep its keystore/passwords backed up privately; never commit them:

```bash
keytool -genkeypair -keystore friday-release.jks -alias friday -keyalg RSA -keysize 3072 -validity 10000
```

2. Base64-encode the keystore locally and add it as the GitHub Actions **release environment** secret `FRIDAY_KEYSTORE_BASE64`. Also add `FRIDAY_STORE_PASSWORD`, `FRIDAY_KEY_ALIAS` (e.g. `friday`), and `FRIDAY_KEY_PASSWORD`. Use private GitHub secret inputs; never paste these into issues, chat, build logs or source files. Base64 is encoding, not encryption.
3. Configure the `release` environment's access rules appropriately. Run **Signed Android release** manually from Actions. It fails clearly when secrets are missing and uploads only the signed APK. The keystore is removed from the temporary runner directory after the build and is not uploaded as an artifact.
4. Every future release must reuse this key and have a higher `versionCode`. Android then supports in-place updates while retaining private app data. Losing or changing the key prevents such updates.

## Migrating from a debug preview

Export your personal data in Friday. An APK signed with a new release key cannot replace a differently signed debug app. Uninstall the old preview once, install the signed release, then use **Restore a backup**. AI models must be imported again; they are deliberately excluded from backups. Check restored future reminder permissions and delivery.

Friday does not implement self-installing updates, Play Store publishing or an update-download server. Use verified workflow artifacts; Android handles installation confirmation. This workflow prepares stable signing, but it is not active until the owner supplies the private key secrets.
