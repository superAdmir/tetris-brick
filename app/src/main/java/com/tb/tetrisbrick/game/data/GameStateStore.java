package com.tb.tetrisbrick.game.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.tb.tetrisbrick.game.enums.FigureType;

import static android.content.Context.MODE_PRIVATE;
import static com.tb.tetrisbrick.game.Values.PREFERENCES_KEY;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_CURRENT_GRID_X_KEY;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_CURRENT_GRID_Y_KEY;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_CURRENT_TYPE_KEY;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_NET_CELLS_KEY;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_NET_COLS_KEY;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_NET_ROWS_KEY;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_NEXT_TYPE_KEY;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_SCHEMA_VERSION;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_SCORE_KEY;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_SQUARES_IN_ROW_KEY;
import static com.tb.tetrisbrick.game.Values.SAVED_GAME_VERSION_KEY;

// Persists an in-progress game across process death (distinct from
// SharedPreferencesManager's settings/high-scores, which are never invalidated).
// Every read is validated against the current schema version and board dimensions;
// anything unexpected is treated as "no usable save" rather than risking a crash or a
// corrupted board - callers should always be ready to fall back to a fresh game.
public class GameStateStore {

    private final SharedPreferences preferences;

    public GameStateStore(Context context) {
        this.preferences = context.getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE);
    }

    public void save(SavedGame savedGame) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SAVED_GAME_VERSION_KEY, SAVED_GAME_SCHEMA_VERSION);
        editor.putInt(SAVED_GAME_SCORE_KEY, savedGame.score);
        editor.putInt(SAVED_GAME_SQUARES_IN_ROW_KEY, savedGame.squaresInRowCount);
        editor.putInt(SAVED_GAME_NET_ROWS_KEY, savedGame.net.length);
        editor.putInt(SAVED_GAME_NET_COLS_KEY, savedGame.net.length == 0 ? 0 : savedGame.net[0].length);
        editor.putString(SAVED_GAME_NET_CELLS_KEY, flatten(savedGame.net));
        editor.putString(SAVED_GAME_CURRENT_TYPE_KEY, savedGame.currentFigureType.name());
        editor.putInt(SAVED_GAME_CURRENT_GRID_X_KEY, savedGame.currentFigureGridX);
        editor.putInt(SAVED_GAME_CURRENT_GRID_Y_KEY, savedGame.currentFigureGridY);
        editor.putString(SAVED_GAME_NEXT_TYPE_KEY, savedGame.nextFigureType.name());
        editor.apply();
    }

    public void clear() {
        preferences.edit().remove(SAVED_GAME_VERSION_KEY).apply();
    }

    public boolean hasSavedGame() {
        return preferences.getInt(SAVED_GAME_VERSION_KEY, -1) == SAVED_GAME_SCHEMA_VERSION;
    }

    // Returns null for anything malformed or incompatible: wrong/missing schema
    // version, dimensions that don't match the requested board size (e.g. the user
    // changed the "squares in a row" setting while backgrounded), a cell string of the
    // wrong length, or a figure type name that no longer exists. Never throws.
    public SavedGame load(int expectedSquaresInRowCount) {
        if (!hasSavedGame()) return null;
        try {
            int squaresInRowCount = preferences.getInt(SAVED_GAME_SQUARES_IN_ROW_KEY, -1);
            if (squaresInRowCount != expectedSquaresInRowCount) return null;

            int rows = preferences.getInt(SAVED_GAME_NET_ROWS_KEY, -1);
            int cols = preferences.getInt(SAVED_GAME_NET_COLS_KEY, -1);
            if (rows <= 0 || cols <= 0) return null;

            String cells = preferences.getString(SAVED_GAME_NET_CELLS_KEY, null);
            if (cells == null || cells.length() != rows * cols) return null;
            boolean[][] net = unflatten(cells, rows, cols);

            FigureType currentType = FigureType.valueOf(
                    preferences.getString(SAVED_GAME_CURRENT_TYPE_KEY, ""));
            FigureType nextType = FigureType.valueOf(
                    preferences.getString(SAVED_GAME_NEXT_TYPE_KEY, ""));

            int score = preferences.getInt(SAVED_GAME_SCORE_KEY, 0);
            int gridX = preferences.getInt(SAVED_GAME_CURRENT_GRID_X_KEY, -1);
            int gridY = preferences.getInt(SAVED_GAME_CURRENT_GRID_Y_KEY, -1);
            if (gridX < 0 || gridY < 0) return null;

            return new SavedGame(score, squaresInRowCount, net, currentType, gridX, gridY, nextType);
        } catch (IllegalArgumentException | NullPointerException malformedSave) {
            return null;
        }
    }

    private String flatten(boolean[][] net) {
        StringBuilder builder = new StringBuilder(net.length * (net.length == 0 ? 0 : net[0].length));
        for (boolean[] row : net) {
            for (boolean cell : row) {
                builder.append(cell ? '1' : '0');
            }
        }
        return builder.toString();
    }

    private boolean[][] unflatten(String cells, int rows, int cols) {
        boolean[][] net = new boolean[rows][cols];
        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                net[i][j] = cells.charAt(index++) == '1';
            }
        }
        return net;
    }
}
