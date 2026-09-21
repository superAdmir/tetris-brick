package com.tb.tetrisbrick.game;


import com.tb.tetrisbrick.game.enums.FigureSpeed;

public class Values {

    private static final String NAMESPACE = "com.tb.tetrisbrick.game";

    public static final int EXTRA_ROWS = 4;
    public static final int FIGURE_STOPPED_SCORE = 10;
    public static final int COUNT_DOWN_INTERVAL = 750;
    public static final long DELAY_IN_MILLIS = 1500;
    public static final long DEBOUNCE_DELAY_IN_MILLIS = 450;
    public static final long GAME_OVER_DELAY_IN_MILLIS = 4000;
    public static final float LINE_WIDTH = 1f;

    public static final String PLAY_MARKET_URL = "https://play.google.com/store/apps/details?id=com.tb.tetrisbrick.game";
    public static final String SHARE_INTENT_TYPE = "text/plain";
    public static final String DEV_NAME = "superadmir";

    /*PREFERENCES*/
    public static final String PREFERENCES_KEY = NAMESPACE + "PREFERENCE_KEY";
    public static final String FIRST_VALUE_KEY = "first_value";
    public static final String SECOND_VALUE_KEY = "second_value";
    public static final String THIRD_VALUE_KEY = "third_value";
    public static final int DEFAULT_VALUE = 0;

    // v1 stored a raw Android resource ID (R.color.xxx) as the figure color preference,
    // which is only guaranteed stable within a single build - AAPT2 can renumber
    // resource IDs across builds (e.g. whenever colors.xml gains/loses/reorders
    // entries, which this modernization pass did). An old int surviving an app update
    // could silently resolve to the wrong resource, or a nonexistent one, and crash.
    // v2 stores a stable string key instead, resolved to the current build's actual
    // resource ID on every read via Utils.resolveColorResId(). See
    // SharedPreferencesManager.getFiguresColorKey() for the one-time migration, which
    // deliberately does not try to reinterpret the old raw int - it can't be trusted to
    // still mean the same color, so it falls back to the default color key instead.
    public static final String LEGACY_FIGURE_COLOR_KEY = "default_color";
    public static final String FIGURE_COLOR_KEY = "default_color_v2";
    public static final String FIGURE_COLOR_L = "l_figure";
    public static final String FIGURE_COLOR_SQUARE = "square_figure";
    public static final String FIGURE_COLOR_LONG = "long_figure";
    public static final String FIGURE_COLOR_Z = "z_figure";
    public static final String FIGURE_COLOR_T = "t_figure";
    public static final String FIGURE_COLOR_J = "j_figure";
    public static final String DEFAULT_FIGURE_COLOR_KEY = FIGURE_COLOR_Z;

    public static final String FIGURE_SPEED_KEY = "default_speed";
    public static final long DEFAULT_SPEED = FigureSpeed.DEFAULT.getFigureSpeedInMillis();
    public static final String ENABLE_HINTS_KEY = "enable_hints_key";
    public static final boolean ENABLED_HINTS = true;
    public static final String SQUARES_COUNT_IN_ROW_KEY = "default_squares_in_row";
    public static final int SQUARES_COUNT_IN_ROW = 10;

    /*NOTIFICATIONS*/
    public static final int NOTIFICATION_ID = 123;
    public static final String CHANNEL_NAME = "SCORES";
    public static final String SCORE_CHANNEL = NAMESPACE + ".scores";
}
