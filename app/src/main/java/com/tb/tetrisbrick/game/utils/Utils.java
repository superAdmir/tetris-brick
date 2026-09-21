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

    public static int getViewIdByColor(int color) {
        if (color == R.color.lFigure) {
            return R.id.vLFigureColor;
        } else if (color == R.color.squareFigure) {
            return R.id.vSquareFigureColor;
        } else if (color == R.color.longFigure) {
            return R.id.vLongFigureColor;
        } else if (color == R.color.zFigure) {
            return R.id.vZFigureColor;
        } else if (color == R.color.tFigure) {
            return R.id.vTFigureColor;
        } else if (color == R.color.jFigure) {
            return R.id.vJFigureColor;
        }
        return 0;
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
