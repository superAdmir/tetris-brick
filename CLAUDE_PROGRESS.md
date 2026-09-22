# tetris-brick modernization — progress log

Reusable handoff doc. Keep this current so a new session can pick up immediately.

## Device QA session (2026-09-22, follow-up) — real device, not just build/lint/unit-test

A physical device (Samsung Galaxy S23 FE, SM-S711B, Android 16 / API 36) became available
over wireless ADB mid-session, after the previous session's emulator attempts (see the
"Verification blocker" section below - now resolved, kept for the record). This section
records what was **actually executed and observed**, distinguished from what's still
unverified, per the instruction not to claim success without having run the check.

**Device-safety finding first**: the device already had `com.tb.tetrisbrick.game`
installed at **versionName 5.0 / versionCode 5** - ahead of anything in this git
history (this repo's newest commit is still versionCode 2/"2.0"), confirming the repo
was stale relative to whatever's actually shipped. A debug-signed build cannot install
over a release-signed one with the same applicationId (signature mismatch), and forcing
it would require uninstalling the real app and losing its saved scores - explicitly
disallowed. Fixed by giving debug builds `applicationIdSuffix ".debug"` (commit
`b6e05dc`) so they install as a separate, non-conflicting package. Confirmed via direct
`aapt2 dump badging` on the built APK that the suffix was actually applied *before*
installing anything, and confirmed after installing that the real
`com.tb.tetrisbrick.game` package was untouched (already absent from the device before
any action here, for reasons outside this session's actions - not caused by any install
performed here).

**What was executed** (not just compiled/reviewed):
- `./gradlew :app:assembleDebug` → installed via `adb install` → **launched and driven
  interactively via `adb shell input tap` + `uiautomator dump` for exact coordinates**.
- `./gradlew :app:connectedDebugAndroidTest` → ran on-device. First run **failed** (a
  real, if minor, bug this session introduced: `ExampleInstrumentedTest` hardcoded the
  literal package name, which no longer matched once the debug variant got a suffix).
  Fixed to compare against `BuildConfig.APPLICATION_ID`; second run **passed 1/1**.
- `./gradlew :app:testDebugUnitTest` → 28/28 passing (Robolectric).
- `./gradlew :app:lintDebug` → 0 errors.

**Screens visually inspected via screenshots** (11 captured, `adb exec-out screencap`):
Start screen (dark theme, cyan accents, edge-to-edge, test banner ad correctly bottom-
anchored) → POST_NOTIFICATIONS permission dialog appeared on first launch (confirms
that flow fires on-device, not just in code) → Game screen: a falling piece landing
under real gravity (score 0 → 10 automatically, no input), NEXT preview updating,
vertical hint lines → Pause (icon switches to play, score freezes) → Resume → Back
navigation to Start (clean, no rewarded-ad interference - confirms that removal is
effective on-device, not just in code) → Scores screen (a real high score of 60,
recorded from this session's play, correctly persisted and displayed with the
violet-accented top score) → Settings screen (color picker showing the correct default
checkmark on the red/Z-figure swatch, speed picker, squares-count picker, hints switch,
"Other" section with now-visible arrow icons - confirms the icon-color fix from the
design-refresh phase) → tapped a different color swatch, checkmark moved correctly →
started a new game, **the falling piece rendered in the newly-chosen purple color** -
full end-to-end confirmation that the color-preference migration/persistence work
(commit `b3487cd`) is correct on a real device, not just in Robolectric.
`uiautomator dump` bounds also directly confirmed no overlap between the control
buttons (bottom edge y=2054) and the ad banner (top edge y=2065), and that content
starts at y=97 (below the status bar) - the responsive-layout and edge-to-edge fixes
hold up on real hardware, not just in ConstraintLayout math.

**What was attempted but not cleanly captured**: a game-over screenshot. Rapid-fired
~60 taps at the move-down button to fill the board quickly; one stray tap landed on the
ad banner instead and opened an unrelated real app (Google Play Console, already
installed on this device) via the test ad's click-through - not a bug in this app, just
an artifact of blind coordinate-tapping automation. Backed out and left the device
clean rather than continuing to fight it. Game-over logic itself (clears the saved
game, shows the "GAME OVER" toast, delayed finish) is covered by
`PlayingAreaViewTest.gameOver_clearsAnyPreviouslySavedGame` and was re-read/confirmed
correct in code this session, but **not visually observed on-device**.

**Not tested this session** (flagging honestly rather than omitting):
- Small-screen device / a physically different screen size (only this one 1080x2340
  device was available).
- Larger system font scale (device's font size setting was not changed).
- An actual old-APK-then-new-APK **upgrade install** on a device (the device's existing
  install was versionCode 5, newer than anything buildable from this repo, so a literal
  "install old, then install new over it" on-device upgrade test wasn't possible with
  the artifacts available here). The upgrade/migration *logic* (legacy color key,
  high scores, settings all surviving together) is covered by
  `SharedPreferencesManagerTest.upgradeInstall_highScoresAndSettingsSurviveAlongsideColorMigration`,
  which simulates the legacy on-disk state directly - a real device-level upgrade
  install was not performed, and that distinction matters per the instruction not to
  claim a successful upgrade migration from unit tests alone.
- Rotation-near-wall/floor/occupied-cell and multi-row-clear scenarios were not
  specifically staged and screenshotted on-device (they're covered by 6 NetManagerTest
  cases and were working correctly during normal on-device play, but weren't
  deliberately engineered into frame the way the color-persistence test was).

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
2. **Build & dependency modernization** — DONE (commit `28bc094`). See "Toolchain version table" below. `./gradlew :app:assembleDebug` and `:app:testDebugUnitTest` both green on this commit.
3. **Ads compliance** — DONE (commit `dbcb3e4`). AdsManager (consent -> init -> banner), debug/release ad ID separation via admob.properties, rewarded ads removed (were already non-functional - see commit message), AdView lifecycle, exported="false" hardening.
4. **Gameplay reliability** — DONE (commits `4376a6f`, `5d097d2`). Gravity decoupled from onDraw(), real rotation collision check, Handler cleanup, POST_NOTIFICATIONS runtime request, PendingIntent immutable, multi-line-clear fixed for non-contiguous full rows. Added Robolectric for real Android framework behavior in unit tests (SDK pinned to 34 - targetSdk 36 needs Java 21, toolchain is 17). 8/8 unit tests passing.
   - **Still open from this phase**: responsive (non-hardcoded-720dp) layout for the playing area/ad banner - deferred into the design-refresh phase since it requires the same layout rework.
5. **State persistence** — DONE.
   - **Color-preference migration**: `SharedPreferencesManager` used to store the figure color as a raw `R.color.xxx` resource ID (an int), which is only stable within a single build - AAPT2 can (and, given how much colors.xml changed in this pass, almost certainly did) renumber it. Migrated to a stable string key (`Values.FIGURE_COLOR_*` constants, e.g. `"l_figure"`) stored under a new preference key (`default_color_v2`); `Utils.resolveColorResId(key)` resolves it to the *current* build's actual resource ID on every read, so renumbering can never point at the wrong (or a nonexistent) resource. On first read after upgrade, if only the old int-keyed entry exists, it is **not** reinterpreted (an old ID can't be trusted to still mean the same color) - it falls back to the default color key and the legacy entry is removed, per "provide a safe fallback" rather than trying to recover the exact old choice. Covered by `SharedPreferencesManagerTest` (fresh install, legacy-only, current-format, both-present cases).
   - High scores (`first/second/third_value`) were already stored as plain ints, not resource IDs - no migration needed there, preserved as-is.
   - **Active-game save/restore**: new `GameStateStore`/`SavedGame` (in `data/`) persist the board (`NetManager.getNetSnapshot()`, flattened to a bit-string), the falling figure's type + grid position, the next-figure preview type, the score, and the board width, versioned (`SAVED_GAME_SCHEMA_VERSION`) and fully validated on load (wrong version, dimension mismatch, corrupted cell string, or an unrecognized figure-type name all return `null` rather than throwing or restoring a broken board). `MainActivity.onCreate()` uses the standard Android signal for "this is a recreation, not a fresh launch" (`savedInstanceState != null`) to choose `PlayingAreaView.restoreGameIfAvailable()` over `startFreshGame()`; the save itself is written in `onStop()` (so it survives whether or not `onDestroy()` ever runs - the actual process-death case) and cleared on game-over or when a genuinely new game starts. Restored games always come back **paused** (`isTimerRunning=false`, pause icon shown) rather than immediately resuming the fall. Figure reconstruction needed a new `FigureFactory.getFigureAtGridPosition()` - the existing 5-arg `getFigure()` overload is rotation-specific (it assumes its `point` argument is the *pre-rotation* figure's position and applies bounding-box adjustments for the transition), which would have misplaced a figure reconstructed fresh from a save. Covered by `GameStateStoreTest` (round-trip of every field, and each validation-rejection case).
