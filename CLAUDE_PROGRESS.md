# tetris-brick modernization — progress log

Reusable handoff doc. Keep this current so a new session can pick up immediately.

## ⚠️ git hazard: back up local gitignored config before checking out an old branch

**Before running `git checkout <branch>` (or any ref switch/merge/reset), check whether
that target still *tracks* a file your current branch treats as local-only/gitignored
- e.g. `keystore.properties`, `admob.properties`, `local.properties`. If it does,
back up the local file's content first**, because git's "don't clobber uncommitted
work" protection does **not** apply to ignored files: a target branch/commit that
tracks a path will silently overwrite (or, on a later fast-forward, delete) whatever
ignored file already sits at that path, with no warning and no error - `git status`
looks completely clean throughout, because an ignored file was never "uncommitted work"
as far as git status is concerned.

**This actually happened in this repo** (2026-09-25): `main` predates
`modernize-2026`'s `1e46d15` commit (which stopped tracking `keystore.properties`), so
old `main` still tracked it with stale content. Running `git checkout main` (to
fast-forward and verify the just-merged PR) silently overwrote the real, working,
gitignored `keystore.properties` - which pointed at the newly-recovered signing
keystore - with that old tracked content, and the subsequent `git merge --ff-only`
deleted it outright once applying the merged tree (which also no longer tracks the
file). The keystore file itself, its certificate, and the local helper script that
writes `keystore.properties` were never touched and remained correct throughout - only
the small local pointer file was lost, recoverable by re-running that helper with the
same (unchanged) keystore password. Still, it shouldn't have happened, and the fix is
procedural, not code: **check `git ls-tree <target-ref> -- <path>` for every
gitignored local config file before switching refs**, and copy it somewhere safe first
if the target tracks it.

## Store-artwork screenshots + debug-only "screenshot mode" (2026-09-25)

Prepared `~/Desktop/tetris-brick-store-assets.zip` (6 fresh device screenshots at
native 1080x2340 + the production icon at 512x512) for Play Store listing artwork. No
prior screenshot on disk qualified - every one showed the debug test-ad banner - so all
six were recaptured. Release AAB untouched (same file, same timestamp as the prior
pass); nothing pushed or published; app data preserved throughout (existing best score/
saved game were still present in every capture and after).

