# Release checklist

Concise, ordered list for turning this branch into a Play Store update. See
`CLAUDE_PROGRESS.md` for the full modernization history and rationale behind each item.

## 1. Signing

- [ ] Confirm `release.keystore` (or your actual keystore file) exists locally at the
      path `keystore.properties` points to (`storeFile=release.keystore`, repo root).
      It is **not** in this checkout or in git - only `keystore.properties` was ever
      tracked, and the keystore binary itself was never committed.
- [ ] **Security note**: `keystore.properties` (containing the real store/key password)
      was tracked in git history before this modernization branch untracked it (commit
      `1e46d15`). The password is still readable in earlier commits.
      - `keyAlias=upload` strongly suggests this is the **Play App Signing upload key**,
        not Google's own held app-signing key (Android Studio's default alias for an
        upload key is literally "upload"). Check Play Console → your app → Setup → App
        integrity: if Play App Signing is enabled (mandatory for apps published since
        Aug 2021), it lists the upload key and app signing key certificates separately.
      - **If it's the upload key**: use Play Console's self-service "Request upload key
        reset" on that same page - generates a fresh upload keystore and invalidates the
        old one for future uploads, with no effect on the app's identity for existing
        users.
      - **If Play App Signing was never enabled** (upload key = distribution key): more
        serious - there's no self-service reset; you'd need Google Play support's
        signing-key-reset process, or in the worst case republish under a new
        applicationId.
      - Not rotated or rewritten automatically - verify which case applies, then act.
- [ ] Run `./gradlew :app:assembleRelease` (or `bundleRelease` for an AAB) locally and
      confirm it signs successfully end to end.

## 2. AdMob production IDs

- [ ] Copy `admob.properties.example` to `admob.properties` (gitignored) and fill in
      your real `admobAppId` / `bannerAdUnitId` from the AdMob console.
- [ ] **A real release build (`assemble`/`bundle`) now fails fast** with an actionable
      Gradle error if `admob.properties` is missing, incomplete, or still has Google's
      sample IDs in it - it no longer silently falls back to test ads. If you genuinely
      need a release build without real IDs yet (testing signing/R8 locally, not for
      distribution), add `useTestAdsForLocalRelease=true` to `admob.properties` as an
      explicit, visible opt-in. `BuildConfig.ADS_CONFIGURED_FOR_RELEASE` still reflects
      whether real IDs were actually found, for any CI check you want to add on top.
- [ ] Rewarded ads were removed during this modernization (the old implementation never
      actually granted anything - see `CLAUDE_PROGRESS.md`). If you want them back,
      design a real, clearly-stated opt-in benefit first; don't re-add the dead
      implementation.
- [ ] Confirm the UMP consent flow (`AdsManager`) shows a consent form for EEA/UK test
      devices before requesting ads there (use AdMob's test device / geography override
      to verify without leaving the EEA).

## 3. Version

- [ ] `versionCode`/`versionName` in `app/build.gradle` are still `2` / `"2.0"` -
      **unchanged from before this modernization pass**, deliberately, per instruction to
      treat the final release versionCode as pending confirmation against Play Console.
      **Confirmed evidence this matters**: a real device used for QA in this pass
      already had this app installed at **versionCode 5 / versionName "5.0"** - the
      actually-shipped app has moved past this git history entirely. Do not bump to 3;
      check Play Console's actual current versionCode and set this to at least one
      higher than that.
- [ ] `applicationId` (`com.tb.tetrisbrick.game`) and signing identity are unchanged -
      this upload will update the existing listing, not create a new one. Note: **debug**
      builds now use `com.tb.tetrisbrick.game.debug` (an `applicationIdSuffix`, added so
      debug builds can be installed on a device that already has the real app without
      conflicting) - this does not affect release builds.
- [ ] `minSdk` moved from 21 to 24 in this pass (`play-services-ads` 25.5.0 requires it
      via its own manifest - see `CLAUDE_PROGRESS.md`). This drops support for Android
      5.0/5.1/6.0 devices, a vanishing fraction of the active install base by now, but
      worth a one-line mention in your release notes if you track device-support changes.

## 4. Play Console Data safety form

Not filled out for you - verify against the actual implementation before submitting:

- The app itself stores only local `SharedPreferences` data: high scores, gameplay
  settings (color/speed/hints/board-width), and an in-progress-game save for process-
  death restore. None of this leaves the device or identifies the user.
