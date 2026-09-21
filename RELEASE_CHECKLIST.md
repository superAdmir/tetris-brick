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
      `1e46d15`). The password is still readable in earlier commits. Before this repo is
      made public (if it isn't already) or before relying on that password long-term,
      consider rotating the key's password via Play App Signing, and/or scrubbing it
      from git history. This was flagged, not acted on automatically - it's your call.
- [ ] Run `./gradlew :app:assembleRelease` (or `bundleRelease` for an AAB) locally and
      confirm it signs successfully end to end.

## 2. AdMob production IDs

- [ ] Copy `admob.properties.example` to `admob.properties` (gitignored) and fill in
      your real `admobAppId` / `bannerAdUnitId` from the AdMob console.
- [ ] Without that file, release builds fall back to Google's official test ad IDs -
      safe to ship (never serves real ads, never crashes), but **earns no revenue** and
      shouldn't be the final shipped configuration. `BuildConfig.ADS_CONFIGURED_FOR_RELEASE`
      reflects whether real IDs were found at build time if you want to assert this in a
      CI check.
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
      Bump `versionCode` (and `versionName` if you want) to whatever comes after the
      last version actually published, per the Play Console listing - not blindly to 3.
- [ ] `applicationId` (`com.tb.tetrisbrick.game`) and signing identity are unchanged -
      this upload will update the existing listing, not create a new one.
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

## 6. Manual QA - what's been verified vs. what still needs a real device

This modernization pass could **not get a working Android emulator running** in its
sandboxed CLI environment (tried twice; the emulator invoked an ARM QEMU backend
against an x86_64 system image and segfaulted - looks like no hardware virtualization
available to that shell; see `CLAUDE_PROGRESS.md` for the exact commands/output).
Everything below was verified only via `./gradlew assembleDebug/lintDebug/
testDebugUnitTest` and careful manual code/XML review - **not** by actually running the
app. Please verify on a real device or Android Studio's emulator before shipping:

- [ ] Fresh install: new game, scores/settings persist correctly.
- [ ] Upgrade install (if you have the old APK): scores carry over; the figure-color
      preference falls back to the default color rather than crashing (see the
      color-migration section of `CLAUDE_PROGRESS.md` for why this specifically needs
      checking - it's the one path most likely to regress silently).
- [ ] Small and large screens, and at least one non-default font-scale setting - the
      responsive-layout rework (ad banner placement, playing-area sizing) was corrected
      based on ConstraintLayout math and lint, not observed rendering.
- [ ] Rotate a piece near a wall, the floor, and next to already-settled blocks -
      confirms the rotation-collision fix actually prevents overlap on-device.
- [ ] Clear a single row, then set up and clear two non-adjacent full rows at once -
      confirms the line-clear fix.
- [ ] Pause/resume repeatedly, background/foreground the app repeatedly, rapid button
      taps on rotate/move-down.
- [ ] Put the app in the background mid-game, force-stop it (or let the OS reclaim it),
      relaunch from the app icon: the game should come back paused, board and score
      intact, not a fresh board.
- [ ] Turn off network / airplane mode: game still plays normally, ad banner simply
      doesn't load, nothing blocks or crashes.
- [ ] Game over -> restart flow.
- [ ] Notification appears (and looks right) on a new best score, with
      POST_NOTIFICATIONS granted and with it denied (should just silently not notify,
      not crash).
- [ ] Haptics on rotate/move-down/pause/line-clear/game-over feel reasonable and
      respect the system haptics toggle (test with it off).
- [ ] Edge-to-edge: status bar and gesture-nav areas don't obscure the score panel,
      controls, or ad banner on a real device with gesture navigation enabled.
