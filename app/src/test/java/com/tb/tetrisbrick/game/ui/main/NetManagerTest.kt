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