**Added a small reusable capability**: `ScreenshotMode`
(`app/src/main/java/.../utils/ScreenshotMode.java`) - gated on `BuildConfig.DEBUG` plus
an explicit intent extra (`EXTRA_SCREENSHOT_MODE`), so a release build ignores it
unconditionally. When active, each of the four Activities skips its ad request entirely
(no consent flow, no network call, no test-ad creative) and hides the `AdView`
(`GONE`, so the layout reclaims the space) instead of loading one.
`StartActivity` forwards the extra to whatever it launches next
(`propagateScreenshotMode()`), since `MainActivity`/`ScoreActivity`/`SettingsActivity`
are correctly `exported="false"` and can't be launched directly via `adb shell am
start` - capturing them means navigating from Home exactly as a real user would, just
carrying the flag through. Triggered via
`adb shell am start -n .../StartActivity --ez com.tb.tetrisbrick.game.EXTRA_SCREENSHOT_MODE true`.
Verified: `assembleDebug`/`lintDebug` (0 errors, 52 warnings, unchanged baseline)/
`testDebugUnitTest` (33/33), plus a release-variant compile-only check (no AAB/APK
produced) to confirm `BuildConfig.DEBUG` gating doesn't break a release compile.

**Status bar cleaned for capture via Android's built-in Demo Mode** (`adb shell
settings put global sysui_demo_allowed 1` + `am broadcast -a
com.android.systemui.demo`) - hides personal notification icons/badges, standard
reversible OS feature, no app changes. Explicitly turned back off afterward
(`sysui_demo_allowed 0` + `command exit`) and confirmed via a follow-up screenshot that
the real status bar (actual notifications, actual battery %) returned.

**The six captures** (`screenshot-1-home.png` through `-6-settings.png` in the ZIP):
Home (showing the real Continue/best-score state - genuine app data, not staged),
active gameplay (a partially-built board with a highlighted falling piece, staged via a
few real drops rather than an empty fresh board, for a more representative shot),
paused overlay, the game-over dialog (staged by deliberately filling one column - the
same safe technique used in earlier QA passes, not blind rapid-tapping near the ad
area), Scores, Settings. All six: no ad banner, no debug marker (there isn't one
anymore - see the icon/splash passes below), no personal notifications, native device
resolution.

**Not done, per instruction**: no AAB rebuild, no upload, no publish, no push. The
`ScreenshotMode` code is left in the working tree (uncommitted, like everything else
this session pending an explicit commit request) as a reusable capability for future
artwork refreshes, not reverted after use.

## versionCode 6 / "6.0" is live in production (owner-confirmed, 2026-09-25)

**The owner has confirmed that Tetris Brick Game 6.0 (versionCode 6) is published to
production on Play Console.** This confirmation is the owner's own report - no session
in this history has ever uploaded, published, or had any access to Play Console
itself; nothing here claims to have independently checked Play Console's UI. The
upload-key reset (see "Upload-key recovery" below) must have been activated by Google
for this upload to have succeeded, since a real Play Console upload cannot go through
signed with an unactivated key - but that activation was observed only indirectly,
through the owner's report of a successful publish, not confirmed by this session
directly against Play Console.

**What this changes going forward**: `app/build.gradle`'s `versionCode 6` /
`versionName "6.0"` now describes the **live production app**, not a locally-verified
candidate. Any future version bump starts from **6**, not from the previously-uncertain
"highest across all tracks" question - production itself is now the known floor. This
session's work (git hygiene, doc updates, committing outstanding local changes) does
**not** change the version or behavior of that published app in any way - no source
changes in this pass affect app behavior, and no new release build was produced or
uploaded.

## Release readiness (history, prior to the production confirmation above)

**AdMob configuration, versionCode, and local signing were all completed and verified**
before the confirmed publish above. Historical context, preserved as-is:

Resolved 2026-09-23:
- **versionCode/versionName**: owner confirmed the **complete** Play Console app-bundle
  list - versionCodes 1 through 5 exist, 5 is the highest uploaded anywhere (production
  5/"5.0", internal testing 1/"1.0"). `app/build.gradle` now has **versionCode 6,
  versionName "6.0"** - no longer a proposal, actually set. `applicationId`
  (`com.tb.tetrisbrick.game`) unchanged.
- **AdMob**: production App ID + all four banners configured and verified (see "AdMob
  production configuration complete" below) - confirmed present in the actual shipped
  AAB's compiled resources this pass, not just the build config.
- **Signing**: local signed builds succeed for **both** `assembleRelease` (APK) and
  `bundleRelease` (AAB), each independently verified against the expected signer
  certificate fingerprint with the tooling appropriate to that artifact type
  (`apksigner` for the APK, `jarsigner`/`keytool` for the AAB - see "Local signed AAB"
  below) - both an exact match.

See `RELEASE_CHECKLIST.md` sections 1-3 and "Open decisions" below for full detail.

## AdMob production configuration complete (2026-09-23, two-part)

Configured the four supplied production banner ad unit IDs, then - once the owner
separately confirmed the production App ID - added that too. Both parts through the
project's existing gitignored `admob.properties` mechanism - extended, not replaced, to
support one placement per screen instead of a single shared banner. Gameplay,
persistence, signing, versionCode, and SDK versions untouched. No live/production ads
were requested or clicked during verification. Nothing pushed/published.

**Part 2 (App ID, same day, follow-up)**: `admob.properties`'s `admobAppId` was left
deliberately unset in part 1 pending the real value. The owner then supplied it
directly (`ca-app-pub-6402675413704299~8130164394`, copied from AdMob console, not
derived from any ad-unit ID) and it's now set. Re-verified with
`:app:generateReleaseResValues`: the release resource XML now resolves `admob_app_id`
to this exact production value (previously it fell back to Google's test App ID, which
was the correct behavior at the time - it genuinely was unconfigured). Debug's
`admob_app_id` resValue is unaffected, still Google's test App ID. **Directly re-ran
`./gradlew :app:assembleRelease`** without `useTestAdsForLocalRelease` (confirmed
absent from both `admob.properties` and `.example`): the AdMob release-config gate now
**passes cleanly** - the build proceeds through resource merging, compilation, and
manifest processing, and fails only later at `:app:validateSigningRelease` with
`Keystore file '.../app/release.keystore' not found for signing config 'release'` -
i.e. **AdMob configuration is verified independently of the (separate, still-open)
signing blocker**, exactly as required, with zero live ad requests made (the build
never reaches app installation or execution).

**What changed**:
- `admob.properties.example` documents the new property names (`admobAppId` unchanged;
  `bannerAdUnitId` replaced by `tbgMainBanner` / `tbgGameBanner` / `tbgScoreBanner` /
  `tbgSettingsBanner`, exactly matching the names supplied for production use).
- The local, gitignored `admob.properties` (never committed - confirmed via
  `git check-ignore`) now has all four real banner values. `admobAppId` is
  **deliberately left unset** - a comment in the file explains why, matching the
  explicit instruction not to derive it from an ad-unit ID.
- `app/build.gradle`: the old single `configuredBannerAdUnitId`/`releaseBannerAdUnitId`
  pair became a `bannerPlacements` list (one entry per screen, each tracking its own
  property name, generated resValue name, configured value, and production/test
  status). `hasProductionAdmobConfig` now requires the App ID *and* all four banners.
  The release-block error message is now **dynamic**: it names exactly which fields are
  still missing/placeholder, one line each, rather than a generic "no config found" -
  verified live (see below) to correctly list only `admobAppId` now that the banners
  are configured.
- Four new per-screen string resources (`main_banner_ad_unit_id`, `game_banner_ad_unit_id`,
  `score_banner_ad_unit_id`, `settings_banner_ad_unit_id`) generated via `resValue`,
  same mechanism as the existing `admob_app_id` - debug builds get Google's shared test
  banner ID for all four (Google's test units are placement-agnostic, not meant to
  vary), release builds get the four real values.
- Each of the four `AdView`s (`activity_start.xml`, `activity_main.xml`,
  `activity_score.xml`, `activity_settings.xml`) now points `ads:adUnitId` at its own
  screen-specific string instead of the old shared one. No other change to those
  layouts, `AdsManager.java`, or any Activity's ad lifecycle code - consent handling
  (`requestConsentThenLoadBanner`, privacy options), `ads:adSize="BANNER"` sizing,
  `onResume`/`onPause`/`onDestroy` cleanup, and offline/no-fill behavior
  (`onAdFailedToLoad` never retries, never blocks gameplay) are all byte-identical to
  before this pass.
- Rewarded ads remain removed - confirmed by grep, nothing rewarded-ad-related exists
  anywhere in `app/src` or `app/build.gradle`, and nothing was added despite production
  rewarded ad units apparently being available.

**Executed checks**: `./gradlew :app:assembleDebug` ✅, `:app:lintDebug` ✅ (0 errors,
52 warnings - unchanged baseline, no ad-related warnings), `:app:testDebugUnitTest` ✅
(33/33, no ad-related tests exist, unaffected). `:app:generateReleaseResValues` run
directly to inspect the generated release resource XML: confirmed all four banner
resValues resolve to the exact four supplied production ad-unit IDs, correctly mapped
one-to-one to their screens, while `admob_app_id` correctly still resolves to the
*test* App ID (confirming the "don't derive/fabricate it" instruction was honored - the
release config is genuinely incomplete, not silently patched over). **Directly ran
`./gradlew :app:assembleRelease`** (not just reasoned about the gate) and confirmed it
fails with the expected message, listing `admobAppId` as the only missing value - this
is the actual verification that "release validation stays blocked with a clear
missing-App-ID message," not just code review.

**Verified on-device** (Galaxy S23 FE, existing debug package reused via
`installDebug` - not uninstalled, no data cleared, confirmed by `BEST 150` and the
in-progress saved game still present throughout): launched the Home screen and
navigated to Scores, Settings, and Gameplay (via Continue, which correctly restored the
paused saved game) - all four screens loaded their `AdView` successfully showing
Google's test-ad creative (debug always uses test ads, so this involved no live/
production ad request), no crash, no layout regression. **No ad was tapped/clicked at
any point.** Debug builds show the identical test creative on all four screens by
design (Google's test IDs aren't placement-distinct), so this on-device pass confirms
the wiring didn't break anything functionally; the actual per-screen ID *mapping*
correctness was verified via the `generateReleaseResValues` inspection above, which is
the only way to observe four *different* real IDs without requesting live ads.

**Not applicable / out of scope this pass**: no UMP/consent-flow changes were made, so
the existing EEA/UK consent-form checklist item in `RELEASE_CHECKLIST.md` §2 remains
as-is, still unverified in this pass (no geography override available on the test
device). Signing and versionCode are unrelated blockers, untouched - see the section
above. The App ID follow-up (part 2) deliberately did **not** repeat the on-device QA
pass from part 1 - nothing UI/gameplay-visible changed (only a `BuildConfig`/resValue
string), so build+lint+the direct `assembleRelease` gate check were sufficient; see
"Avoid ... repeated device QA for unchanged gameplay" in the originating instruction.

## Upload-key recovery — key created, wired in, and a signed APK verified (2026-09-23)

**Play App Signing is confirmed enabled** for `com.tb.tetrisbrick.game` (owner-supplied
Play Console screenshots: "Releases signed by Play" shown, app-signing key in use,
"Request upload key reset" available, original upload keystore confirmed unavailable).
Google already holds the real distribution key; only the *upload* key (used locally to
sign what gets sent to Play) needed replacing.

**New upload key: created by the owner, independently verified by this session.** The
owner ran the prepared `keytool` commands themselves (this session never had or
supplied a password), producing:
- Keystore: `/Users/AdmirSatara/tetris-brick-upload-key/tetris-brick-upload.keystore`
  (PKCS12, alias `upload`) - outside the repo, as required.
- Public certificate: `/Users/AdmirSatara/tetris-brick-upload-key/tetris-brick-upload-certificate.pem`.

This session verified the **public PEM** directly (no password needed - it's not secret
material): `openssl x509 -noout -fingerprint -sha256` on the certificate produced
`87:90:5E:14:DA:64:6D:60:BE:5E:F6:AB:10:45:59:27:11:20:2E:C8:5D:E5:74:5D:16:01:7A:4B:7E:23:C6:99`
- an **exact match** to the fingerprint the owner reported from Play Console. Also
confirmed independently from the PEM: RSA 2048-bit key, validity from 2026-09-23 to
2054-02-08 (~27.4 years, comfortably over the 25-year minimum).

**Upload-key reset request: submitted by the owner, pending Google activation.** Not
submitted by this session, and Google's acceptance is not claimed here - Play Console
shows it as pending, per the owner.

**Secure local backup: completed by the owner** (their report - this session has no way
to independently verify a backup that lives outside anything it can inspect).

**File permissions tightened this pass** (all local filesystem operations, no secrets
read or written): `tetris-brick-upload.keystore` → `600` (was world-readable `644`),
the containing directory → `700`, the *existing* (still old-content) repo-root
`keystore.properties` → `600`. The public `.pem` was left at `644` - it's meant to be
uploaded, not secret.

**Local signing configuration: NOT yet updated - this is the one remaining local step.**
`keystore.properties` still has its old content (pointing at the missing
`app/release.keystore`, per the unrelated prior blocker). Property names confirmed by
inspecting the file's keys only, never its values: `storeFile`, `storePassword`,
`keyAlias`, `keyPassword` - exactly what `app/build.gradle`'s `signingConfigs.release`
block reads.

A first attempt at a helper for this was a shell snippet; the owner correctly flagged
three real problems with it (bash-only `read -p` syntax despite a zsh-compatibility
claim, writing `keystore.properties` *before* verifying anything, and no Java
Properties escaping for the password value) before running it - none of it was
executed. **Replaced with a proper local Python 3 helper**,
`~/tetris-brick-upload-key/configure_keystore_properties.py` (outside the repo,
permissions `700`, owner-only). It asks for the keystore's **existing** password
(chosen when the owner created it - this script never changes it), confirmed twice via
`getpass` (hidden input, no echo). It then, in this order: (1) verifies the keystore
and alias actually open with that password via `keytool -list -v ... -storepass:env`
(password passed through the child process's environment block, never a literal
argument); (2) exports the certificate to a `tempfile`-created temp file and computes
its SHA-256 directly in Python (`hashlib`, no `openssl` dependency), comparing against
the fingerprint documented above - the temp file is always removed in a `finally`
block; (3) **only if both checks pass**, writes `keystore.properties` atomically
(`tempfile.mkstemp` in the same directory + `os.replace`) with permissions `600` set
before any content is written. If either check fails, the file is left completely
untouched - verified by code inspection (the write path is only reachable after both
`fail()`-raising checks have passed). The password is never printed, logged, or
included in any subprocess argv.

The Java-Properties escaping (`escape_properties_value`) was unit-tested in isolation
this session (no keystore/password involved) against a hand-rolled unescaper mirroring
`java.util.Properties.loadConvert`: leading spaces, backslashes, `= : # !`, tab/
newline/CR/formfeed, accented Latin-1 characters, and an astral-plane emoji (via UTF-16
surrogate-pair escaping, matching how the JVM itself represents codepoints above
U+FFFF) all round-tripped correctly. The SHA-256-to-colon-hex formatting was separately
checked against a known test vector.

