<p align="center"><img src="art/penny_wave.png" width="140" alt="Penny, the Dues mascot"></p>

<h1 align="center">Dues – Free Subscription Tracker for Android</h1>
<p align="center"><b>Know what you really pay for subscriptions.</b><br>
A free, private, open-source subscription manager and renewal reminder app by <b>Pennylume</b>.</p>

<p align="center">
  <a href="LICENSE"><img alt="License: GPL-3.0" src="https://img.shields.io/badge/license-GPL--3.0-blue"></a>
  <img alt="Platform: Android" src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white">
  <a href="../../releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/PennyLume-dev/dues-subscription-tracker/total?color=gold"></a>
  <a href="https://dues-app.vercel.app"><img alt="Website" src="https://img.shields.io/badge/web-dues--app.vercel.app-orange"></a>
</p>

Dues helps you **track subscriptions, cut subscription costs and never miss a renewal**. Add Netflix, Spotify, ChatGPT, YouTube Premium, iCloud+, Disney+, Amazon Prime and more, and Dues shows your real monthly and yearly spend with **verified local prices**, then reminds you before every charge or free-trial end. No account, no ads, no in-app purchases.

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

## FAQ
**Is Dues free?** Yes. Completely free: no ads, no in-app purchases, no subscription limit.

**Does Dues upload my data?** No. There is no account; your subscriptions stay on your phone. Export to CSV for backups.

**Which countries have verified prices?** United States, India, United Kingdom, Germany, France, Canada, Australia, Singapore, UAE, Brazil, Japan and Mexico. Elsewhere, prices are converted from USD at live rates.

**Can Dues cancel a subscription for me?** No. It reminds you before you're charged; you cancel with the provider and mark it cancelled in Dues.

**Is there an iPhone version?** Not yet. Dues is Android only for now.

## Tech
Kotlin · Jetpack Compose · Material 3 · Room · WorkManager · Coil.

## License
The code is licensed under the **GNU GPL v3.0** (see [LICENSE](LICENSE)).

The **"Dues" name, app icon, logo and the Penny mascot artwork** are trademarks and property of Pennylume and are **not** covered by the GPL. Forks must use their own name and artwork.
