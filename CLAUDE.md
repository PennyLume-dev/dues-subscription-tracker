# Dues — project rules

Dues is a free Android subscription tracker (Kotlin + Jetpack Compose) by **Pennylume**.

## Git & GitHub
- Commit **only as Pennylume**: repo-local `user.name = Pennylume`, `user.email` = the `pennylume` GitHub account's noreply address.
- **No attribution trailers** in commits or PRs: no `Co-Authored-By: Claude …`, no `Claude-Session: …`, no "Generated with Claude Code".
- Never commit the user's personal name, email or local paths (e.g. `C:\Users\...`).
- Push to the organization **PennyLume-dev** using the `pennylume` account. Other personal accounts are also logged in to `gh`: run `gh auth status` and make sure `pennylume` is active before any push.
- Never commit secrets: `local.properties` (API keys, `RELEASE_*` signing values), `*.jks` / `*.keystore`. Scan for keys before the first push of a new repo.

## Product decisions
- Dues is **fully free**: no ads, no in-app purchases, no subscription limit. What is free stays free.
- Distribution: Samsung Galaxy Store (Private seller, free apps only), GitHub Releases, dues-app.vercel.app.

## Build
- Low-RAM PC: `./gradlew assemblePreview testDebugUnitTest --max-workers=1`.
- Release signing reads `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` from `local.properties`.
- Price catalog: `app/src/main/assets/catalog.json`; bump `"v"` on every change, then deploy `site/` (`python site/build.py pennylume@proton.me`, `vercel deploy --prod` from `site/dist`).
