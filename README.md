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
       -> is it a short link?              yes -> follow the redirect chain (max 5 hops, 5s timeout)
       -> strip query string and fragment
  -> open in the user's browser via Custom Tabs
  -> exit
```

Three design decisions worth knowing:

- **Custom Tabs, not `ACTION_VIEW`.** Re-firing a plain `ACTION_VIEW` for a host the app itself intercepts resolves back into the app and loops forever. Custom Tabs bind to a browser package and cannot.
- **Redirect resolution happens first.** `vm.tiktok.com/ZM...` contains no query string at all; the tracking parameters only exist after the redirect lands on the canonical URL.
- **Cleaning is per-site, never global.** On YouTube the query string *is* the content (`watch?v=ID`), so a global strip would destroy links instead of cleaning them.

## Install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Setup — the one step nobody can automate

Android only lets an app open links silently if it proves domain ownership by serving `https://<domain>/.well-known/assetlinks.json` with its signing certificate. That file can only be hosted by the domain owner, so WebFirst can never verify `tiktok.com`.

Instead, associate the hosts once by hand:

1. Open WebFirst.
2. Tap **Open by default**.
3. Enable **Open supported links**.
4. Tick every host listed on the screen.

After that, tapping a supported link opens WebFirst, which cleans it and hands it to your browser. Until then, Android shows its own chooser and the app is just one option there.

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
