# tetris-brick modernization — progress log

Reusable handoff doc. Keep this current so a new session can pick up immediately.

## Baseline

- Verified baseline commit: `f57bd46` on `main` ("Updated target API version to latest"), dated 2024-09-29, matches the prior read-only audit.
- Working branch: `modernize-2026` (created off `main` at `f57bd46`).
- Local tooling verified present: JDK 17 (Temurin), Android SDK at `~/Library/Android/sdk`, Android Studio, `gradlew` executable.
- Baseline build/test results (before any source changes, run on `f57bd46`):
  - `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL** (53s)
  - `./gradlew :app:testDebugUnitTest` → **BUILD SUCCESSFUL**, only `NetManagerTest.kt` runs (1-2 trivial tests, see below)
- Deadline context: user's license expires **2026-09-28** — prioritizing a working, tested release candidate over completeness of every checklist item.

## Confirmed findings (superseding the 2024-09-29 audit, verified against current checkout)

All prior audit points independently reverified true as of this session:
- Java/XML app, custom Canvas gameplay (`PlayingAreaView` extends `View`, draws via `Canvas`/`Path`), Kotlin only in one test file.
- `compileSdk`/`targetSdk` 34, `minSdk` 21, `versionCode` 2, `versionName` "2.0" — [app/build.gradle](app/build.gradle).
- AGP 8.6.1, Gradle 8.7, Kotlin 1.7.21 (top-level `build.gradle`, `gradle/wrapper/gradle-wrapper.properties`).
- ButterKnife 10.1.0 (`@BindView`/`@OnClick` in `MainActivity`), `kotlin-android-extensions` plugin applied, `jcenter()` in both root and app `repositories {}`.
- `testImplementation project(path: ':app')` in [app/build.gradle:47](app/build.gradle#L47) — self-dependency, serves no purpose, removing.
- `PlayingAreaView` height hardcoded `720dp`, ad `AdView` positioned with `layout_marginTop="720dp"` — [activity_main.xml](app/src/main/res/layout/activity_main.xml).
- `PlayingAreaView.onDraw()` calls `startMoveDown()` which cancels/recreates the `CountDownTimer` on every repaint — game state advancement is driven by rendering.
- `NetManager.canRotate()` (NetManager.java:68) only bounds-checks the rotated figure against grid width/height and checks `isNetFreeToMoveDown()` — never checks the rotated figure's actual destination cells against settled blocks beside it. Confirmed exploitable: rotating next to a settled stack can overlap it.
- No process-death save/restore: no `onSaveInstanceState` override anywhere, `MainActivity.onCreate` ignores `savedInstanceState`.
- `Handler().postDelayed(...)` calls in `PlayingAreaView.createFigureWithDelay()` and `onTopLineHasTrue()` are never captured/cancelled on teardown (`cleanup()` doesn't cancel them) — can fire after the view/activity is gone.
- `MainActivity.onBackPressed()` calls `loadRewardedAd()`/shows a rewarded ad **after** `super.onBackPressed()` already navigated back; `onUserEarnedReward` only `Log.d`s, grants nothing.
- Google's sample/test AdMod IDs (`ca-app-pub-3940256099942544/...`) hardcoded directly in main source (`MainActivity.java`, `activity_main.xml`), not confined to a debug-only config.
- No `POST_NOTIFICATIONS` runtime permission request found near `NotificationUtil` usage (Android 13+ requires it) — confirming via background survey agent.
- `Values.DEFAULT_COLOR = R.color.zFigure` — an Android *resource ID* (not a stable key or ARGB value) is the persisted default and is what gets written to `SharedPreferences` as the color choice.
- Only test coverage: `app/src/test/java/.../NetManagerTest.kt` (Kotlin) + `app/src/androidTest/.../ExampleInstrumentedTest.java` (the default generated instrumentation test).
- No root `.gitignore` existed — `.gradle/` caches, `local.properties`, `keystore.properties` (containing the real release keystore password) were tracked in git.
- README claims "No ads." despite `com.google.android.gms:play-services-ads` integration and an `AdView` + rewarded ad in the game screen.

## Phase plan

1. **Repo hygiene** — DONE (commit `1e46d15`): add `.gitignore`, untrack `.gradle/`, `local.properties`, `keystore.properties` (local copies preserved). Signing password remains in earlier history — flagged for user decision (rotate / private repo / history scrub), not acted on automatically.
2. **Build & dependency modernization** — in progress. AGP/Gradle/Kotlin/AndroidX/Ads SDK to latest mutually-compatible stable versions (research via background agent, no alpha/beta/RC). Remove JCenter, ButterKnife → View Binding, drop `kotlin-android-extensions`, remove `testImplementation project(':app')`, decide on minSdk (currently 21 — document any change).
3. **Ads compliance** — split debug/release ad unit IDs, remove sample IDs from main source, remove auto rewarded-ad-on-back, correct reward grant (earned-reward callback only, dedup), banner placement outside gameplay/controls, UMP consent flow, never block gameplay on ad failure, proper lifecycle cleanup (`AdView.pause/resume/destroy`).
4. **Gameplay reliability** — decouple the fall-timer/game loop from `onDraw()`, add real destination-cell rotation collision check, cancel all `Handler` callbacks in teardown, responsive (non-hardcoded-720dp) layout for the playing area.
5. **State persistence** — save/restore active game (board, current + next figure, score, settings) across process death, versioned + validated; preserve existing high scores; safe color-preference migration off resource IDs with fallback.
6. **Design refresh** — dark navy/charcoal + cyan/violet accent theme across home/game/pause/game-over/scores/settings, edge-to-edge + system-bar insets, responsive layout, accessible labels, touch targets, optional haptics.
7. **Tests** — unit tests for rotation/collision/line-clear/scoring, save/restore, preference migration; instrumentation smoke test; lint.
8. **Docs** — this file, README rewrite (drop "No ads" claim, document real feature set), release checklist (production AdMob IDs, signing, versionCode, Data safety, internal testing, manual QA).

## Files changed so far

- `.gitignore` (new)
- Untracked: `.gradle/**`, `local.properties`, `keystore.properties` (still present locally, gitignored)
- `gradlew` (restored executable bit)

## Commands run (all from repo root)

```
./gradlew :app:assembleDebug --console=plain   # BUILD SUCCESSFUL, 53s, 31 tasks
./gradlew :app:testDebugUnitTest --console=plain  # BUILD SUCCESSFUL, 21 tasks
```

## Open decisions / needs user input

- **Signing credential in git history**: `keystore.properties` (with the real release-signing password) was committed in earlier history and remains there since history isn't being rewritten automatically. Recommend: rotate the Play App Signing upload key password and/or scrub history — user's call.
- **Production AdMob IDs**: none available; release config will use Google's official sample/test IDs clearly isolated to a "not yet configured for monetized release" state until the user supplies real ad unit IDs.
- **minSdk 21**: keeping unless dependency research forces a bump; will document exact device-support impact if so.
- **Final versionCode**: pending confirmation against Play Console per user instruction — not incrementing blindly.

## Next steps

- Await background research: (a) latest mutually-compatible stable toolchain/dependency versions, (b) survey of remaining ~25 source/resource files not yet read directly (settings/start/score screens, notification permission handling, listeners, full color/dimens resources, existing test contents).
- Apply build.gradle modernization once versions are confirmed.
- Implement gameplay/ads/persistence/design phases with commits per logical unit, running build+tests after each.
