package com.tb.tetrisbrick.game.utils;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// targetSdk 35+ enforces edge-to-edge: system bars draw over the app's content by
// default, so every screen needs to pad itself back away from them instead of the
// old "the system reserves the space for me" behavior.
public class EdgeToEdgeUtils {

    private EdgeToEdgeUtils() {
    }

    public static void applySystemBarInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
    }
}
