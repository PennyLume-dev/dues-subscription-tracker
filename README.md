<p align="center"><img src="art/penny_wave.png" width="140" alt="Penny, the Dues mascot"></p>

<h1 align="center">Dues</h1>
<p align="center"><b>Know what you really pay for subscriptions.</b><br>
A free, private Android subscription tracker by <b>Pennylume</b>.</p>

---

## Features
- **Every subscription in one place**: 69 popular services with real plans and prices.
- **Verified local prices** in 12 countries (US, IN, GB, DE, FR, CA, AU, SG, AE, BR, JP, MX), with live currency conversion elsewhere.
- **Renewal and free-trial reminders** before you get charged.
- **Spending orbit, calendar and yearly totals** so small charges stop hiding.
- **Private by design**: no account, no ads, no tracking. Your data stays on your phone (CSV export for backups).
- **Free.** No in-app purchases, no limits.

## Download
- Latest APK: [GitHub Releases](../../releases)
- Galaxy Store: coming soon
- Website: https://dues-app.vercel.app

## Build
Requirements: JDK 17, Android SDK 36.

```bash
./gradlew assembleDebug            # debug APK
./gradlew testDebugUnitTest        # unit tests
```

Optional `local.properties` entries:
- `LOGO_DEV_KEY=` publishable [logo.dev](https://logo.dev) key for service logos (falls back to favicons).
- `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` for signed release builds.

Prices live in `app/src/main/assets/catalog.json`; the app also fetches the newest copy from the website. Price corrections are welcome as issues or pull requests.

## Tech
Kotlin · Jetpack Compose · Material 3 · Room · WorkManager · Coil.

## License
The code is licensed under the **GNU GPL v3.0** (see [LICENSE](LICENSE)).

The **"Dues" name, app icon, logo and the Penny mascot artwork** are trademarks and property of Pennylume and are **not** covered by the GPL. Forks must use their own name and artwork.
