package com.tb.tetrisbrick.game.utils;

import android.app.Activity;

import com.tb.tetrisbrick.game.BuildConfig;

// Local-only capture aid for producing clean store-artwork screenshots: when active, an
// Activity skips its ad request entirely (no consent flow, no ad load, no network call)
// and hides the AdView so screenshots aren't cluttered with a test-ad creative.
//
// Gated on BuildConfig.DEBUG in addition to the intent extra, so this can never affect
// a release build even if the extra were somehow present - release always ignores it
// and always shows ads normally. Triggered explicitly per-launch via
// `adb shell am start ... --ez com.tb.tetrisbrick.game.EXTRA_SCREENSHOT_MODE true`,
// never on by default.
public final class ScreenshotMode {

    public static final String EXTRA = "com.tb.tetrisbrick.game.EXTRA_SCREENSHOT_MODE";

    private ScreenshotMode() {
    }

    public static boolean isActive(Activity activity) {
        return BuildConfig.DEBUG && activity.getIntent().getBooleanExtra(EXTRA, false);
    }
}
