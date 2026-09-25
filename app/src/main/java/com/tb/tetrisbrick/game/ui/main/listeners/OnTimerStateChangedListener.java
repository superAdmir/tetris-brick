package com.tb.tetrisbrick.game.ui.main.listeners;

public interface OnTimerStateChangedListener {

    void isTimerRunning(boolean isRunning);

    void disableAllControls();

    // finalScore is what the just-ended game actually scored; isNewRecord reflects a
    // comparison against the best score as it stood BEFORE this game's score was
    // recorded, not the (possibly just-overwritten) value afterward.
    void onGameOver(int finalScore, boolean isNewRecord);
}
