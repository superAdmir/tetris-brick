package com.tb.tetrisbrick.game.ui.main

import android.content.Context
import android.graphics.Point
import com.tb.tetrisbrick.game.Values
import com.tb.tetrisbrick.game.figures.Figure
import com.tb.tetrisbrick.game.figures.SquareFigure
import com.tb.tetrisbrick.game.figures.figure_j.JFigure
import com.tb.tetrisbrick.game.ui.main.listeners.OnNetChangedListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Figure/SquareFigure construct android.graphics.Point internally (for pointInNet), whose
// real assignment behavior only exists under Robolectric - plain JVM unit tests leave every
// Point's fields at their Java default regardless of constructor arguments. SDK pinned to 34:
// the project's targetSdk 36 shadow requires Java 21, but the toolchain builds with Java 17.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetManagerTest {

    private lateinit var netManager: NetManager
    private lateinit var jFigure: Figure
    private lateinit var context: Context
    private var squareWidth = 50
    private var scale = 10
    private var widthOfSquareSide = 50
    private var horizontalSquareCount = 10
    private var verticalSquareCount = 22

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val listener = Mockito.mock(OnNetChangedListener::class.java)
        context = Mockito.mock(Context::class.java)
        jFigure = JFigure(squareWidth, scale, horizontalSquareCount, context)
        netManager = NetManager(listener, verticalSquareCount, horizontalSquareCount,
                widthOfSquareSide, scale)
    }

    @Test
    fun initNetTest() {
        assertEquals(NetManager.combo, 0)
        assertEquals(netManager.netColumnCount, horizontalSquareCount)
        assertEquals(netManager.netRowCount, verticalSquareCount + Values.EXTRA_ROWS)
    }

    @Test
    fun initJFigureTest() {
        netManager.initFigure(jFigure)
        assertEquals(jFigure.scale, scale + 2 * squareWidth)
    }

    private fun squareAt(column: Int, row: Int) =
        SquareFigure(squareWidth, scale, context, Point(column * squareWidth, row * squareWidth))
            .apply { initFigureMask() }

    @Test
    fun canRotate_rejectsOverlapWithSettledBlocks() {
        // A block already settled at columns 5-6, rows 5-6 ...
        netManager.initFigure(squareAt(5, 5))
        // ... and a different figure now active elsewhere.
        netManager.initFigure(squareAt(0, 0))

        // A rotation candidate whose destination cells land exactly on the settled block
        // must be rejected - this is the collision check that was previously missing.
        assertFalse(netManager.canRotate(squareAt(5, 5)))
    }

    @Test
    fun canRotate_acceptsFreeDestination() {
        netManager.initFigure(squareAt(5, 5))
        netManager.initFigure(squareAt(0, 0))

        assertTrue(netManager.canRotate(squareAt(2, 2)))
    }

    @Test
    fun canRotate_rejectsOutOfBoundsDestination() {
        netManager.initFigure(squareAt(0, 0))

        // A 2-wide figure at column (horizontalSquareCount - 1) runs off the right edge.
        assertFalse(netManager.canRotate(squareAt(horizontalSquareCount - 1, 0)))
    }

    @Suppress("UNCHECKED_CAST")
    private fun netField(): Array<BooleanArray> {
        val field = NetManager::class.java.getDeclaredField("net")
        field.isAccessible = true
        return field.get(netManager) as Array<BooleanArray>
    }

    private fun setNetField(board: Array<BooleanArray>) {
        val field = NetManager::class.java.getDeclaredField("net")
        field.isAccessible = true
        field.set(netManager, board)
    }

    @Test
    fun checkBottomLine_clearsTwoAdjacentFullRows() {
        val rows = netManager.netRowCount
        val cols = horizontalSquareCount
        val board = Array(rows) { BooleanArray(cols) }
        for (c in 0 until cols) {
            board[rows - 1][c] = true
            board[rows - 2][c] = true
        }
        setNetField(board)

        netManager.checkBottomLine()

        assertEquals(2, NetManager.combo)
        assertTrue("no row should still be full after clearing", netField().none { row -> row.all { it } })
    }

    @Test
    fun checkBottomLine_clearsNonContiguousFullRows() {
        // Two full rows separated by non-full rows, with a marker cell above and one
        // between them, so we can confirm both full rows are removed - not just the
        // topmost one - and that the surviving rows are neither lost nor duplicated.
        val rows = netManager.netRowCount
        val cols = horizontalSquareCount
        val board = Array(rows) { BooleanArray(cols) }
        for (c in 0 until cols) {
            board[10][c] = true
            board[15][c] = true
        }
        board[5][0] = true  // marker above both full rows
        board[12][0] = true // marker between the two full rows
        setNetField(board)

        netManager.checkBottomLine()

        assertEquals("both separated full rows should count towards the clear", 2, NetManager.combo)
        val result = netField()
        assertTrue("no row should still be full after clearing", result.none { row -> row.all { it } })

        val markerRows = result.indices.filter { result[it][0] }
        assertEquals("both markers must survive - none lost, none duplicated", 2, markerRows.size)
        assertTrue("the marker that started above the other must stay above it",
            markerRows[0] < markerRows[1])
    }

    @Test
    fun canRotate_doesNotMutateTheBoard() {
        netManager.initFigure(squareAt(5, 5))
        netManager.initFigure(squareAt(0, 0))
        val cellCountBefore = netManager.stoppedFiguresPaths.size

        // Only initRotatedFigure() should ever commit a rotation - canRotate() is a
        // pure check. Verify that holds for both a rejected and an accepted candidate.
        netManager.canRotate(squareAt(5, 5))
        assertEquals(cellCountBefore, netManager.stoppedFiguresPaths.size)

        netManager.canRotate(squareAt(2, 2))
        assertEquals(cellCountBefore, netManager.stoppedFiguresPaths.size)
    }
}