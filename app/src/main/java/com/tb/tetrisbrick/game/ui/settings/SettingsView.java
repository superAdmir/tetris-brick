package com.tb.tetrisbrick.game.ui.settings;

interface SettingsView {

    void markChosenColor(String oldColorKey, int newItemId);

    void setSpeedTitle(int newItemId);

    void setVerticalHintsChecked(boolean hintsEnabled);

    void setSquaresCountInRow(int squaresCountInRow);
}
