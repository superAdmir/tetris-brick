# Release checklist

Concise, ordered list for turning this branch into a Play Store update. See
`CLAUDE_PROGRESS.md` for the full modernization history and rationale behind each item.

> **versionCode 6 / "6.0" is confirmed live in production** (owner-reported,
> 2026-09-25). This checklist's items below describe the verification work that led up
> to that release; they were all completed *before* the publish. No session in this
> history has uploaded to or directly observed Play Console - the production status
> above is the owner's own report, not something checked here. For the **next**
> release, start this checklist over: none of the checked items below re-verify
> automatically against a new versionCode.

## 1. Signing

- [x] **Play App Signing confirmed enabled** for `com.tb.tetrisbrick.game` (2026-09-23,
      owner-supplied Play Console screenshots): "Releases signed by Play" shown, the
      app-signing key is in use, "Request upload key reset" is available, and the
      original upload keystore is confirmed unavailable. This means Google already
      holds the real distribution key - only the local *upload* key (used to sign what
      you send to Play) needs replacing, via the self-service reset path below. This
      was the likely case per the `keyAlias=upload` evidence noted previously, and is
      now confirmed rather than assumed.
- [x] **New upload key created and verified** (2026-09-23): the owner generated it
      themselves (this session never had/supplied a password) at
      `/Users/AdmirSatara/tetris-brick-upload-key/tetris-brick-upload.keystore`
      (PKCS12, alias `upload`, RSA 2048, valid to 2054-02-08). This session
      independently verified the public certificate
      (`tetris-brick-upload-certificate.pem` in the same directory) via
      `openssl x509 -noout -fingerprint -sha256`: SHA-256
      `87:90:5E:14:DA:64:6D:60:BE:5E:F6:AB:10:45:59:27:11:20:2E:C8:5D:E5:74:5D:16:01:7A:4B:7E:23:C6:99`
      - an exact match to the owner-reported expected value. File permissions
      restricted (keystore `600`, directory `700`).
- [x] **Upload-key reset request: submitted by the owner, pending Google activation.**
      Visible as "pending" in Play Console per the owner. Not submitted by this
      session; Google's acceptance is not claimed.
- [x] **Local signing config updated and verified** (2026-09-23). The owner ran the
      Python helper (`~/tetris-brick-upload-key/configure_keystore_properties.py`) -
      it verified the keystore/alias and the certificate SHA-256 first, then wrote
      `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`)
      atomically with `600` permissions. Confirmed on this side: `storeFile` now points
      at `/Users/AdmirSatara/tetris-brick-upload-key/tetris-brick-upload.keystore`,
      file permissions are `600`.
- [x] **Local signed release build succeeds** (2026-09-23). Ran
      `./gradlew :app:assembleRelease` with no `useTestAdsForLocalRelease` override:
      `BUILD SUCCESSFUL`. Output APK:
      `app/build/outputs/apk/release/app-release.apk` (3,231,313 bytes). Verified with
      `apksigner verify --verbose --print-certs` (Android SDK build-tools 36.0.0):
      `Verifies: true`, signed with the new upload key, one signer, certificate SHA-256
      `87905e14da646d60be5ef6ab1045592711202ec85de5745d16017a4b7e23c699` - normalized
      and compared programmatically against the expected
      `87:90:5e:14:da:64:6d:60:be:5e:f6:ab:10:45:59:27:11:20:2e:c8:5d:e5:74:5d:16:01:7a:4b:7e:23:c6:99`
      (case/colons aside): **exact match**. `versionCode`/`versionName` read directly
      from the APK via `aapt2 dump badging`: `2` / `"2.0"` - unchanged, as required.
      APK was **not** installed (confirmed via `adb shell pm list packages`: only the
      pre-existing debug package is present on the connected device).
- [x] **Local signed release AAB also succeeds and is independently verified**
      (2026-09-23, with versionCode 6 - see §3). Ran `./gradlew :app:bundleRelease`, no
      `useTestAdsForLocalRelease`: `BUILD SUCCESSFUL`. Output:
      `app/build/outputs/bundle/release/app-release.aab` (6,021,813 bytes, SHA-256
      `999a2d28a9782cc23951493d438ac3e67acb64dfdaa56de87de450c2c6103baf`). Verified with
      AAB-appropriate tooling (an `.aab` is JAR-signed, not APK-signed, so `apksigner`
      doesn't apply): `jarsigner -verify -verbose -certs` → `jar verified.` (signature
      files literally named `META-INF/UPLOAD.SF`/`UPLOAD.RSA`), and `keytool -printcert
      -jarfile` → signer SHA-256
      `87:90:5E:14:DA:64:6D:60:BE:5E:F6:AB:10:45:59:27:11:20:2E:C8:5D:E5:74:5D:16:01:7A:4B:7E:23:C6:99`
      - exact match to the expected value, confirmed programmatically.
- [x] **`bundletool` structural validation complete** (2026-09-23, same day follow-up).
      `bundletool validate --bundle=app-release.aab`: exit 0, no errors. `bundletool
      dump manifest`: confirms `package="com.tb.tetrisbrick.game"`,
      `android:versionCode="6"`, `android:versionName="6.0"` directly from the AAB's own
      protobuf manifest - independent of the Gradle/`aapt2`-based checks above.
      Re-checksummed the AAB (`shasum -a 256`): unchanged, exact match to the value
      above - confirms the artifact wasn't modified between checks. See
      `CLAUDE_PROGRESS.md` "bundletool structural validation" for how bundletool was
      obtained (the earlier `brew install` had silently failed to actually install it
      despite exiting 0; the already-downloaded bottle was extracted and run directly
      rather than starting a second install).
