# Tetris Brick Game

A lightweight Tetris-style puzzle game for Android, built with a custom Canvas 2D
gameplay engine (no game framework/engine dependency).

How to play:

- Tap or swipe left/right to move the falling brick.
- Fill a row completely to clear it and score.
- Use the on-screen controls to rotate the brick or drop it down fast.
- Pause and resume at any time.
- View your top 3 scores.
- Customize the playing experience in Settings.

Features:

- Play offline - no account, no network dependency for gameplay.
- No in-app purchases.
- Contains banner ads (Google AdMob). See "Advertising" below.
- "Next brick" preview.
- Pick your preferred brick color.
- Choose brick fall speed.
- Optional vertical alignment hints.
- Configurable squares-per-row (board width).
- Notification when you set a new best score.
- An in-progress game is saved and restored if the app is closed or the OS
  reclaims it mid-game - you come back to a paused game, not a lost one.
- Dark, cyan/violet arcade theme with edge-to-edge system-bar support.

## Advertising

This app shows a banner ad (Google AdMob) on every screen. There is no rewarded-ad
flow. Ad requests are gated behind a GDPR/UMP consent check, and Settings has a
"Privacy options" entry for revisiting your consent choice where required. Ads never
block gameplay - a failed, declined, or offline ad load is silently skipped.

## Building

Requires JDK 17 and the Android SDK (compileSdk 37). Debug builds work out of the box:

```
./gradlew :app:assembleDebug
```

Release builds need your own signing key - see `keystore.properties` (gitignored,
never committed) and `CLAUDE_PROGRESS.md` / `RELEASE_CHECKLIST.md` for details.

## Development notes

See `CLAUDE_PROGRESS.md` for the modernization history, architectural decisions, and
current state of the codebase, and `RELEASE_CHECKLIST.md` before shipping a Play Store
update.

<p align="center">
  <img src="https://user-images.githubusercontent.com/23102335/71322503-cd09f200-24d0-11ea-841d-061ce9b6cd8a.png" width="300">
  <img src="https://user-images.githubusercontent.com/23102335/71322575-a9937700-24d1-11ea-8567-779e1b09f5e6.png" width="300">
</p>

*Screenshots above are from the pre-2026 light-themed version and don't yet reflect
the current dark theme.*
