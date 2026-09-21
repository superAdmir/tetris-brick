package com.tb.tetrisbrick.game.ui.main.views;

import android.app.Activity;
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
import android.view.View;
import android.widget.Toast;

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
import static com.tb.tetrisbrick.game.Values.GAME_OVER_DELAY_IN_MILLIS;
import static com.tb.tetrisbrick.game.Values.LINE_WIDTH;

public class PlayingAreaView extends View implements OnNetChangedListener, OnPlayingAreaTouch {

    private int squareWidth, verticalSquareCount;
    private int screenHeight, screenWidth;
    private int scale;
    private int squaresInRowCount;
    private boolean isTimerRunning, isGameOver;

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
    private Runnable pendingGameOverFinish;

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
        if (netManager != null && netManager.getStoppedFiguresPaths() != null) {
            for (Path squarePath : netManager.getStoppedFiguresPaths()) {
                paint.setColor(getResources().getColor(Utils.resolveColorResId(sharedPreferencesManager.getFiguresColorKey())));
                canvas.drawPath(squarePath, paint);
            }
        }
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
        sharedPreferencesManager.putNewScore(scoreView.getScore());
        cancelTimer();
        if (pendingCreateFigure != null) handler.removeCallbacks(pendingCreateFigure);
        if (pendingGameOverFinish != null) handler.removeCallbacks(pendingGameOverFinish);
        scoreView.setStartValue();
        netManager = null;
        currentFigure = null;
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
    }

    @Override
    public void onTopLineHasTrue() {
        isGameOver = true;
        gameStateStore.clear();
        cancelTimer();
        onTimerStateChangedListener.disableAllControls();
        Toast.makeText(context, context.getString(R.string.game_over_text), Toast.LENGTH_LONG).show();
        pendingGameOverFinish = () -> ((Activity) context).finish();
        handler.postDelayed(pendingGameOverFinish, GAME_OVER_DELAY_IN_MILLIS);
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
