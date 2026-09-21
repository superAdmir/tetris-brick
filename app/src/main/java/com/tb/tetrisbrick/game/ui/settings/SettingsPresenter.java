package com.tb.tetrisbrick.game.ui.settings;

import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.data.SharedPreferencesManager;
import com.tb.tetrisbrick.game.enums.FigureSpeed;
import com.tb.tetrisbrick.game.utils.Utils;

import static com.tb.tetrisbrick.game.Values.DEFAULT_COLOR;
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
            settingsView.markChosenColor(DEFAULT_COLOR, Utils.getViewIdByColor(sharedPreferencesManager.getFiguresColor()));
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
            manageColorPicking(R.color.lFigure, id);
        } else if (id == R.id.vSquareFigureColor) {
            manageColorPicking(R.color.squareFigure, id);
        } else if (id == R.id.vLongFigureColor) {
            manageColorPicking(R.color.longFigure, id);
        } else if (id == R.id.vZFigureColor) {
            manageColorPicking(R.color.zFigure, id);
        } else if (id == R.id.vTFigureColor) {
            manageColorPicking(R.color.tFigure, id);
        } else if (id == R.id.vJFigureColor) {
            manageColorPicking(R.color.jFigure, id);
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

    private void manageColorPicking(int newColor, int newItemId) {
        int oldColor = sharedPreferencesManager.getFiguresColor();
        sharedPreferencesManager.setFiguresColor(newColor);
        settingsView.markChosenColor(oldColor, newItemId);
    }
}