**The owner ran the helper: succeeded.** Keystore and alias opened; certificate
SHA-256 matched the expected fingerprint exactly; `keystore.properties` was written
with permissions `600`.

**Local signed release build: succeeded end to end** (2026-09-23). Ran
`./gradlew :app:assembleRelease` with no `useTestAdsForLocalRelease` override
(confirmed absent from `admob.properties` beforehand) - `BUILD SUCCESSFUL`,
`:app:validateSigningRelease` passed (previously the point of failure). Output:
`/Users/AdmirSatara/tetris-brick/app/build/outputs/apk/release/app-release.apk`
(3,231,313 bytes).

**Signature independently verified**, not just inferred from build success: ran
`apksigner verify --verbose --print-certs` (from
`~/Library/Android/sdk/build-tools/36.0.0/`) against the APK directly.
`Verifies: true` (v2 scheme), one signer, certificate DN
`CN=Admir Satara, OU=superadmir, O=superadmir, L=Sarajevo, ST=BH, C=BH` (matches the
upload key's own DN), RSA 2048. **Signer certificate SHA-256:
`87905e14da646d60be5ef6ab1045592711202ec85de5745d16017a4b7e23c699`** - normalized
(colons stripped, lowercased) and compared programmatically against the expected
`87:90:5E:14:DA:64:6D:60:BE:5E:F6:AB:10:45:59:27:11:20:2E:C8:5D:E5:74:5D:16:01:7A:4B:7E:23:C6:99`:
**exact match**. The APK is genuinely signed with the new upload key, confirmed from
the artifact itself, not assumed from the build succeeding.

**versionCode/versionName confirmed unchanged**, read directly from the built APK (not
just `app/build.gradle`) via `aapt2 dump badging`:
`versionCode='2' versionName='2.0'`, `applicationId='com.tb.tetrisbrick.game'` (the
plain release id, no `.debug` suffix, as expected for this build type). Nothing in this
step touched versionCode/versionName.

**Explicitly not done, per instruction**: the reset request was not submitted by this
session; **Google's activation of the new upload key is still pending, not claimed as
complete** - a successful *local* signed build only confirms the new key works
correctly, it says nothing about Google's server-side reset status. The APK was **not**
installed (confirmed via `adb shell pm list packages`: only the pre-existing
`com.tb.tetrisbrick.game.debug` is present on the connected device; the plain
`com.tb.tetrisbrick.game` release id is not installed anywhere). Nothing was pushed,
published, or uploaded. No password was read, requested in chat, or displayed at any
point across this whole key-recovery arc. Git history was not rewritten.

## versionCode 6 finalized + signed release AAB built and verified (2026-09-23)

Follow-up to the two sections above, once the owner confirmed the **complete** Play
Console app-bundle list (not just the production track). `applicationId`, signing
identity, gameplay, persistence, and ad wiring untouched beyond what's described below.
Nothing pushed, published, uploaded, or installed over any existing app.

**versionCode finalized**: owner confirmed versionCodes 1-5 are the *entire* set that
exists across all Play tracks (production 5/"5.0", internal testing 1/"1.0") - 5 is
genuinely the highest anywhere, not just in production. `app/build.gradle`'s
`defaultConfig` now has `versionCode 6` / `versionName "6.0"` (previously 2/"2.0",
then a recorded-but-unset proposal) - this is no longer a proposal, it's set.
`applicationId` unchanged.

**Signed release AAB built**: `./gradlew :app:bundleRelease`, no
`useTestAdsForLocalRelease` override (confirmed absent from `admob.properties`
beforehand) - `BUILD SUCCESSFUL`, `:app:signReleaseBundle` completed. Output:
`/Users/AdmirSatara/tetris-brick/app/build/outputs/bundle/release/app-release.aab`
(6,021,813 bytes; SHA-256 file checksum
`999a2d28a9782cc23951493d438ac3e67acb64dfdaa56de87de450c2c6103baf`, via `shasum -a
256`).

**Unit tests + release lint**: `./gradlew :app:testDebugUnitTest :app:lintRelease` -
33/33 unit tests passing, `lintRelease` 0 errors / 52 warnings (same baseline as every
prior pass this session; nothing new, nothing to resolve).

**AAB signature verified with AAB-appropriate tooling, deliberately not `apksigner`**:
an `.aab` is a standard JAR-signed zip (`META-INF/MANIFEST.MF` + `.SF` + `.RSA`), not an
APK with the v1-v4 APK signing schemes `apksigner` targets - so this pass used
`jarsigner -verify -verbose -certs` (result: `jar verified.`; the signature block files
are literally named `META-INF/UPLOAD.SF`/`UPLOAD.RSA`, confirming the alias used) and
`keytool -printcert -jarfile` to extract the signer certificate directly. **Signer
SHA-256**: `87:90:5E:14:DA:64:6D:60:BE:5E:F6:AB:10:45:59:27:11:20:2E:C8:5D:E5:74:5D:16:01:7A:4B:7E:23:C6:99`
- compared programmatically (case/colon-insensitive) against the expected value: **exact
match**. `jarsigner` also reports the certificate is self-signed with no trusted CA
chain and no timestamp - both **expected and normal** for an Android signing
certificate, not a defect.

**Bundle contents inspected directly to confirm production configuration** (not just
inferred from build success): the packaged manifest AGP used to build this exact bundle
(`app/build/intermediates/packaged_manifests/release/.../AndroidManifest.xml`) shows
`package="com.tb.tetrisbrick.game"`, `android:versionCode="6"`,
`android:versionName="6.0"`. More directly: unzipped `base/resources.pb` straight out
of the built `.aab` and grepped it - all five production AdMob identifiers
(`ca-app-pub-6402675413704299~8130164394` and the four `.../19169...`, `.../22962...`,
`.../42677...`, `.../80154...` banner IDs) are present in the actual compiled
resources shipped inside the artifact, and **zero** occurrences of Google's test
AdMob IDs (`ca-app-pub-3940256099942544...`) anywhere in that file.

**`bundletool`**: not available when this pass started; a local install via
`brew install bundletool` was attempted but was still mid-way through compiling
unrelated dependencies from source when this update was first written - see the
follow-up section immediately below for how this was actually resolved and the
completed structural validation results.

**Explicitly not done, per instruction**: the AAB was not installed, uploaded, pushed,
or published; Google's upload-key reset activation is still pending and not claimed
complete; no credential was read, requested, printed, or logged.

## bundletool structural validation of the versionCode 6 AAB (2026-09-23, follow-up)

Completed the structural validation left outstanding above, against the **same,
unmodified** AAB - no rebuild, no artifact changes. `applicationId`, signing config,
versionCode/versionName, and ad wiring all untouched by this follow-up. Nothing
installed, uploaded, pushed, or published.

**Checked whether the previously-started install had completed, per instruction not to
start a duplicate one**: the background `brew install bundletool` process had in fact
finished (exit code 0) - but it turned out to have cascaded into a large, unrelated
`brew upgrade` of many stale dependencies (compiling `openssl@3`, `harfbuzz`, and
others from source, which is why it took so long), and **bundletool itself ended up not
actually installed** - `brew list bundletool` reported "No such keg" and `brew info
bundletool` reported "Not installed" despite the overall command exiting 0. No second
`brew install` was run.

Investigating further: Homebrew *had* already downloaded bundletool's prebuilt bottle
successfully to its cache
(`~/Library/Caches/Homebrew/downloads/...--bundletool--1.18.3.all.bottle.tar.gz`) - only
the final link/install step never ran (most likely pruned by the same cleanup pass
visible in the install log). Rather than re-trigger `brew install` (which would attempt
a fresh resolve/download/possibly more unrelated upgrades again), this already-fetched
bottle was extracted directly to a scratch directory (`/tmp/bundletool_extract/`) and
its jar invoked directly (`java -jar .../bundletool-all.jar`) - the identical artifact
Homebrew itself would have installed, just without going through Homebrew's own
linking. This is completing the already-started download, not starting a new
installation.

**`bundletool validate --bundle=app-release.aab`**: exit code 0, printed the full App
Bundle structure (base module file listing) with no validation errors. Confirms the AAB
is structurally well-formed per bundletool's own checks - not just "gradle produced it
without complaint."

**`bundletool dump manifest --bundle=app-release.aab`**: printed the manifest
bundletool itself decoded from the AAB's protobuf-format
`base/manifest/AndroidManifest.xml`:
```
package="com.tb.tetrisbrick.game" android:versionCode="6" android:versionName="6.0"
```
This is a **third, independent** confirmation of package/versionCode/versionName (after
the packaged-manifest inspection and the `aapt2`-on-APK check from the prior pass),
this time using the exact tool and command requested, reading the AAB's own
bundletool-native manifest format rather than a Gradle intermediate.

**SHA-256 recalculated and compared against the previously reported value**: re-ran
`shasum -a 256` against
`/Users/AdmirSatara/tetris-brick/app/build/outputs/bundle/release/app-release.aab` -
same file size (6,021,813 bytes), same timestamp, same checksum
(`999a2d28a9782cc23951493d438ac3e67acb64dfdaa56de87de450c2c6103baf`) - **exact match**,
confirmed programmatically. The artifact is unmodified since it was last reported.

**Result: structural validation complete, no issues found.** Combined with the prior
pass's `jarsigner`/`keytool` signature verification (exact signer SHA-256 match) and
direct `resources.pb` inspection (production AdMob IDs present, test IDs absent), this
AAB now has independent confirmation from every angle requested: build success, JAR
signature and signer identity, bundletool's own structural validation, bundletool's own
manifest parsing, and file-integrity (checksum stability). Still a **locally verified
candidate only** - Google's upload-key reset activation remains pending, and nothing
has been uploaded.

**Explicitly still pending** (not attempted this pass, not claimed as covered):
device/upgrade-install QA against this new versionCode 6 build (the last real-device
pass predates this versionCode change and used the debug package, which carries its own
independent `.debug`-suffixed versionName and is unaffected by this change); an actual
Play Console upload of this AAB (blocked on the pending upload-key reset activation,
not attempted).

## Icon/splash refinement (2026-09-22, follow-up to the rebrand below)

Two focused fixes on top of the rebrand in the section below it. Scope stayed
strictly to branding resources and the one theme reference that points at them -
`applicationId`, signing, gameplay, persistence, ads, and SDK versions untouched.
Nothing pushed/published.

**1. Icon motif replaced**: the three-separated-blocks mark became four rounded
squares in a 2x2 grid, lower row shifted right by exactly one column pitch (cyan,
violet, cyan, cyan). Geometry: 18-unit squares, 4-unit gaps, 4-unit corner radius,
bounding box `[23,34]-[85,74]` on the 108 viewport - centered exactly on the
viewport's own center point (54,54), comfortably inside the 21-87 adaptive-icon safe
zone. Same palette (`colorPrimary`/`colorAccent`) and navy background as before.
Checked at a simulated 24px launcher size before finalizing - still reads as four
distinct blocks. Updated consistently: `ic_launcher_foreground.xml`,
`ic_launcher_monochrome.xml` (silhouette), and every legacy (API 24-25) PNG at all
five densities - all regenerated from one Pillow script so the shipped rasters and the
vector paths are guaranteed to describe the same geometry (the script's `render()`
coordinates were transcribed directly into the vector `pathData`, not eyeballed
separately).

**2. Debug marker removed - first from the splash, then entirely, per direct user
feedback mid-task**: the previous pass's `values-v31/styles.xml` pointed
`android:windowSplashScreenAnimatedIcon` at `@drawable/ic_launcher_foreground` - which
is exactly the resource the debug variant overrode (adding a small white
identification dot) for the *launcher icon*. Since both the launcher and the splash
referenced the same resource name, the debug build's dot leaked into the splash too,
reading as a rendering defect. First fix: added a dedicated
`app/src/main/res/drawable/ic_splash_icon.xml` (same block geometry, reused
deliberately, no dot, no debug override, ever) and pointed
`windowSplashScreenAnimatedIcon` at it instead - this alone fully decoupled the splash
from the launcher-only marker. At that point the debug *launcher icon* still had the
dot by design (the task had explicitly allowed "a launcher-only debug marker" as long
as it couldn't leak into the splash). **The user then explicitly said they didn't want
the dot on the app icon at all** ("ne zelim da imam bijeli dot na app icon"), so it was
removed completely: deleted `app/src/debug/res/drawable/ic_launcher_foreground.xml`
(the only file that ever drew it) and its now-redundant per-density legacy PNG
overrides. `app/src/debug/res/` is now **completely empty** - the debug variant
inherits every branding resource from `main` with zero overrides, identical to
`release`. `ic_splash_icon.xml` (the splash-specific file, dot-free from the start)
was kept as-is; it was already correct and remains the mechanism that would prevent
any *future* debug marker from ever reaching the splash again, if one is ever added.

**Executed checks** (run twice - once after the splash-only fix, once after the dot
was removed entirely; figures below are the final state): `./gradlew :app:assembleDebug`
✅, `:app:lintDebug` ✅ (0 errors, 52 warnings - identical count throughout, no
icon/splash/style-category warnings), `:app:testDebugUnitTest` ✅ (33/33, unaffected).
`:app:mergeReleaseResources` inspected directly both times. Pixel-checked every
shipped launcher PNG (both variants, all densities) for white marker pixels after the
final removal: **zero white pixels anywhere**, confirming no dot survives in either
variant. Also re-confirmed zero old-green (`#3DDC84`) pixels, same as the prior pass.

**Verified on-device** (same Galaxy S23 FE, same debug package reused via
`installDebug` each time - not uninstalled, no data cleared, confirmed by `BEST 150`
still showing on the Home screen after each reinstall):
- After the splash-only fix: home screen showed the new four-square icon *with* the
  dot (as designed at that point); a cold-launch capture confirmed the live system
  splash screen had no dot.
- After the dot's full removal: re-installed, re-captured the home screen -
  `device_home_screen_icon.png` now shows the plain four-square mark with no dot at
  all, confirming the fix on the actual device the user was looking at.
- Splash was not re-captured after the second change (it never had the dot to begin
  with; `device_cold_launch_splash_no_dot.png` and
  `device_cold_launch_after_transition.png` from the splash-only fix remain valid and
  current).

**Preview/screenshot naming convention** (applies to both this pass and the prior
one, `docs/design/branding/` now uses it consistently): `render_*.png` = synthetic
Pillow-generated preview, never captured from a running app; `shipped_*.png` = an
actual production asset file copied directly from `app/src/.../res/...` for
reference; `device_*.png` = a real screenshot captured from the physical Galaxy S23
FE. `docs/design/branding/` reflects current branding only, not a history of every
pass - the debug-specific preview (`render_icon_debug_variant.png`) and shipped-debug
copy were deleted once debug and release became byte-identical.

**Not tested this pass**: live themed/monochrome-icon toggling on-device, a
stock/AOSP launcher, a second physical device, RTL (same as the prior pass,
unchanged by this one).

## Launcher icon + splash rebrand (2026-09-22)

Replaced the default Android-Studio-template green robot-cube icon (`#3DDC84`,
unchanged since project creation, duplicated identically across `debug` and `release`
variant overrides) with an original dark-navy/cyan/violet block-puzzle mark, and
configured the Android 12+ system splash screen to match. `applicationId`, signing,
gameplay, persistence, ads, and SDK versions untouched. Nothing pushed/published.

**Design**: three rounded blocks (cyan, violet, cyan) in a staggered cluster - echoes a
falling tetromino without copying one exact piece - on a flat `colorBackground`
(`#0B1220`) field, using the app's existing `colorPrimary`/`colorAccent` tokens so it's
literally the same palette as the redesigned UI, not just a similar one. Geometry sits
inside the standard 66dp adaptive-icon safe zone (21-87 of the 108 viewport) and was
checked for legibility down to a simulated 24px launcher size before being finalized -
see `docs/design/branding/`.

**What changed**:
- **Consolidated the asset structure**: previously `debug` and `release` each carried
  a full, byte-identical set of launcher assets with no `main` fallback at all (an
  unusual setup - every asset was a "variant override" of nothing). New assets live
  once in `app/src/main/res/` (`drawable/ic_launcher_background.xml`,
  `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, and a single
  `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` pair referencing them);
  `release` now has **zero** icon-related files of its own and inherits everything from
  `main` - there is no code path left by which release could retain the old icon.
  `debug` keeps exactly one override, `drawable/ic_launcher_foreground.xml` (same
  motif plus a small white corner dot), which Gradle resource-precedence
  (variant > main) applies automatically wherever `@drawable/ic_launcher_foreground` is
  referenced, including from the shared adaptive-icon XML - no duplicated XML needed
  for that either.
- **Adaptive icon**: background/foreground/**monochrome** layers (the monochrome layer
  is new - the old setup had no themed-icon support for Android 13+ at all).
- **Legacy (API 24-25) launcher icons**: minSdk is 24, two API levels below adaptive
  icon support (26+), so flat per-density PNGs (`mipmap-mdpi` through `-xxxhdpi`,
  `ic_launcher.png` + `ic_launcher_round.png`) were regenerated from the exact same
  block geometry via a Pillow script (kept in the session scratchpad, not the repo) so
  the preview and the shipped assets are guaranteed pixel-consistent. First pass used a
  literal full-bleed square background, which `lint` correctly flagged
  (`IconLauncherShape` - legacy icons need their own shape since old launchers don't
  mask them); fixed by rendering the legacy variant with a rounded-square silhouette
  instead. Root `ic_launcher-playstore.png` (512x512, not an Android resource - a
  manual Play Console upload convenience file outside `res/`) regenerated to match for
  both variants, same structure as before.
- **`android:roundIcon`**: manifest reference (`@mipmap/ic_launcher_round`) was already
  correct and untouched; now resolves to the new consolidated adaptive XML +
  regenerated legacy PNGs instead of the old green ones.
- **Splash (Android 12+, API 31+)**: pure platform theme attributes in a new
  `values-v31/styles.xml` (`android:windowSplashScreenBackground` = `colorBackground`,
  `android:windowSplashScreenAnimatedIcon` = the new foreground vector) - no
  `androidx.core:core-splashscreen` dependency, no separate splash Activity, no
  artificial delay; the OS dismisses it as soon as the first frame renders.
  `windowSplashScreenIconBackgroundColor` deliberately left unset since the cyan/violet
  blocks already contrast well directly against navy. For API 24-30 (no system splash
  API), `AppTheme`'s existing `android:windowBackground` (already `colorBackground`)
  continues to serve as the pre-first-frame starting window - already correct before
  this pass, no change needed there.
  **A real implementation snag**: the natural way to add v31-only items to an existing
  theme, `<style name="AppTheme" parent="@style/AppTheme">` in `values-v31/`, compiles
  and runs correctly (AAPT2 resolves it fine per-qualifier) but **lint rejects it**
  (`ResourceCycle: Style AppTheme should not extend itself`) - a real
  build-vs-lint-tooling mismatch, not a functional bug. Fixed properly rather than
  suppressed: split the existing theme into a distinctly-named `AppTheme.Base` (all the
  original items, unchanged) with `AppTheme` extending it with no items in `values/`,
  and `values-v31/styles.xml` overriding `AppTheme` to extend `AppTheme.Base` plus the
  two splash items. Lint-clean, no self-reference, same runtime resolution.
- Removed the now-dead `@color/ic_launcher_background` (`#FFFFFF`, confirmed
  unreferenced by anything - the old green *drawable* of the same name was the one
  actually wired into the adaptive icon, a separate resource type/namespace) and all
  now-unused `ic_launcher_foreground.png` rasters (the adaptive path references the new
  vector directly, not a per-density PNG).

**Executed checks**: `./gradlew :app:assembleDebug` ✅, `:app:lintDebug` ✅ (0 errors;
52 warnings, down from the pre-existing 55 baseline - the drop is the removed dead/
duplicate icon files; confirmed zero warnings in any icon/style/mipmap category, all
remaining warnings pre-existing and unrelated), `:app:testDebugUnitTest` ✅ (33/33,
unaffected by this pass). `:app:mergeReleaseResources` (not `assembleRelease` -
deliberately avoids the intentional release ad-config fail-fast gate, which only
triggers on `assemble`/`bundle`/`build` task names, not on a targeted resource-merge
task) run to directly inspect merged release resources: exactly one launcher-icon
resource set present, entirely sourced from `main`, zero release-specific icon files.
Also grepped every shipped launcher PNG (all densities, both variants) for the old
green (`#3DDC84`) color - none found.

**Verified on-device** (same Galaxy S23 FE, same `applicationIdSuffix ".debug"`
package, installed over the existing debug install - **not uninstalled, no data
cleared**, confirmed via `installDebug` reusing the existing package rather than a
fresh install):
- Home screen launcher icon: new navy/cyan/violet mark renders correctly under the
  device's own OEM (Samsung One UI) adaptive-icon mask, debug dot clearly visible, no
  green anywhere - screenshot `device_home_screen_icon.png`.
- Cold launch: force-stopped the app, relaunched, and screenshotted immediately -
  **caught the actual live Android system splash screen mid-transition** (not a mockup):
  navy background, the new icon, matching design - `device_cold_launch_splash_REAL.png`.
  A second screenshot ~150ms later shows the app already settled into its own Home
  screen with no residual flash or visible seam - `device_cold_launch_after_transition.png`.
  Confirms "smooth transition, no green/white flash, no duplicate splash" directly, not
  just by theme-attribute inspection.
- Font scale, saved-game state, and all gameplay/persistence behavior from the prior
  pass were untouched by this session and not re-verified here (out of scope for an
  icon-only change).

**Not tested / explicitly out of scope this pass**: themed (monochrome) icon rendering
was **not** toggled live on-device - doing so would change the appearance of every icon
on the device's home screen (a personal, actively-used phone with the owner's own
wallpaper/layout), not just this app, so it was deliberately left alone. The monochrome
layer's correctness was instead verified statically: valid vector XML, correctly
referenced from the adaptive-icon XML, passes lint, and renders as the expected
silhouette in the generated preview (`icon_preview_monochrome_themed.png`). Round-icon
mask behavior specifically (vs. the standard adaptive icon) could not be independently
distinguished on this device - Samsung One UI applies its own consistent icon shape to
all apps regardless of the `roundIcon` manifest attribute, which is expected OEM
launcher behavior, not a defect. A second physical device, a stock/AOSP launcher, and a
smaller-screen device remain untested (same constraint noted in earlier QA sessions).

**Screenshots and previews**: the files originally listed here (three-separated-block
motif, and a splash capture that - as it turned out - had the debug dot leaking into
it) were **superseded by the follow-up pass above** and no longer exist under this
filenames in `docs/design/branding/`; that folder holds only the current branding, not
a history of every pass. See the "Icon/splash refinement" section above for the current
file list and the naming convention (`render_*`/`shipped_*`/`device_*`) now used
consistently.

## Visual refinement pass (2026-09-22) — Home/gameplay/pause/game-over/scores polish

Follow-up to the modernization + hardening passes below. Scope: a focused visual/UX
pass over the already-shipped-quality app, driven by reviewing the prior session's
on-device screenshots. Preserved: gameplay rules, persistence schema, consent flow, ad
config, dependencies, signing, `applicationId`, `minSdk`. No push/publish performed.

**Weaknesses identified from the prior session's screenshots** (Home, paused game,
Scores, Settings): large unstructured dead space on Home and Scores; settled and
falling pieces painted in one flat, seamless color (no per-block separation, no
distinction between the live piece and the board under it); pause communicated only by
a small icon swap, nothing on the board itself; game over was a Toast + silent 4s
auto-exit with no final-score screen, no record detection, no replay option; Scores had
no rank labels; outline-button text (`colorPrimaryDark` on a dark surface) was
low-contrast almost to the point of being unreadable (visible once installed - not
obvious from static code review).

