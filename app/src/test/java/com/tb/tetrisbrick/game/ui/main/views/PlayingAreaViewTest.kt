package com.tb.tetrisbrick.game.ui.main.views

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.tb.tetrisbrick.game.Values
import com.tb.tetrisbrick.game.data.GameStateStore
import com.tb.tetrisbrick.game.data.SavedGame
import com.tb.tetrisbrick.game.enums.FigureType
import com.tb.tetrisbrick.game.figures.factory.FigureCreator
import com.tb.tetrisbrick.game.ui.main.NetManager
import com.tb.tetrisbrick.game.ui.main.listeners.OnTimerStateChangedListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// Exercises PlayingAreaView.restoreGameIfAvailable()/saveGameState()/startFreshGame() end
// to end - the actual restore call site, not just the GameStateStore layer underneath it.
// See NetManagerTest for why SDK is pinned to 34.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayingAreaViewTest {

    private lateinit var context: Context
    private lateinit var view: PlayingAreaView
    private lateinit var scoreView: ScoreView
    private lateinit var previewAreaView: PreviewAreaView
    private lateinit var listener: RecordingTimerStateListener
    private lateinit var gameStateStore: GameStateStore

    private val squaresInRowCount = 10
    private val squareWidth = 50
    private val measuredWidth = squareWidth * squaresInRowCount // 500
    private val measuredHeight = 1100 // -> verticalSquareCount = 22, net rows = 26

    private class RecordingTimerStateListener : OnTimerStateChangedListener {
        var lastIsRunning: Boolean? = null
        var disableAllControlsCalled = false
        override fun isTimerRunning(isRunning: Boolean) {
            lastIsRunning = isRunning
        }
        override fun disableAllControls() {
            disableAllControlsCalled = true
        }
    }

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(Values.PREFERENCES_KEY, Context.MODE_PRIVATE).edit().clear().commit()
        gameStateStore = GameStateStore(context)

        view = PlayingAreaView(context)
        scoreView = ScoreView(context).apply { setStartValue() }
        previewAreaView = PreviewAreaView(context)
        listener = RecordingTimerStateListener()
        view.setDependencies(scoreView, previewAreaView, listener)
        attachAndMeasure(view)
    }

    // restoreGameIfAvailable() uses View.post(), which - per real Android behavior, which
    // Robolectric follows - only routes through the main Looper once the view is attached
    // to a window; an unattached view silently queues the Runnable forever. So every view
    // under test needs a real (simulated) window, not just measure().
    private fun attachAndMeasure(target: PlayingAreaView) {
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().resume().visible().get()
        activity.setContentView(target, ViewGroup.LayoutParams(measuredWidth, measuredHeight))
        shadowOf(Looper.getMainLooper()).idle()
        target.measure(
            View.MeasureSpec.makeMeasureSpec(measuredWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(measuredHeight, View.MeasureSpec.EXACTLY)
        )
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Suppress("UNCHECKED_CAST")
    private fun netManagerOf(view: PlayingAreaView): NetManager {
        val field = PlayingAreaView::class.java.getDeclaredField("netManager")
        field.isAccessible = true
        return field.get(view) as NetManager
    }

    private fun currentFigureOf(view: PlayingAreaView): Any? {
        val field = PlayingAreaView::class.java.getDeclaredField("currentFigure")
        field.isAccessible = true
        return field.get(view)
    }

    private fun cellCount(netManager: NetManager) = netManager.stoppedFiguresPaths.size

    // Column 4-5, rows 5-6 true - exactly SquareFigure's mask (see figures/SquareFigure.java)
    // placed at grid (4,5), plus two unrelated settled cells elsewhere on the board.
    private fun boardWithSquareFigureAt(gridX: Int, gridY: Int): Array<BooleanArray> {
        val net = Array(26) { BooleanArray(squaresInRowCount) }
        net[25][0] = true
        net[25][1] = true
        net[gridY][gridX] = true
        net[gridY][gridX + 1] = true
        net[gridY + 1][gridX] = true
        net[gridY + 1][gridX + 1] = true
        return net
    }

    @Test
    fun restore_doesNotDuplicateTheMovingPiecesCellsInTheBoard() {
        val net = boardWithSquareFigureAt(4, 5)
        val cellsBeforeRestore = net.sumOf { row -> row.count { it } } // 2 settled + 4 figure = 6
        gameStateStore.save(SavedGame(150, squaresInRowCount, net,
            FigureType.SQUARE_FIGURE, 4, 5, FigureType.LONG_FIGURE))

        view.restoreGameIfAvailable()
        idleMainLooper()

        val netManager = netManagerOf(view)
        assertEquals(cellsBeforeRestore, cellCount(netManager))

        // The restored figure must be a real, functional live piece, not just a visual
        // artifact - moving it must not change the total cell count either (it should
        // vacate the cells it leaves and occupy the same number it moves into).
        view.moveLeft()
        assertEquals(cellsBeforeRestore, cellCount(netManager))
    }

    @Test
    fun restore_setsCorrectScoreNextPieceAndContinuesTheSequence() {
        val net = boardWithSquareFigureAt(4, 5)
        gameStateStore.save(SavedGame(275, squaresInRowCount, net,
            FigureType.SQUARE_FIGURE, 4, 5, FigureType.LONG_FIGURE))

        view.restoreGameIfAvailable()
        idleMainLooper()

        assertEquals(275, scoreView.score)
        assertNotNull("the next-figure preview must be populated", previewFigureOf(previewAreaView))

        val figureCreatorField = PlayingAreaView::class.java.getDeclaredField("figureCreator")
        figureCreatorField.isAccessible = true
        val figureCreator = figureCreatorField.get(view) as FigureCreator
        assertEquals(FigureType.SQUARE_FIGURE, figureCreator.currentFigureType)
        assertEquals(FigureType.LONG_FIGURE, figureCreator.nextFigureType)

        // The restored sequence must continue normally: advancing it moves the restored
        // "next" into "current" and generates a fresh "next", same as a non-restored game.
        figureCreator.createNextFigure()
        assertEquals(FigureType.LONG_FIGURE, figureCreator.currentFigureType)
    }

    private fun previewFigureOf(previewAreaView: PreviewAreaView): Any? {
        val field = PreviewAreaView::class.java.getDeclaredField("figure")
        field.isAccessible = true
        return field.get(previewAreaView)
    }

    @Test
    fun restore_endsInASafePausedState() {
        val net = boardWithSquareFigureAt(4, 5)
        gameStateStore.save(SavedGame(10, squaresInRowCount, net,
            FigureType.SQUARE_FIGURE, 4, 5, FigureType.LONG_FIGURE))

        view.restoreGameIfAvailable()
        idleMainLooper()

        assertFalse("a restored game must not resume falling immediately", view.isTimerRunning())
        assertEquals(false, listener.lastIsRunning)
    }

    @Test
    fun restore_fallsBackToFreshGameForABoardWidthMismatch() {
        // Saved with a different squares-per-row than the board being restored into -
        // e.g. the user changed that setting while the app was backgrounded.
        val net = boardWithSquareFigureAt(4, 5)
        gameStateStore.save(SavedGame(999, squaresInRowCount + 2, net,
            FigureType.SQUARE_FIGURE, 4, 5, FigureType.LONG_FIGURE))

        view.restoreGameIfAvailable()
        idleMainLooper()

        // startFreshGame() clears the stale/incompatible save and resets score/state
        // synchronously; the actual new figure spawn is on a further delayed post which
        // we don't need to wait for to prove the fallback path was taken correctly.
        assertFalse(gameStateStore.hasSavedGame())
        assertEquals(0, scoreView.score)
        assertNull("no figure should be left over from the rejected save", currentFigureOf(view))
    }

    @Test
    fun restore_fallsBackToFreshGameWhenNoSaveExists() {
        assertFalse(gameStateStore.hasSavedGame())

        view.restoreGameIfAvailable()
        idleMainLooper()

        assertEquals(0, scoreView.score)
        assertNull(currentFigureOf(view))
    }

    @Test
    fun startFreshGame_discardsAnyPreviouslySavedGame() {
        val net = boardWithSquareFigureAt(4, 5)
        gameStateStore.save(SavedGame(50, squaresInRowCount, net,
            FigureType.SQUARE_FIGURE, 4, 5, FigureType.LONG_FIGURE))
        assertTrue(gameStateStore.hasSavedGame())

        view.startFreshGame()

        assertFalse(gameStateStore.hasSavedGame())
    }

    @Test
    fun gameOver_clearsAnyPreviouslySavedGame() {
        val net = boardWithSquareFigureAt(4, 5)
        gameStateStore.save(SavedGame(50, squaresInRowCount, net,
            FigureType.SQUARE_FIGURE, 4, 5, FigureType.LONG_FIGURE))
        assertTrue(gameStateStore.hasSavedGame())

        view.onTopLineHasTrue()

        assertFalse(gameStateStore.hasSavedGame())
        assertTrue(listener.disableAllControlsCalled)
    }

    @Test
    fun saveThenRestore_roundTripsThroughTheRealCallSites() {
        // Restore a first game, then drive saveGameState() (as MainActivity.onStop()
        // does) and restore again into a second, independent view - end-to-end through
        // the real production call sites rather than constructing a SavedGame by hand.
        val net = boardWithSquareFigureAt(2, 3)
        gameStateStore.save(SavedGame(60, squaresInRowCount, net,
            FigureType.SQUARE_FIGURE, 2, 3, FigureType.T_FIGURE))
        view.restoreGameIfAvailable()
        idleMainLooper()
        scoreView.setScore(123)

        view.saveGameState()

        val secondView = PlayingAreaView(context)
        val secondScoreView = ScoreView(context).apply { setStartValue() }
        val secondPreview = PreviewAreaView(context)
        val secondListener = RecordingTimerStateListener()
        secondView.setDependencies(secondScoreView, secondPreview, secondListener)
        attachAndMeasure(secondView)

        secondView.restoreGameIfAvailable()
        idleMainLooper()

        assertEquals(123, secondScoreView.score)
        assertEquals(cellCount(netManagerOf(view)), cellCount(netManagerOf(secondView)))
    }
}
