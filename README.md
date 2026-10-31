# WebFirst

Open links on the web instead of being forced into a vendor app.

WebFirst intercepts links from sites that push you into their native app, removes the tracking parameters, resolves short links, and opens the clean URL in your browser. No account, no telemetry, no local storage.

## Why

Many sites make their mobile web deliberately worse so that you install their app. Installing the app is not the only option: the content is usually still reachable at its own URL, just buried under share-tracking parameters and a redirect stub. WebFirst does that cleanup for you at the moment you tap a link.

## How it works

```
tap link in any app
  -> InterceptActivity (invisible)
       -> is the host supported?           no  -> forward to the browser unchanged
       -> is it a short link?              yes -> follow the redirect chain
       -> strip query string and fragment
  -> open in the user's browser via Custom Tabs
  -> exit
```

Three design decisions worth knowing:

- **Custom Tabs, pinned to a browser package.** Re-firing a plain `ACTION_VIEW` for a host the app itself intercepts resolves back into the app and loops forever. Every outgoing intent therefore names the browser explicitly, discovered by resolving a URL this app never handles.
- **Redirect resolution happens first.** `vm.tiktok.com/ZM...` contains no query string at all; the tracking parameters only exist after the redirect lands on the canonical URL. Resolution is capped at 3.5s on a worker thread and races a deadline, so a slow network degrades to opening the original link instead of stalling.
- **Cleaning is per-site, never global.** On YouTube the query string *is* the content (`watch?v=ID`), so a global strip would destroy links instead of cleaning them.

## Install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or download the APK attached to a [release](../../releases).

## Setup — the one step nobody can automate

Android only lets an app open links silently if it proves domain ownership by serving `https://<domain>/.well-known/assetlinks.json` with its signing certificate. That file can only be hosted by the domain owner, so WebFirst can never verify `tiktok.com`.

Instead, associate the hosts once by hand:

1. Open WebFirst.
2. Tap **Open by default**.
3. Enable **Open supported links**.
4. Tick every host listed on the screen.

After that, tapping a supported link opens WebFirst, which cleans it and hands it to your browser. Until then, Android shows its own chooser and the app is just one option there.

Reinstalling the app resets this; the association lives in the system, not in the app.

## Supported sites

| Site | Hosts | Short links |
| --- | --- | --- |
| TikTok | `tiktok.com`, `www.`, `m.`, `vm.`, `vt.` | `vm.`/`vt.` hosts, `/t/`, `/v/` paths |

### Adding a site

1. Add a `SiteRule` to `SiteRules.kt` (host suffixes, short-link hosts and path prefixes).
2. Add the same hosts as `<data android:host>` entries for `InterceptActivity` in `AndroidManifest.xml`.
3. Add a case to `LinkPipelineTest`.

No other code changes are required: the pipeline is rule-driven.

## Build

Requires a JDK 17+ and Android SDK platform 35.

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew testDebugUnitTest assembleDebug
```

Debug builds are signed with the machine's debug keystore. Release builds are unsigned unless the signing values below are present.

## Releasing

Two workflows live in `.github/workflows/`:

| Workflow | Trigger | Produces |
| --- | --- | --- |
| `build.yml` | every push and pull request | tests + unsigned debug APK, kept 90 days |
| `release.yml` | a `v*` tag | signed APK + signed Play AAB, attached to a GitHub Release |

### One-time setup

**1. Create the keystore.** Keep it out of the repository.

```bash
mkdir -p ~/keys
keytool -genkeypair -v \
  -keystore ~/keys/webfirst-release.jks \
  -alias webfirst -keyalg RSA -keysize 4096 -validity 10000
```

**2. Back it up in two places.** A password manager attachment plus one encrypted copy somewhere else. With Play App Signing enabled this is your *upload* key and Play support can reset it; before that, losing it means never being able to update the app again.

**3. Add four repository secrets** — Settings → Secrets and variables → Actions:

```bash
base64 -w0 ~/keys/webfirst-release.jks   # the value for WEBFIRST_KEYSTORE_B64
```

| Secret | Value |
| --- | --- |
| `WEBFIRST_KEYSTORE_B64` | output of the command above |
| `WEBFIRST_KEYSTORE_PASSWORD` | store password |
| `WEBFIRST_KEY_ALIAS` | `webfirst` |
| `WEBFIRST_KEY_PASSWORD` | key password |

Secrets are encrypted at rest, write-only, masked in logs, and withheld from fork pull requests. The release workflow fails on purpose when the keystore secret is missing, rather than publishing an unsigned build.

### Cutting a release

```bash
git tag -a v1.0.0 -m "First release"
git push origin v1.0.0
```

That is the whole procedure. The job tests, builds, signs, and attaches `app-release.apk` and `app-release.aab` to the `v1.0.0` release page.

`versionName` and `versionCode` come from the tag — `v1.2.3` → `1.2.3` / `10203` — because Play rejects a repeated or lower `versionCode`. Creating the release from the GitHub UI works too: the workflow uploads into the existing release instead of failing.

### Signed builds locally

Export the same four values instead of using secrets:

```bash
export WEBFIRST_KEYSTORE=~/keys/webfirst-release.jks
export WEBFIRST_KEYSTORE_PASSWORD=...
export WEBFIRST_KEY_ALIAS=webfirst
export WEBFIRST_KEY_PASSWORD=...

./gradlew assembleRelease   # signed APK, for sideloading
./gradlew bundleRelease     # AAB, for Play
```

### Publishing to Play

Play takes the `.aab`, not the APK. An AAB cannot be installed on a device — Play generates per-device APKs from it.

1. Play Console → create the app ($25 one-time, identity verification).
2. Upload `app-release.aab` to a track.
3. Complete data safety (no data collected), privacy policy URL, and content rating.
4. Personal developer accounts created after 2023 need a closed test with **12 testers for 14 continuous days** before production access.
5. Enable Play App Signing, so Google holds the app-signing key and your keystore becomes the upload key.

The GitHub APK and the Play build carry different signatures, so a user cannot switch between the two channels without uninstalling.

## Privacy

- No analytics, no crash reporting, no accounts.
- The only network request is the redirect resolution for short links, made directly to the site being opened.
- Nothing is stored: no history, no preferences, no database.

## Limitations

- Links opened inside another app's embedded WebView never become `ACTION_VIEW` intents and cannot be intercepted.
- If the vendor app is installed and verified for the domain, it may win the association; disassociate it in Settings first.
- A site that refuses to serve its content in a mobile browser will still refuse after cleaning. WebFirst removes tracking and the app detour, not server-side gating.

## Not affiliated

WebFirst is an independent project. It is not affiliated with, endorsed by, or supported by TikTok or ByteDance. Site names are used only to describe which links the app understands.

## License

MIT — see [LICENSE](LICENSE).