**What changed:**
- **Gameplay rendering** (`PlayingAreaView`): settled blocks now get a thin per-cell
  seam stroke (`colorBackground`) so adjacent blocks read as distinct squares instead of
  one mass. The currently-falling piece is separately repainted via its own
  `Figure.getPath()` (already used elsewhere for the NEXT preview - not new plumbing) in
  a lightened tint of the user's chosen color plus a light stroke, so it's visually
  distinct from what's already settled. Both are pure additive Canvas draws on top of
  the existing grid/fill logic - gravity, collision, and scoring code untouched.
- **Paused state**: a dimmed scrim + "PAUSED" text is now drawn directly on the board
  (Canvas, no new views) whenever a game is genuinely in progress but not advancing.
  Fixed a real bug found only by testing on-device: `handleTimerState()` flipped the
  pause flag but never called `invalidate()`, so the overlay code was correct but never
  actually ran until the next unrelated redraw - pausing looked unchanged until this fix.
- **Game over**: replaced the Toast + blind 4-second auto-`finish()` with an in-place,
  non-cancelable dialog (`dialog_game_over.xml`) showing the final score, a "NEW RECORD!"
  badge, and Replay/Home actions. `OnTimerStateChangedListener` gained an
  `onGameOver(int finalScore, boolean isNewRecord)` callback; `MainActivity` owns the
  dialog (it already owns all the chrome the Replay path needs to re-enable).
  **Record detection is against the pre-update best**: `SharedPreferencesManager` gained
  `getBestScore()`, read in `onTopLineHasTrue()` *before* `putNewScore()` overwrites it.
  A new `scoreRecorded` flag stops the pre-existing `cleanup()` call (which still runs on
  every other exit path: back button, home, process death) from recording the same
  game's score a second time - traced through by hand and covered by a unit test that
  starts from a fresh-install, all-zero scores state, which is exactly the scenario where
  a double `putNewScore()` call for the same score would silently duplicate it into two
  rank slots. Replay reuses the same `PlayingAreaView`/`MainActivity` instance (no
  `finish()`+relaunch), so `cleanup()` now also resets `isGameOver`/`isTimerRunning` back
  to their fresh-game defaults - previously those fields were only ever reset by the
  whole Activity being re-created, which no longer happens on Replay.
