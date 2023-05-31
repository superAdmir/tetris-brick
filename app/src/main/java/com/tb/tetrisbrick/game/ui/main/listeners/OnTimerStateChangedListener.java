package com.tb.tetrisbrick.game.ui.main.listeners;

public interface OnTimerStateChangedListener {

    void isTimerRunning(boolean isRunning);

    void disableAllControls();
}