- `POST_NOTIFICATIONS` is requested (Android 13+) to show a local "new best score"
  notification - no server-sent notifications, no push token.
- Google Mobile Ads SDK (`play-services-ads`) and the UMP consent SDK are integrated.
  Per Google's own SDK data-safety disclosures, this means the app does collect and
  share advertising-related data (device/advertising identifiers, etc.) with Google for
  ad serving - declare this in the Data safety form per
  https://support.google.com/googleplay/android-developer/answer/10787469, don't assume
  "no data collected."
- No accounts, no login, no user-generated content leaving the device, no location.

## 5. Internal testing

- [ ] Upload the signed AAB/APK to an Internal testing track in Play Console.
- [ ] Confirm the store listing's "No ads" claim (if still present from the old
      listing) is corrected - the app does show banner ads. The in-app README was
      already corrected in this pass; the Play Store listing itself is a separate,
      manual edit in Play Console.

## 6. Manual QA - what's been verified vs. what still needs checking

A real device (Samsung Galaxy S23 FE, Android 16/API 36) was used for interactive QA
in a follow-up session over wireless ADB - see the "Device QA session" section at the
top of `CLAUDE_PROGRESS.md` for the full detail. Checked items below reflect what was
**actually observed on-device**, not just reasoned about from code.

- [x] Fresh install + new game: piece falls under real gravity, lands, scores
      automatically (0 → 10 → ...), NEXT preview updates. Observed via screenshots.
- [x] Settings → gameplay round trip: picked a different brick color in Settings,
      checkmark moved correctly, started a new game, **the falling piece rendered in
      the newly-chosen color** - confirms the color-preference persistence end to end.
- [x] Pause/resume: icon toggles correctly (pause ⇄ play), score freezes while paused.
- [x] Back navigation from the game screen: clean, no rewarded-ad interference.
- [x] Scores screen: a real high score (60) earned during this session's play was
      correctly recorded and displayed.
- [x] POST_NOTIFICATIONS permission dialog appears on first launch.
- [x] Edge-to-edge / layout: `uiautomator dump` bounds confirmed no overlap between
      the controls (bottom edge y=2054) and the ad banner (top edge y=2065), and that
      content correctly starts below the status bar (y=97) - not just ConstraintLayout
      math, actually measured on a live device.
- [x] `connectedDebugAndroidTest` executed on-device: 1/1 passing (after fixing a real
      bug this exposed - see `CLAUDE_PROGRESS.md`).
- [ ] **Game over → restart flow**: attempted via rapid automated input but a stray tap
      hit the ad banner instead and opened an unrelated app - did not get a clean
      capture. Game-over logic is unit-tested and was re-confirmed by code review, but
      not visually observed. Re-attempt manually (not via blind rapid-tap automation).
- [ ] **Upgrade install** (old APK → new APK on the same device): not performed. The
      test device's existing install (versionCode 5) is newer than anything buildable
      from this repo, so a literal old→new upgrade wasn't possible with the artifacts
      on hand. The migration *logic* is unit-tested
      (`SharedPreferencesManagerTest.upgradeInstall_...`) against simulated legacy data,
      which is not the same as a real device-level upgrade install - do this once a
      real prior-version APK is available.
- [ ] Small-screen device (only one 1080x2340 device was available this session).
- [ ] Larger system font scale (device's setting was not changed during this session).
- [ ] Rotate a piece near a wall, the floor, and next to already-settled blocks - this
      logic is covered by 6 `NetManagerTest` cases and worked correctly during normal
      on-device play, but wasn't deliberately staged and screenshotted the way the
      color-persistence check was.
- [ ] Clear a single row, then set up and clear two non-adjacent full rows at once -
      same status as above (unit-tested, not deliberately staged on-device).
- [ ] Turn off network / airplane mode: game still plays normally, ad banner simply
      doesn't load, nothing blocks or crashes.
- [ ] Haptics on rotate/move-down/pause/line-clear/game-over feel reasonable and
      respect the system haptics toggle (test with it off) - the calls are wired in
      and ran without crashing during on-device play, but the physical *feel* wasn't
      specifically evaluated.
- [ ] Repeated background/foreground transitions specifically to confirm no duplicate
      fall-timer loops accumulate - verified by code review this session (the single
      mutable `timer` field + `CountDownTimer.start()`'s own restart semantics make a
      duplicate-loop bug structurally unlikely), not by a dedicated device test.