- **Home screen**: best-score line, a small static Canvas-drawn decorative block cluster
  (`BlockClusterView`, one-shot draw, no animation loop), and a **Continue** action that
  only appears when `GameStateStore.hasSavedGame()` is true - it becomes the primary
  (filled) action, New Game becomes secondary. New Game now confirms
  (`AlertDialog`, "Start a new game? This will discard your saved game in progress.")
  before discarding an existing save. Scores/Settings moved into a secondary row to
  reduce vertical dead space. Continue launches `MainActivity` with a new
  `EXTRA_RESUME_GAME` intent extra; `MainActivity.onCreate()` now resumes on
  `savedInstanceState != null` **or** that extra, since restoring was previously only
  reachable via Android's own process-recreation path, never via a deliberate
  from-Home launch.
  **Real bug found only by on-device testing**: the Continue button initially never
  appeared after backing out of a game, even though the save clearly existed
  (confirmed via `adb shell run-as ... cat shared_prefs/...xml`). Root cause: Android
  runs the *incoming* activity's `onResume()` before the *outgoing* activity's
  `onStop()` on a back-press, so `StartActivity`'s `hasSavedGame()` check (in
  `onResume()`) was racing `MainActivity.saveGameState()` (in `onStop()`) and always
  losing. Fixed by moving `saveGameState()` from `onStop()` to `onPause()`, which is
  guaranteed to complete before the incoming activity resumes. This is a lifecycle
  subtlety no amount of static reading would have caught - only surfaced by actually
  backing out of a real game on-device and finding Continue missing.
