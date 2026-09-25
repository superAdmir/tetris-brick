package com.tb.tetrisbrick.game.ui.main.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;

import com.tb.tetrisbrick.game.BuildConfig;
import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.Values;
import com.tb.tetrisbrick.game.data.GameStateStore;
import com.tb.tetrisbrick.game.data.SavedGame;
import com.tb.tetrisbrick.game.data.SharedPreferencesManager;
import com.tb.tetrisbrick.game.enums.FigureState;
import com.tb.tetrisbrick.game.enums.FigureType;
import com.tb.tetrisbrick.game.figures.Figure;
import com.tb.tetrisbrick.game.figures.factory.FigureCreator;
import com.tb.tetrisbrick.game.figures.factory.FigureFactory;
import com.tb.tetrisbrick.game.utils.Utils;
import com.tb.tetrisbrick.game.ui.main.NetManager;
import com.tb.tetrisbrick.game.ui.main.listeners.OnNetChangedListener;
import com.tb.tetrisbrick.game.ui.main.listeners.OnPlayingAreaTouch;
import com.tb.tetrisbrick.game.ui.main.listeners.OnViewTouchListener;
import com.tb.tetrisbrick.game.ui.main.listeners.OnTimerStateChangedListener;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.tb.tetrisbrick.game.Values.COUNT_DOWN_INTERVAL;
import static com.tb.tetrisbrick.game.Values.LINE_WIDTH;

public class PlayingAreaView extends View implements OnNetChangedListener, OnPlayingAreaTouch {

    // Purely visual constants for the additive rendering below (block seams, active-piece
    // highlight, paused scrim) - none of these affect gravity/collision/scoring.
    private static final float BLOCK_SEAM_STROKE_WIDTH = 2f;
    private static final float ACTIVE_FIGURE_STROKE_WIDTH = 3f;
    private static final float ACTIVE_FIGURE_LIGHTEN_FACTOR = 0.35f;
    private static final int PAUSED_OVERLAY_ALPHA = 190;

    private int squareWidth, verticalSquareCount;
    private int screenHeight, screenWidth;
    private int scale;
    private int squaresInRowCount;
    private boolean isTimerRunning, isGameOver;

    // Set once a just-ended game's score has been recorded (see onTopLineHasTrue()), so
    // cleanup() - which also runs for every other exit path - never records it a second
    // time. Reset back to false whenever cleanup() runs, ready for the next game.
    private boolean scoreRecorded;

    private Figure currentFigure;
    private FigureType currentFigureType;

    private NetManager netManager;
    private FigureCreator figureCreator;
    private ScoreView scoreView;
    private PreviewAreaView previewAreaView;
    private SharedPreferencesManager sharedPreferencesManager;
    private GameStateStore gameStateStore;
    private OnViewTouchListener onViewTouchListener;

    private Paint paint;

    private CountDownTimer timer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingCreateFigure;

    private Context context;
    private OnTimerStateChangedListener onTimerStateChangedListener;

