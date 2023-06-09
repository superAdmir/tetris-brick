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
        switch (id) {
            case R.id.vLFigureColor:
                manageColorPicking(R.color.lFigure, id);
                break;
            case R.id.vSquareFigureColor:
                manageColorPicking(R.color.squareFigure, id);
                break;
            case R.id.vLongFigureColor:
                manageColorPicking(R.color.longFigure, id);
                break;
            case R.id.vZFigureColor:
                manageColorPicking(R.color.zFigure, id);
                break;
            case R.id.vTFigureColor:
                manageColorPicking(R.color.tFigure, id);
                break;
            case R.id.vJFigureColor:
                manageColorPicking(R.color.jFigure, id);
                break;
            case R.id.sEnableHints:
                boolean isEnabled = sharedPreferencesManager.isHintsEnabled();
                sharedPreferencesManager.setHintsEnabled(!isEnabled);
                break;
            case R.id.tvVeryFast:
                manageSpeedPicking(VERY_FAST.getFigureSpeedInMillis(), id);
                break;
            case R.id.tvFast:
                manageSpeedPicking(FAST.getFigureSpeedInMillis(), id);
                break;
            case R.id.tvDefault:
                manageSpeedPicking(DEFAULT.getFigureSpeedInMillis(), id);
                break;
            case R.id.tvSlow:
                manageSpeedPicking(SLOW.getFigureSpeedInMillis(), id);
                break;
            case R.id.tvVerySlow:
                manageSpeedPicking(VERY_SLOW.getFigureSpeedInMillis(), id);
                break;
            default:
                break;
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
