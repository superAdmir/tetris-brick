package com.tb.tetrisbrick.game.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Path;
import android.net.Uri;

import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.enums.FigureSpeed;

import java.util.List;

import static com.tb.tetrisbrick.game.Values.DEV_NAME;
import static com.tb.tetrisbrick.game.Values.EXTRA_ROWS;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_J;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_L;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_LONG;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_SQUARE;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_T;
import static com.tb.tetrisbrick.game.enums.FigureSpeed.DEFAULT;
import static com.tb.tetrisbrick.game.enums.FigureSpeed.FAST;
import static com.tb.tetrisbrick.game.enums.FigureSpeed.SLOW;
import static com.tb.tetrisbrick.game.enums.FigureSpeed.VERY_FAST;
import static com.tb.tetrisbrick.game.enums.FigureSpeed.VERY_SLOW;

public class Utils {

    public static Path createSmallSquareFigure(int i, int j, int squareWidth, int scale) {
        Path path = new Path();
        int delta = j * squareWidth - (EXTRA_ROWS - 2) * squareWidth - scale;
        path.moveTo(i * squareWidth, delta);
        path.lineTo(i * squareWidth, delta - squareWidth);
        path.lineTo(i * squareWidth + squareWidth, delta - squareWidth);
        path.lineTo(i * squareWidth + squareWidth, delta);
        path.close();
        return path;
    }

    // Stable-key -> current-build resource ID resolution. Never persist the resource
    // ID itself (see Values.FIGURE_COLOR_KEY for why); only ever persist the key.
    public static int resolveColorResId(String colorKey) {
        if (FIGURE_COLOR_L.equals(colorKey)) {
            return R.color.lFigure;
        } else if (FIGURE_COLOR_SQUARE.equals(colorKey)) {
            return R.color.squareFigure;
        } else if (FIGURE_COLOR_LONG.equals(colorKey)) {
            return R.color.longFigure;
        } else if (FIGURE_COLOR_T.equals(colorKey)) {
            return R.color.tFigure;
        } else if (FIGURE_COLOR_J.equals(colorKey)) {
            return R.color.jFigure;
        }
        return R.color.zFigure;
    }

    public static int getViewIdByColorKey(String colorKey) {
        if (FIGURE_COLOR_L.equals(colorKey)) {
            return R.id.vLFigureColor;
        } else if (FIGURE_COLOR_SQUARE.equals(colorKey)) {
            return R.id.vSquareFigureColor;
        } else if (FIGURE_COLOR_LONG.equals(colorKey)) {
            return R.id.vLongFigureColor;
        } else if (FIGURE_COLOR_T.equals(colorKey)) {
            return R.id.vTFigureColor;
        } else if (FIGURE_COLOR_J.equals(colorKey)) {
            return R.id.vJFigureColor;
        }
        return R.id.vZFigureColor;
    }

    public static FigureSpeed getFiguresSpeedByMillis(long speedMillis) {
        FigureSpeed speed;
        if (speedMillis == VERY_FAST.getFigureSpeedInMillis()) {
            speed = VERY_FAST;
        } else if (speedMillis == FAST.getFigureSpeedInMillis()) {
            speed = FAST;
        } else if (speedMillis == DEFAULT.getFigureSpeedInMillis()) {
            speed = DEFAULT;
        } else if (speedMillis == SLOW.getFigureSpeedInMillis()) {
            speed = SLOW;
        } else {
            speed = VERY_SLOW;
        }
        return speed;
    }

    public static Intent openMarket(Activity activity) {
        Uri uri = Uri.parse("market://details?id=" + activity.getPackageName());
        return new Intent(Intent.ACTION_VIEW, uri);
    }

    public static Intent showMoreApps() {
        Uri uri = Uri.parse("market://search?q=pub:" + DEV_NAME);
        return new Intent(Intent.ACTION_VIEW, uri);
    }
}
