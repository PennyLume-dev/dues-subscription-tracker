# Contributing to Dues

Thanks for helping! Dues is a free, private subscription tracker, and the easiest way to help is to keep its prices right.

## Fix or add a price
1. Open an issue with the **Wrong or missing price** or **Add a service** template, with an official source link, or
2. Edit `app/src/main/assets/catalog.json` directly and open a pull request:
   - Services: `n` name, `d` domain, `c` category, `p` plans.
   - Plans: `n` name, `u` cycle unit (`month`/`year`), `k` cycle count, `fam`/`stu` family/student, `t` trial days,
     `pr` prices as `{"COUNTRY": amount}` in that country's own currency, `x` countries where the plan isn't sold.
   - Bump the top-level `"v"` (date, e.g. `2026-10-01`) so installed apps pick up the change.
   - Only use official sources (the service's pricing page or app store listing). No estimates.

## Code
- Kotlin + Jetpack Compose, JDK 17, Android SDK 36.
- `./gradlew testDebugUnitTest` must pass; add a test for tricky logic.
- Keep it private: no analytics, no accounts, no network calls that include user data.
- Keep pull requests small and focused, and match the surrounding code style.

## Good first issues
Look for issues labelled [`good first issue`](../../labels/good%20first%20issue).

By contributing you agree your contribution is licensed under the GPL-3.0. The Dues name, logo and Penny mascot are Pennylume trademarks.
