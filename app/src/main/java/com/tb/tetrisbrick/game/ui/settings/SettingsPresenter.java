package com.tb.tetrisbrick.game.ui.settings;

import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.data.SharedPreferencesManager;
import com.tb.tetrisbrick.game.enums.FigureSpeed;
import com.tb.tetrisbrick.game.utils.Utils;

import static com.tb.tetrisbrick.game.Values.DEFAULT_FIGURE_COLOR_KEY;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_J;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_L;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_LONG;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_SQUARE;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_T;
import static com.tb.tetrisbrick.game.Values.FIGURE_COLOR_Z;
import static com.tb.tetrisbrick.game.enums.FigureSpeed.DEFAULT;
import static com.tb.tetrisbrick.game.enums.FigureSpeed.FAST;
import static com.tb.tetrisbrick.game.enums.FigureSpeed.SLOW;
import static com.tb.tetrisbrick.game.enums.FigureSpeed.VERY_FAST;
import static com.tb.tetrisbrick.game.enums.FigureSpeed.VERY_SLOW;

class SettingsPresenter {

    private final SharedPreferencesManager sharedPreferencesManager;
    private final SettingsView settingsView;

    SettingsPresenter(SettingsView settingsView, SharedPreferencesManager sharedPreferencesManager) {
        this.settingsView = settingsView;
        this.sharedPreferencesManager = sharedPreferencesManager;
    }

    void setValues() {
        FigureSpeed figureSpeed = Utils.getFiguresSpeedByMillis(sharedPreferencesManager.getFiguresSpeed());
        if (settingsView != null) {
            String chosenColorKey = sharedPreferencesManager.getFiguresColorKey();
            settingsView.markChosenColor(DEFAULT_FIGURE_COLOR_KEY, Utils.getViewIdByColorKey(chosenColorKey));
            settingsView.setSquaresCountInRow(sharedPreferencesManager.getSquaresCountInRow());
            settingsView.setSpeedTitle(figureSpeed.getSpeedItemId());
            settingsView.setVerticalHintsChecked(sharedPreferencesManager.isHintsEnabled());
        }
    }

    void setSquareCountInRow(int newValue) {
        sharedPreferencesManager.setSquaresCountInRow(newValue);
    }

    void getEvent(int id) {
        if (id == R.id.vLFigureColor) {
            manageColorPicking(FIGURE_COLOR_L, id);
        } else if (id == R.id.vSquareFigureColor) {
            manageColorPicking(FIGURE_COLOR_SQUARE, id);
        } else if (id == R.id.vLongFigureColor) {
            manageColorPicking(FIGURE_COLOR_LONG, id);
        } else if (id == R.id.vZFigureColor) {
            manageColorPicking(FIGURE_COLOR_Z, id);
        } else if (id == R.id.vTFigureColor) {
            manageColorPicking(FIGURE_COLOR_T, id);
        } else if (id == R.id.vJFigureColor) {
            manageColorPicking(FIGURE_COLOR_J, id);
        } else if (id == R.id.sEnableHints) {
            boolean isEnabled = sharedPreferencesManager.isHintsEnabled();
            sharedPreferencesManager.setHintsEnabled(!isEnabled);
        } else if (id == R.id.tvVeryFast) {
            manageSpeedPicking(VERY_FAST.getFigureSpeedInMillis(), id);
        } else if (id == R.id.tvFast) {
            manageSpeedPicking(FAST.getFigureSpeedInMillis(), id);
        } else if (id == R.id.tvDefault) {
            manageSpeedPicking(DEFAULT.getFigureSpeedInMillis(), id);
        } else if (id == R.id.tvSlow) {
            manageSpeedPicking(SLOW.getFigureSpeedInMillis(), id);
        } else if (id == R.id.tvVerySlow) {
            manageSpeedPicking(VERY_SLOW.getFigureSpeedInMillis(), id);
        }
    }

    private void manageSpeedPicking(long newSpeed, int newItemId) {
        sharedPreferencesManager.setFiguresSpeed(newSpeed);
        settingsView.setSpeedTitle(newItemId);
    }

    private void manageColorPicking(String newColorKey, int newItemId) {
        String oldColorKey = sharedPreferencesManager.getFiguresColorKey();
        sharedPreferencesManager.setFiguresColor(newColorKey);
        settingsView.markChosenColor(oldColorKey, newItemId);
    }
}