    public PlayingAreaView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PlayingAreaView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayingAreaView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        figureCreator = new FigureCreator();
        sharedPreferencesManager = new SharedPreferencesManager(getContext());
        gameStateStore = new GameStateStore(getContext());
        onViewTouchListener = new OnViewTouchListener(context, this);
        setOnTouchListener(onViewTouchListener);
        this.squaresInRowCount = sharedPreferencesManager.getSquaresCountInRow();
        this.context = context;
        this.isTimerRunning = true;
        this.isGameOver = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(LINE_WIDTH);
        drawHorizontalLines(canvas);
        drawVerticalLines(canvas);
        drawSettledBlocks(canvas);
        drawActiveFigure(canvas);
        if (isPausedMidGame()) drawPausedOverlay(canvas);
    }

    // Settled cells (which, per NetManager, also include the currently-falling piece's
    // own cells - they're baked into the same net array) get a flat fill plus a thin
    // seam stroke per cell so adjacent blocks read as distinct squares instead of one
    // solid mass. drawActiveFigure() then overpaints just the falling piece's own
    // outline on top so it's visually distinguishable from what's already settled.
    private void drawSettledBlocks(Canvas canvas) {
        if (netManager == null || netManager.getStoppedFiguresPaths() == null) return;
        int fillColor = getResources().getColor(Utils.resolveColorResId(sharedPreferencesManager.getFiguresColorKey()));
        int seamColor = getResources().getColor(R.color.colorBackground);
        for (Path squarePath : netManager.getStoppedFiguresPaths()) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fillColor);
            canvas.drawPath(squarePath, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(BLOCK_SEAM_STROKE_WIDTH);
            paint.setColor(seamColor);
            canvas.drawPath(squarePath, paint);
        }
    }

    // currentFigure.getPath() is the same per-figure outline PreviewAreaView already
    // draws for the NEXT preview - reused here, just painted in a lighter tint with a
    // light stroke so the piece still in play is readable at a glance against whatever
    // has already settled underneath it.
    private void drawActiveFigure(Canvas canvas) {
        if (currentFigure == null || currentFigure.getState() != FigureState.MOVING) return;
        Path path = currentFigure.getPath();
        int baseColor = getResources().getColor(Utils.resolveColorResId(sharedPreferencesManager.getFiguresColorKey()));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(lighten(baseColor, ACTIVE_FIGURE_LIGHTEN_FACTOR));
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(ACTIVE_FIGURE_STROKE_WIDTH);
        paint.setColor(getResources().getColor(R.color.colorOnBackground));
        canvas.drawPath(path, paint);
    }

    private static int lighten(int color, float factor) {
        int r = Color.red(color) + (int) ((255 - Color.red(color)) * factor);
        int g = Color.green(color) + (int) ((255 - Color.green(color)) * factor);
        int b = Color.blue(color) + (int) ((255 - Color.blue(color)) * factor);
        return Color.rgb(r, g, b);
    }

    // True only while a game is genuinely in progress but not advancing: excludes both
    // "no figure has spawned yet" (currentFigure == null, e.g. during the initial spawn
    // delay) and game over (isGameOver), so the overlay never flashes at the wrong time.
    private boolean isPausedMidGame() {
        return !isGameOver && !isTimerRunning && currentFigure != null;
    }

    private void drawPausedOverlay(Canvas canvas) {
        int background = getResources().getColor(R.color.colorBackground);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((PAUSED_OVERLAY_ALPHA << 24) | (background & 0x00FFFFFF));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

        paint.setColor(getResources().getColor(R.color.colorOnBackground));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(squareWidth * 0.9f);
        canvas.drawText(getResources().getString(R.string.paused_text),
                screenWidth / 2f, screenHeight / 2f, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        squareWidth = MeasureSpec.getSize(widthMeasureSpec) / squaresInRowCount;
        verticalSquareCount = MeasureSpec.getSize(heightMeasureSpec) / squareWidth;
        scale = squareWidth - (MeasureSpec.getSize(heightMeasureSpec) % squareWidth);
        screenHeight = MeasureSpec.getSize(heightMeasureSpec);
        screenWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (onViewTouchListener != null) onViewTouchListener.setScreenWidth(screenWidth);
    }

    public void cleanup() {
        // Game-over already recorded this game's score itself (see onTopLineHasTrue()),
        // so skip here to avoid recording the same score twice - see scoreRecorded.
        if (!scoreRecorded) {
            sharedPreferencesManager.putNewScore(scoreView.getScore());
        }
        scoreRecorded = false;
        cancelTimer();
        if (pendingCreateFigure != null) handler.removeCallbacks(pendingCreateFigure);
        scoreView.setStartValue();
        netManager = null;
        currentFigure = null;
        // Restores the same defaults init() sets, since cleanup() now also runs ahead of
        // an in-place replay (same PlayingAreaView instance) rather than only ever being
        // followed by the whole Activity being destroyed.
        isGameOver = false;
        isTimerRunning = true;
    }

    public void setDependencies(ScoreView scoreView, PreviewAreaView previewAreaView, OnTimerStateChangedListener onTimerStateChangedListener) {
        this.scoreView = scoreView;
        this.previewAreaView = previewAreaView;
        this.onTimerStateChangedListener = onTimerStateChangedListener;
    }

    private void drawHorizontalLines(Canvas canvas) {
        for (int i = 1; i <= verticalSquareCount; i++) {
            canvas.drawLine(0, screenHeight - squareWidth * i, screenWidth, screenHeight - squareWidth * i, paint);
        }
    }

    private void drawVerticalLines(Canvas canvas) {
        for (int i = 1; i <= squaresInRowCount; i++) {
            if (sharedPreferencesManager.isHintsEnabled()) drawVerticalHints(i);
            canvas.drawLine(i * squareWidth, 0, i * squareWidth, screenHeight, paint);
        }
    }

    private void drawVerticalHints(int line) {
        if (currentFigure != null) {
            if (line == currentFigure.getCurrentX() || line == currentFigure.getCurrentX() + currentFigure.getWidthInSquare()) {
                paint.setColor(getResources().getColor(R.color.colorPrimaryTransparent));
                paint.setStrokeWidth(LINE_WIDTH * 4);
            } else {
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(LINE_WIDTH);
            }
        }
    }

    public boolean isTimerRunning() {
        return isTimerRunning && !isGameOver;
    }

    public void handleTimerState() {
        if (isTimerRunning) {
            cancelTimer();
        } else {
            startTimer();
        }
        isTimerRunning = !isTimerRunning;
        if (!isGameOver) onTimerStateChangedListener.isTimerRunning(isTimerRunning);
        // Unlike resuming (where the timer's own onFinish() -> invalidate() cycle takes
        // over), pausing stops that cycle dead, so nothing would otherwise trigger the
        // redraw that shows/hides the paused overlay.
        invalidate();
    }

    public void startTimer() {
        if (timer != null) {
            timer.start();
        }
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    // Drives gravity on its own schedule, independent of rendering: each successful
    // moveDown() reschedules the next tick itself, and nothing outside this method
    // (moving/rotating/redrawing) resets it.
    private void advanceOrStopFalling() {
        if (currentFigure == null || currentFigure.getState() != FigureState.MOVING) return;
        if (BuildConfig.DEBUG) netManager.printNet();
        if (!netManager.isNetFreeToMoveDown()) {
            netManager.changeFigureState();
            return;
        }
        if (!isTimerRunning) return;
        cancelTimer();
        timer = new CountDownTimer(sharedPreferencesManager.getFiguresSpeed(), COUNT_DOWN_INTERVAL) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (currentFigure != null && currentFigure.getState() == FigureState.MOVING) {
                    currentFigure.moveDown();
                    netManager.moveDownInNet();
                    invalidate();
                    advanceOrStopFalling();
                }
            }
        };
        startTimer();
    }

    public void fastMoveDown() {
        if (currentFigure != null && isTimerRunning) {
            cancelTimer();
            while (currentFigure.getState() == FigureState.MOVING) {
                if (!netManager.isNetFreeToMoveDown()) {
                    netManager.changeFigureState();
                    break;
                }
                currentFigure.moveDown();
                netManager.moveDownInNet();
            }
            invalidate();
        }
    }

    public void moveRightFast() {
        if (currentFigure != null && isTimerRunning) {
            cancelTimer();
            while (netManager.isNetFreeToMoveRight()) {
                netManager.resetMaskBeforeMoveWithFalse();
                currentFigure.moveRight();
                netManager.moveRightInNet();
            }
            invalidate();
            advanceOrStopFalling();
        }
    }

    public void moveLeftFast() {
        if (currentFigure != null && isTimerRunning) {
            cancelTimer();
            while (netManager.isNetFreeToMoveLeft()) {
                netManager.resetMaskBeforeMoveWithFalse();
                currentFigure.moveLeft();
                netManager.moveLeftInNet();
            }
            invalidate();
            advanceOrStopFalling();
        }
    }

    public void rotate() {
        if (currentFigure != null && currentFigure.getState() == FigureState.MOVING && currentFigure.getRotatedFigure() != null && isTimerRunning) {
            FigureType rotatedType = currentFigure.getRotatedFigure();
            Figure figure = FigureFactory.getFigure(rotatedType, squareWidth, scale, context, currentFigure.pointOnScreen);
            if (figure != null) {
                figure.initFigureMask();
                if (netManager.canRotate(figure)) {
                    currentFigure = figure;
                    currentFigureType = rotatedType;
                    netManager.checkBottomLine();
                    netManager.initRotatedFigure(figure);
                }
            }
        }
    }

    private void createFigure() {
        FigureType type = figureCreator.getCurrentFigureType();
        Figure figure = FigureFactory.getFigure(type, squareWidth, scale, squaresInRowCount, context);
        if (figure != null) {
            currentFigure = figure;
            currentFigureType = type;
            if (netManager == null) {
                netManager = new NetManager(this, verticalSquareCount, squaresInRowCount, squareWidth, scale);
            }
            if (netManager.isVerticalLineComplete()) {
                netManager.checkBottomLine();
                netManager.initFigure(currentFigure);
                if (BuildConfig.DEBUG) netManager.printNet();
                invalidate();
                advanceOrStopFalling();
            }
        }
    }

    public void moveLeft() {
        if (netManager != null && netManager.isNetFreeToMoveLeft() && isTimerRunning) {
            netManager.resetMaskBeforeMoveWithFalse();
            currentFigure.moveLeft();
            netManager.moveLeftInNet();
            if (BuildConfig.DEBUG) netManager.printNet();
            invalidate();
        }
    }

    public void moveRight() {
        if (netManager != null && netManager.isNetFreeToMoveRight() && isTimerRunning) {
            netManager.resetMaskBeforeMoveWithFalse();
            currentFigure.moveRight();
            netManager.moveRightInNet();
            if (BuildConfig.DEBUG) netManager.printNet();
            invalidate();
        }
    }

    public void createFigureWithDelay() {
        pendingCreateFigure = () -> {
            previewAreaView.drawNextFigure(FigureFactory.getFigure(figureCreator.getNextFigureType(), (squareWidth * squaresInRowCount) / Values.SQUARES_COUNT_IN_ROW, context));
            createFigure();
        };
        handler.postDelayed(pendingCreateFigure, Values.DELAY_IN_MILLIS);
    }

    // The normal "New Game" entry point: discards any previously saved game (the user
    // is deliberately starting over, not resuming) and spawns the first figure as usual.
    public void startFreshGame() {
        cleanup();
        gameStateStore.clear();
        createFigureWithDelay();
    }

    // Persists enough to resume later: the board (which already has the falling
    // figure's cells baked in - see NetManager.getNetSnapshot()), that figure's type and
    // grid position, the next figure preview, and the score. Does nothing if there's no
    // game in progress to save (nothing spawned yet, or it already ended).
    public void saveGameState() {
        if (netManager == null || currentFigure == null || currentFigureType == null || isGameOver || scoreView == null) {
            return;
        }
        SavedGame savedGame = new SavedGame(scoreView.getScore(), squaresInRowCount, netManager.getNetSnapshot(),
                currentFigureType, currentFigure.getCurrentX(), currentFigure.getCurrentY(),
                figureCreator.getNextFigureType());
        gameStateStore.save(savedGame);
    }

    // Called instead of startFreshGame() when Android is recreating this screen after
    // process death (i.e. onCreate() received a non-null savedInstanceState) rather than
    // a fresh Intent launch. Waits for layout via post() - squareWidth/scale/netManager's
    // dimensions aren't known until onMeasure() has run. Falls back to a fresh game for
    // any missing, malformed, or dimension-mismatched save (see GameStateStore.load()),
    // and always restores to a paused state rather than resuming the fall immediately.
    public void restoreGameIfAvailable() {
        post(() -> {
            if (netManager == null) {
                netManager = new NetManager(this, verticalSquareCount, squaresInRowCount, squareWidth, scale);
            }
            SavedGame savedGame = gameStateStore.load(squaresInRowCount);
            Figure restoredFigure = savedGame == null ? null : reconstructFigure(savedGame);
            if (savedGame == null || restoredFigure == null
                    || savedGame.net.length != netManager.getNetRowCount()
                    || savedGame.net[0].length != netManager.getNetColumnCount()) {
                startFreshGame();
                return;
            }

            netManager.restoreNet(savedGame.net);
            currentFigure = restoredFigure;
            currentFigureType = savedGame.currentFigureType;
            netManager.initFigure(currentFigure);
            figureCreator.restoreState(savedGame.currentFigureType, savedGame.nextFigureType);
            scoreView.setScore(savedGame.score);
            previewAreaView.drawNextFigure(FigureFactory.getFigure(savedGame.nextFigureType,
                    (squareWidth * squaresInRowCount) / Values.SQUARES_COUNT_IN_ROW, context));

            isGameOver = false;
            isTimerRunning = false;
            invalidate();
            if (onTimerStateChangedListener != null) onTimerStateChangedListener.isTimerRunning(false);
        });
    }

    private Figure reconstructFigure(SavedGame savedGame) {
        Point pointOnScreen = new Point(savedGame.currentFigureGridX * squareWidth, savedGame.currentFigureGridY * squareWidth);
        Figure figure = FigureFactory.getFigureAtGridPosition(savedGame.currentFigureType, squareWidth, scale, context, pointOnScreen);
        if (figure != null) figure.initFigureMask();
        return figure;
    }

    @Override
    public void onFigureStoppedMove() {
        if (netManager.isVerticalLineComplete()) {
            scoreView.sumScoreWhenFigureStopped();
            previewAreaView.drawNextFigure(FigureFactory.getFigure(figureCreator.createNextFigure(), (squareWidth * squaresInRowCount) / Values.SQUARES_COUNT_IN_ROW, context));
            createFigure();
        }
    }

    @Override
    public void onBottomLineIsTrue() {
        scoreView.sumScoreWhenBottomLineIsTrue(squaresInRowCount);
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
    }

    @Override
    public void onTopLineHasTrue() {
        isGameOver = true;
        gameStateStore.clear();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        cancelTimer();
        onTimerStateChangedListener.disableAllControls();
        int finalScore = scoreView.getScore();
        // Must be read before putNewScore() below overwrites it - see getBestScore().
        int previousBest = sharedPreferencesManager.getBestScore();
        sharedPreferencesManager.putNewScore(finalScore);
        scoreRecorded = true;
        boolean isNewRecord = finalScore > previousBest;
        onTimerStateChangedListener.onGameOver(finalScore, isNewRecord);
    }

    @Override
    public void onRightMove() {
        moveRight();
    }

    @Override
    public void onLeftMove() {
        moveLeft();
    }

    @Override
    public void onLongLeftClick() {
        moveLeftFast();
    }

    @Override
    public void onLongRightClick() {
        moveRightFast();
    }
}