6. **Design refresh** — DONE for the "cohesive dark theme + responsive layout" core, plus haptics added afterward (commit `5ff14b4`). Dark navy/charcoal + cyan/violet accent theme applied across all 4 screens (colors.xml/styles.xml + every drawable/layout touching color). Fixed the 720dp/680dp/700dp hardcoded ad-banner-placement bug on all 4 screens: every screen is now a ConstraintLayout with the AdView pinned to the bottom (`wrap_content` height, `layout_constraintBottom_toBottomOf="parent"`) and game/menu content filling the space above it via `layout_constraintBottom_toTopOf="@id/adView"` - no more fixed-height assumptions. Added edge-to-edge system-bar inset handling (`EdgeToEdgeUtils`, required now that targetSdk 36 enforces edge-to-edge) to all 4 activities. `Switch` -> `SwitchCompat` for consistent theming. Haptic feedback (`performHapticFeedback`, respects the system setting) on rotate/move-down/pause taps, line-clear, and game-over. Lint clean (0 errors; only pre-existing/deliberate warnings remain, e.g. intentional portrait lock).
   - **Not done / not verified**: actual on-device rendering, font-scaling and small-screen behavior, and how the haptics feel - all blocked on the no-working-emulator issue below. The layout math and lint are clean, but nobody has looked at a rendered screen.
