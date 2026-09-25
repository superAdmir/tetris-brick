package com.tb.tetrisbrick.game.data;

import com.tb.tetrisbrick.game.enums.FigureType;

public class SavedGame {

    public final int score;
    public final int squaresInRowCount;
    public final boolean[][] net;
    public final FigureType currentFigureType;
    public final int currentFigureGridX;
    public final int currentFigureGridY;
    public final FigureType nextFigureType;

    public SavedGame(int score, int squaresInRowCount, boolean[][] net,
                      FigureType currentFigureType, int currentFigureGridX, int currentFigureGridY,
                      FigureType nextFigureType) {
        this.score = score;
        this.squaresInRowCount = squaresInRowCount;
        this.net = net;
        this.currentFigureType = currentFigureType;
        this.currentFigureGridX = currentFigureGridX;
        this.currentFigureGridY = currentFigureGridY;
        this.nextFigureType = nextFigureType;
    }
}