- [x] **Google's upload-key reset: activation implied by the confirmed production
      publish** (2026-09-25) - versionCode 6/"6.0" is now live, per the owner, and a
      real Play Console upload cannot succeed signed with an unactivated upload key.
      This session did not observe the activation directly in Play Console; it's
      inferred from the publish having succeeded, not independently checked here.
- [ ] **Security note**: `keystore.properties` for the *old, lost* keystore (different
      keystore than the one above) had its real password tracked in git history before
      this modernization branch untracked it (commit `1e46d15`). Now moot for signing
      (replaced), but worth knowing if that old password was reused anywhere else.

## 2. AdMob production IDs

- [x] **Banner ad unit IDs configured** (2026-09-23): all four per-screen placements are
      set in the local, gitignored `admob.properties` (see `admob.properties.example`
      for the property names) - Home/start (`tbgMainBanner`), Gameplay (`tbgGameBanner`),
      Scores (`tbgScoreBanner`), Settings (`tbgSettingsBanner`). Each screen's `AdView`
      loads its own placement; confirmed via `:app:generateReleaseResValues` that each
      resolves to its correct distinct ad-unit ID, and confirmed on-device (debug build,
      test ads only) that all four screens load their banner without error.
- [x] **Production AdMob App ID configured** (2026-09-23, same day follow-up): the owner
      supplied `ca-app-pub-6402675413704299~8130164394` directly from the AdMob console
      (not derived from any ad-unit ID); it's now set as `admobAppId` in
      `admob.properties`. Re-verified via `:app:generateReleaseResValues` that the
      release resource XML resolves `admob_app_id` to this exact value, while debug's
      still resolves to Google's test App ID.
- [x] **AdMob production config is now fully complete and verified.** Re-ran
      `./gradlew :app:assembleRelease` (2026-09-23, without
      `useTestAdsForLocalRelease`, which remains absent/unused) and confirmed the AdMob
      gate now **passes** - the build proceeds past resource generation and compilation
      and fails only later, at `:app:validateSigningRelease` (missing keystore file -
      see §1, a separate, unrelated blocker). This confirms AdMob readiness
      independently of signing, with no live ad requests made.
      `BuildConfig.ADS_CONFIGURED_FOR_RELEASE` now evaluates `true` for a real release
      build.
- [ ] Rewarded ads were removed during this modernization (the old implementation never
      actually granted anything - see `CLAUDE_PROGRESS.md`) and remain removed as of
      this pass too, deliberately, even though production rewarded ad units exist. If
      you want them back, design a real, clearly-stated opt-in benefit first; don't
      re-add the dead implementation.
- [ ] Confirm the UMP consent flow (`AdsManager`) shows a consent form for EEA/UK test
      devices before requesting ads there (use AdMob's test device / geography override
      to verify without leaving the EEA).

## 3. Version

- [x] **versionCode/versionName finalized, built, and confirmed published**
      (2026-09-23 build, 2026-09-25 owner-confirmed publish): `app/build.gradle` has
      `versionCode 6` / `versionName "6.0"`. Owner originally confirmed the
      **complete** Play Console app-bundle list - versionCodes 1 through 5 existed
      across all tracks (production 5/"5.0", internal testing 1/"1.0") - making 6 the
      correct next value; confirmed present in the built artifacts via `aapt2 dump
      badging` and the AAB's packaged manifest. **The owner has since confirmed 6/"6.0"
      is live in production** - this is now a real, shipped version, not just a
      locally-verified candidate. For the next release, the floor is production's
      current 6, not the old "highest across all tracks" uncertainty.
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
- [ ] **Upgrade install** (old APK → new APK on the same device): still **not
      performed**, explicitly kept pending. Updated status as of 2026-09-23: this repo
      now builds versionCode 6, which *would* be newer than the versionCode 5 the test
      device previously had installed - so the versionCode-ordering obstacle noted
      earlier no longer applies in principle. However, the connected test device
      currently has **no plain `com.tb.tetrisbrick.game` (release) install at all** -
      only the separate `com.tb.tetrisbrick.game.debug` package (confirmed via
      `adb shell pm list packages`, 2026-09-23) - so there is nothing to literally
      upgrade *from* right now, and this pass deliberately did not install anything
      (per instruction: no install over an existing app). The migration *logic* remains
      unit-tested (`SharedPreferencesManagerTest.upgradeInstall_...`) against simulated
      legacy data, which is not a substitute for a real device-level upgrade install.
- [ ] **Device QA against the new versionCode 6 build**: **not performed this pass**,
      explicitly kept pending, per instruction to avoid repeated device QA for
      unchanged gameplay. Every real-device check above predates the versionCode 6
      release build (APK/AAB) produced in this pass and was run against the debug
      package, which carries its own independent versioning and is unaffected by the
      `versionCode`/`versionName` change. Nothing about gameplay, UI, or persistence
      changed in this pass, so this is a version-bump/signing verification pass, not a
      UI-affecting one - but a fresh on-device install of this *exact* release
      configuration has not been observed.
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