7. **Tests** — DONE for what's unit-testable. 19 Robolectric-backed unit tests total: `NetManagerTest` (init, rotation collision x4, line-clear x2), `SharedPreferencesManagerTest` (color migration x4), `GameStateStoreTest` (save/restore round-trip + 5 validation-rejection cases). `ExampleInstrumentedTest` modernized to current androidx.test APIs and confirmed to compile (`:app:compileDebugAndroidTestSources`), but **not executed** - no working emulator/device in this environment. Lint (`:app:lintDebug`) clean.
8. **Docs** — DONE. This file (kept current throughout); `README.md` rewritten (dropped the false "No ads" claim, documented the real feature set including save/restore and the ad/consent behavior, removed the "share your score" feature claim since that code path was found to be dead/unwired - `Values.SHARE_INTENT_TYPE` and `R.string.share_body_part_second` exist but nothing ever constructs a share `Intent`, confirmed via lint's unused-resource warning and a full-codebase grep); `RELEASE_CHECKLIST.md` added covering signing, production AdMob IDs, version, Play Console Data safety, internal testing, and the manual-QA items that still need a real device specifically because of the emulator blocker.

## Emulator blocker (2026-09-21) — root-caused and resolved 2026-09-22

Original attempt: booting an AVD (`tetris_test`, Pixel 5 profile, android-35 google_apis
x86_64 system image, manually created since `avdmanager` in the legacy `tools/bin`
package fails with `NoClassDefFoundError: javax/xml/bind/...` under JDK 17) via
`emulator -avd tetris_test` started (logged through "Started GRPC server") then died
with exit code 139 (SIGSEGV), invoking an ARM QEMU backend against an x86_64 image.
At the time this was attributed to "no hardware virtualization available in this
sandbox."

**That conclusion was wrong.** A follow-up session checked properly (host arch, HVF
support, matching system image - exactly what should have been checked the first time
instead of concluding "incompatible host" from one crash): this Mac is genuinely
x86_64 with `kern.hv_support: 1` (HVF available), and the installed system image is a
native x86_64 match. The actual cause was a config bug in the manually-written AVD:
`config.ini` had `abi.type=x86_64` but was missing `hw.cpu.arch=x86_64` entirely -
without it, the emulator launcher guessed the wrong backend. Adding that one line let
the same AVD boot cleanly to a working, adb-connected device. Root cause, not a
retry-until-it-works fix.

In practice, a **real physical device** (Samsung Galaxy S23 FE) became available over
wireless ADB before the fixed emulator was needed for anything further - see the
"Device QA session" section at the top of this file for what was actually run and
observed on it. The emulator fix is recorded here in case a future session needs one
and hits the same "looks like no HVF" red herring.

## Toolchain version table (as of 2026-09-21, all latest stable / no alpha-beta-RC)

| Component | Before | After |
|---|---|---|
| AGP | 8.6.1 | 9.4.0 |
| Gradle | 8.7 | 9.7.1 |
| Kotlin | 1.7.21 | 2.4.20 (kotlin-android plugin removed; AGP 9 has built-in Kotlin support) |
| compileSdk | 34 | 37 (required by androidx.core 1.19.x) |
| targetSdk | 34 | 36 (Play's Aug 2026 target-API deadline; API 37 not yet mandated) |
| minSdk | 21 | **24** — play-services-ads 25.5.0 requires it; see decision note below |
| androidx.appcompat | 1.2.0-beta01 | 1.8.0 |
| androidx.constraintlayout | 2.0.0-beta4 | 2.2.2 |
| androidx.core-ktx | 1.2.0 | 1.19.0 |
| androidx.work(-ktx) | 2.7.1 | 2.11.2 |
| androidx.lifecycle-process | (none) | 2.11.0 (added, for later process-lifecycle-aware ad cleanup) |
| com.google.android.gms:play-services-ads | 22.0.0 | 25.5.0 |
| com.google.android.ump:user-messaging-platform | (none) | 4.0.0 (added, for consent flow) |
| junit | 4.13-beta-3 | 4.13.2 |
| mockito-core | 2.25.0 | 5.23.0 |
| androidx.test.espresso:espresso-core | 3.3.0-alpha01 | 3.7.0 |
| androidx.test.ext:junit | (none) | 1.3.0 (added) |
| com.jakewharton:butterknife | 10.1.0 | removed (View Binding) |
| io.github.ShawnLin013:number-picker | 2.4.13 | 2.4.13 (unchanged — unmaintained since 2021, flagged as a risk, no alternative substituted since replacing it is out of scope) |
| org.jetbrains:annotations | 15.0 | removed (unused) |

**minSdk 21 → 24 decision**: `play-services-ads:25.5.0`'s own manifest requires minSdk 24; there is no current, policy-compliant Ads SDK release that still supports API 21-23. Devices on Android 5.0-6.0 (API 21-23) are a vanishing fraction of the active install base by late 2026. Proceeded without asking since the alternative (shipping a years-stale, non-compliant Ads SDK, or dropping ads against explicit user authorization) is clearly worse — flagging here per the instruction to document minSdk impact rather than change it silently.

### Independent re-verification (2026-09-22 follow-up session)

All of the above was independently re-verified against official sources in a follow-up session (not just re-trusting this table), plus one piece of hard local evidence:

- **minSdk 24 requirement confirmed via direct artifact inspection**, not just the earlier build-log error: unzipped `play-services-ads-25.5.0.aar` from the Gradle cache and read its own `AndroidManifest.xml` directly - it declares `<uses-sdk android:minSdkVersion="24" .../>` under package `com.google.android.gms.ads.impl`. Cross-checked `user-messaging-platform-4.0.0.aar` too: declares `minSdkVersion="23"` (lower, not the binding constraint). Confirmed the app's own merged manifest (`app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml`) resolves to exactly `minSdkVersion="24" targetSdkVersion="36"` with no other dependency pushing it higher.
- **AGP 9.4.0 / Gradle 9.7.1 / Kotlin 2.4.20 mutual compatibility**: confirmed via developer.android.com and JetBrains' own compatibility tables. AGP 9.4.0 requires Gradle >= 9.6.0 (9.7.1 satisfies it). AGP 9.0+ genuinely requires *not* applying the separate `org.jetbrains.kotlin.android` plugin - Android's own migration guide confirms applying it now produces a build error, so the project's setup (only `kotlin-parcelize` applied) is the documented-correct configuration, not an oversight. Kotlin 2.4 only requires AGP >= 8.5.2, so no conflict there either.
- **Why compileSdk 37 but targetSdk 36 (the trickiest claim to justify) - confirmed legitimate**: (a) Android API 37 (Android 17) is genuinely stable since 2026-06-16 and AGP 9.4 supports compiling against it; (b) Google Play's current *binding* deadline is targetSdk 36 by 2026-08-31 (an API-37 deadline is not yet in effect, expected ~a year out) - per Play's own "target API level requirements" page, so targetSdk 36 is fully compliant today and bumping to 37 is optional, not required; (c) `androidx.core:core-ktx:1.19.0` (already a dependency here) itself requires compileSdk >= 37 to build at all. So compileSdk is forced up by a build-time dependency requirement, while targetSdk is deliberately held at the current Play policy floor rather than opted into untested API-37 behavior changes early. This is standard, Google-sanctioned practice, not a mismatch to fix.
- **Every dependency version confirmed real and currently published stable** (no alpha/beta/RC, no typos) against Google Maven metadata, Maven Central, and the respective SDKs' own release-notes pages. Two items have newer *patch* releases available that this pass deliberately did not chase (per "do not change versions just to make numbers match"): AGP 9.4.1 supersedes 9.4.0, and Robolectric 4.16.1/4.17 supersede 4.16. Neither has a known issue affecting this project; noting for a future bump, not treating as a defect.

## Commit log (modernize-2026 branch, oldest first)

1. `1e46d15` — repo hygiene: `.gitignore`, untrack `.gradle/`/`local.properties`/`keystore.properties`
2. `607131d` — this progress log, created
3. `28bc094` — toolchain/dependency modernization, ButterKnife → View Binding
4. `61b4a42` — progress log update
5. `dbcb3e4` — ads compliance (AdsManager, debug/release ID split, rewarded ads removed)
6. `4376a6f` — gravity/rendering decoupling, rotation collision fix, POST_NOTIFICATIONS
7. `5d097d2` — multi-line clear fix for non-contiguous rows
8. `e414b58` — dark theme + responsive layout (fixes the 720dp ad-placement bug) + edge-to-edge
9. `b3487cd` — color-preference resource-ID migration
10. `ce8b613` — active-game save/restore across process death
11. `5ff14b4` — haptics + instrumentation test modernization
12. `8654ff7` — README rewrite, RELEASE_CHECKLIST.md, first progress log finalization
13. `1c7cc1c` — (follow-up session) release ad-config hardening (fail-fast on missing/sample production IDs), AdListener diagnostics, restore/migration regression tests (PlayingAreaViewTest, upgrade-scenario SharedPreferencesManagerTest)
14. `93a857b` — independent toolchain re-verification, expanded signing-key guidance
15. `b6e05dc` — debug applicationIdSuffix (device-safe testing) + the instrumentation test bug it exposed and fixed
16. (this commit) — device QA session results, final handoff update

## Open decisions / needs user input

- **Signing credential in git history**: `keystore.properties` (with the real release-signing password) was committed in earlier history and remains there since history isn't being rewritten automatically.
  - **`keyAlias=upload`** in the (currently untracked, locally-present) `keystore.properties` is a strong signal this is the **Play App Signing upload key**, not Google's own held app-signing key - "upload" is the exact alias name Android Studio's signed-bundle wizard suggests by default for an upload key, and is the standard convention. This matters a lot for how serious the exposure is:
    - If Play App Signing is enabled for this app (mandatory for all apps published since Aug 2021; likely already the case here) - the exposed password only lets someone sign an *upload*, not a package Play would actually distribute to users, since Play re-signs every release with its own separately-held app signing key before distribution. **To check**: Play Console → your app → Setup → App integrity (sometimes labelled "App signing") - it lists both the upload key certificate and the app signing key certificate separately if Play App Signing is active.
    - **If it's the upload key**: Play Console has a self-service "Request upload key reset" flow on that same App integrity page - generate a fresh upload keystore, register it, and the old (exposed) one is invalidated for future uploads. This does not affect the app's identity as seen by users or already-installed copies.
    - **If Play App Signing was never enabled** (self-managed signing, upload key *is* the distribution key) - this is more serious; there's no automatic self-service reset. Contact Google Play support's signing-key-reset process (restricted, for lost/compromised keys), or as a last resort republish under a new applicationId (loses review/install history).
  - Not acted on automatically (no history rewrite, no credential rotation) per instruction - this is the owner's call once they've checked which case applies. See `RELEASE_CHECKLIST.md` §1.
- **Production AdMob IDs**: none available; release config falls back to Google's official test IDs (`BuildConfig.ADS_CONFIGURED_FOR_RELEASE` reflects whether real ones were found) until the user supplies real ad unit IDs via `admob.properties`. See `RELEASE_CHECKLIST.md` §2.
- **Final versionCode**: still `2`/`"2.0"` in this repo, unchanged — pending confirmation against Play Console per user instruction, not incrementing blindly. **Important new evidence**: the physical test device already had this app installed at **versionCode 5 / versionName "5.0"** - confirming the real, currently-shipped app is well ahead of what's in this git history. Whatever versionCode this branch eventually ships **must** be set to at least 6 (checked against the actual Play Console listing, not assumed from this number) or the upload will be rejected as a downgrade. See `RELEASE_CHECKLIST.md` §3.
- **Debug package identity**: debug builds now install as `com.tb.tetrisbrick.game.debug` (commit `b6e05dc`), not `com.tb.tetrisbrick.game` - a deliberate, standard Android practice so a debug build can never conflict with (or require uninstalling) a real install. Release builds are unaffected.
- **Rewarded ads**: removed rather than fixed (see phase 3 above) — reintroduce only with a real, designed, tested opt-in reward if wanted.
- **`io.github.ShawnLin013:number-picker`**: unmaintained since 2021 (confirmed via the toolchain research pass), still in use for the squares-per-row picker in Settings. Not replaced — no drop-in replacement evaluated, out of scope for this pass, but worth flagging as an ongoing dependency risk.

## Next steps for a future session

- Real-device QA is now done for the core flows (see "Device QA session" above) - the remaining gaps are: small-screen device, larger font scale, an actual on-device upgrade install (old APK → new APK), and a clean game-over screenshot (attempted, interrupted by a stray ad-click during rapid automated testing - not a bug, just didn't get a clean capture).
- **Reconcile versionCode against Play Console before any release** - this repo's history and the real shipped app have diverged (see the versionCode note above); don't assume this branch's `2` is meaningful, check the actual next-available versionCode in Play Console.
- Supply production AdMob IDs and a real release keystore, then do a full release-build dry run (the build will now refuse without real IDs or the explicit local-testing opt-in - see commit `1c7cc1c`).
- Decide on the git-history signing-credential exposure (rotate vs. scrub vs. accept the risk) - check Play Console's App integrity page first to determine whether the exposed credential is the upload key (self-service reset available) or something more serious.
- If desired: replace the unmaintained number-picker dependency, and design a real opt-in rewarded-ad benefit rather than leaving rewarded ads removed.
