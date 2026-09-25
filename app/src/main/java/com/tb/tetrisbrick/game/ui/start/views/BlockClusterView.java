package com.tb.tetrisbrick.game.ui.start.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.tb.tetrisbrick.game.R;

import androidx.annotation.Nullable;

// Static, one-shot Canvas decoration for the Home screen: a small tetromino-like
// cluster of squares in the app's own figure-color palette. Draws once per
// onSizeChanged/onDraw pass (no animation loop, no per-frame allocation) - purely
// decorative, never touched by gameplay logic.
public class BlockClusterView extends View {

    private final Paint paint = new Paint();
    private final RectF square = new RectF();
    private int[] colorResIds;

    // {column, row} offsets, forming a loose S/L-shaped cluster reminiscent of falling
    // pieces without literally depicting one exact tetromino.
    private static final int[][] LAYOUT = {
            {0, 1}, {1, 1}, {1, 0}, {2, 0}, {3, 0}
    };

    public BlockClusterView(Context context) {
        super(context);
        init();
    }

    public BlockClusterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        colorResIds = new int[]{
                R.color.longFigure, R.color.tFigure, R.color.lFigure,
                R.color.zFigure, R.color.squareFigure
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int columns = 4;
        int rows = 2;
        float cell = Math.min((float) getWidth() / columns, (float) getHeight() / rows);
        float gap = cell * 0.12f;
        float totalWidth = cell * columns;
        float offsetX = (getWidth() - totalWidth) / 2f;

        for (int i = 0; i < LAYOUT.length; i++) {
            float left = offsetX + LAYOUT[i][0] * cell + gap;
            float top = LAYOUT[i][1] * cell + gap;
            square.set(left, top, left + cell - gap * 2, top + cell - gap * 2);
            paint.setColor(getResources().getColor(colorResIds[i % colorResIds.length]));
            canvas.drawRoundRect(square, gap, gap, paint);
        }
    }
}