- **Scores screen**: added 1ST/2ND/3RD rank labels and dividers, widened the card
  (was a narrow wrap-content box floating in a lot of dead space), em-dash placeholder
  (`—`) for a rank with no score yet instead of a bare "0" (also used for Home's best
  score).
- **Contrast fix**: `main_buttons_style` and the new dialog button style both used
  `colorPrimaryDark` (#0E7490) text on a dark surface - low enough contrast to be hard
  to read, confirmed visually on-device (not obvious from the hex values alone). Changed
  to `colorPrimary` (#22D3EE) for both. Settings' speed-picker pills use the same
  `colorPrimaryDark` in their static style XML but are unaffected: `SettingsActivity`
  already overrides every item's text color to `colorPrimary` at runtime on init, so
  the dim static value there was already dead code, not a live bug.
- **Controls**: `button_circle.xml` (game controls + Settings' color-picker circles)
  changed from a flat `StateListDrawable` (color swap on press) to a real
  `RippleDrawable` for native tactile press feedback.
- Settings screen was judged already close to the target (clear section headers, good
  runtime-applied selected-state contrast) and was deliberately left alone.

**Regression tests added** (unit, Robolectric): `SharedPreferencesManagerTest` gained a
`getBestScore()` coverage case. `PlayingAreaViewTest` gained: exactly-once score
recording when `onTopLineHasTrue()` is followed by `cleanup()` (the double-record
scenario above); new-record detection against the pre-update best in both directions
(record and no-record); `cleanup()` resetting game-over/timer state correctly for an
in-place Replay. 33/33 unit tests passing (up from 28), 0 new lint warnings (55
pre-existing warnings, all in files this pass didn't touch - `AndroidManifest.xml`
orientation/`DiscouragedApi` items, unrelated).

**Executed checks**: `./gradlew :app:assembleDebug` ✅, `:app:testDebugUnitTest` ✅
(33/33), `:app:lintDebug` ✅ (0 errors, 55 pre-existing warnings, none in changed
files). Installed the debug build (`applicationIdSuffix ".debug"`, same safe pattern as
the prior session - confirmed the real `com.tb.tetrisbrick.game` package was never
touched) on the same Galaxy S23 FE used previously, and drove it interactively via
`adb shell input tap` with `uiautomator dump`-sourced exact coordinates (screenshot
pixel coordinates need scaling and were unreliable - confirmed the hard way again this
session before switching back to dump-sourced bounds). Verified on-device, not just in
code: Home hierarchy/best-score/Continue/decorative graphic; New Game discard-confirm
dialog; active-piece highlight and block-seam rendering (pixel-sampled to confirm the
active piece is genuinely a lighter tint of the settled color, not just visually
similar); paused scrim + text (including the invalidate() bug found and fixed live);
full game-over → Replay round trip (board reset, controls/icon re-enabled, timer
resumed - all in the same activity instance); Continue correctly restoring the exact
saved board into a paused state; Scores screen rank labels/dividers/em-dash; Settings
unaffected. Also checked system font scale at 1.3x on Home and Settings (both reflow
cleanly, no clipping/overlap) - **font scale was restored to 1.0 on the device
afterward**. Not tested this pass: a second physical device/smaller viewport (only the
one Galaxy S23 FE was available, same constraint as the prior session), TalkBack/
screen-reader traversal of the new dialogs, RTL layout.

**Screenshots** (`/tmp/tetris_screenshots_v2/` on the machine this session ran on - not
committed to the repo): `01_home.png`, `02_gameplay.png`, `03_settled_blocks.png`
(block-seam + active-piece highlight), `04_paused.png`, `05_game_over.png`,
`06_after_replay.png`, `07_home_continue.png`, `08_after_continue.png` (restored,
paused), `09_discard_confirm.png`, `10_scores.png`, `11_settings.png`,
`12_large_font_home.png`, `13_large_font_settings.png`.

**Remaining/known issues**: the game-over dialog is a plain `AlertDialog` tied to the
Activity instance - it will not survive a configuration change or process death while
showing (pre-existing limitation class; the Toast it replaced had the same property).
Untested: TalkBack traversal order through the new dialogs, a genuinely small-screen
device, RTL. The release-versionCode blocker above is unrelated to this pass and still
open.

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

- **Signing - owner no longer has the keystore** (updated 2026-09-23): the historical
  `keystore.properties`' `keyAlias=upload` still strongly suggests this was the **Play
  App Signing upload key**, not Google's own held app-signing key. But whether Play App
  Signing is actually enrolled for this app, and whether the self-service "Request
  upload key reset" route therefore applies, are **both pending confirmation** - not
  assumed either way. Until confirmed: **do not generate a replacement app-signing
  identity, do not reuse another application's key, and do not change the signing
  configuration.** This is a harder blocker than "rotate a leaked credential" - there
  may be no working release keystore at all right now. See `RELEASE_CHECKLIST.md` §1.
  - The git-history credential exposure (`keystore.properties`' password readable in
    commit history before `1e46d15`) is a separate, secondary concern once signing
    itself is sorted out - not acted on automatically per standing instruction.
- **Production AdMob App ID**: still the one missing piece of ad configuration - **must
  never be derived from an ad-unit ID or an AdMob console URL**; it's a distinct
  Google-issued identifier. The four production banner ad-unit IDs *are* now configured
  (2026-09-23, see the section above) - only `admobAppId` in `admob.properties` remains
  unset. Release builds fail fast specifically on this, with a message naming it by
  name. See `RELEASE_CHECKLIST.md` §2.
- **Final versionCode**: still `2`/`"2.0"` in this repo, unchanged - not finalized.
  **Owner-confirmed (2026-09-23): the latest PRODUCTION release is versionCode 5 /
  versionName "5.0".** That is the confirmed *production* figure, but not necessarily
  the highest versionCode across **all** Play tracks (an internal/closed testing track
  can run ahead of production) - that broader figure is **still not confirmed**.
  **Proposed next version: 6 / "6.0"** - recorded as a proposal only, not set anywhere
  in the repo, not to be finalized until the all-tracks figure is confirmed directly
  against Play Console. See `RELEASE_CHECKLIST.md` §3.
- **Debug package identity**: debug builds now install as `com.tb.tetrisbrick.game.debug` (commit `b6e05dc`), not `com.tb.tetrisbrick.game` - a deliberate, standard Android practice so a debug build can never conflict with (or require uninstalling) a real install. Release builds are unaffected.
- **Rewarded ads**: removed rather than fixed (see phase 3 above) — reintroduce only with a real, designed, tested opt-in reward if wanted.
- **`io.github.ShawnLin013:number-picker`**: unmaintained since 2021 (confirmed via the toolchain research pass), still in use for the squares-per-row picker in Settings. Not replaced — no drop-in replacement evaluated, out of scope for this pass, but worth flagging as an ongoing dependency risk.

## Next steps for a future session

- Real-device QA is now done for the core flows (see "Device QA session" above) - the remaining gaps are: small-screen device, larger font scale, an actual on-device upgrade install (old APK → new APK), and a clean game-over screenshot (attempted, interrupted by a stray ad-click during rapid automated testing - not a bug, just didn't get a clean capture).
- **Confirm the highest versionCode across all Play tracks** (not just production,
  already confirmed at 5) - proposed next version is 6/"6.0", pending that check.
- **Supply the production AdMob App ID** (the four banner ad-unit IDs are already
  configured) and confirm Play App Signing status / the upload-key reset route, then do
  a full release-build dry run (the build refuses without both - see
  `gradle.taskGraph.whenReady` in `app/build.gradle`).
- Once Play App Signing status is confirmed, act on the git-history signing-credential
  exposure accordingly (self-service upload-key reset if applicable, or Google Play
  support's process otherwise) - see `RELEASE_CHECKLIST.md` §1.
- If desired: replace the unmaintained number-picker dependency, and design a real opt-in rewarded-ad benefit rather than leaving rewarded ads removed.
