package com.tb.tetrisbrick.game.data

import android.content.Context
import com.tb.tetrisbrick.game.Values
import com.tb.tetrisbrick.game.enums.FigureType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// See NetManagerTest for why SDK is pinned to 34 (targetSdk 36's shadow needs Java 21).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameStateStoreTest {

    private lateinit var context: Context
    private lateinit var store: GameStateStore

    private val squaresInRowCount = 10

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(Values.PREFERENCES_KEY, Context.MODE_PRIVATE).edit().clear().commit()
        store = GameStateStore(context)
    }

    private fun sampleNet(): Array<BooleanArray> {
        val net = Array(26) { BooleanArray(squaresInRowCount) }
        net[25][0] = true
        net[25][1] = true
        net[10][5] = true
        return net
    }

    @Test
    fun noSaveYet_hasSavedGameIsFalse_loadReturnsNull() {
        assertFalse(store.hasSavedGame())
        assertNull(store.load(squaresInRowCount))
    }

    @Test
    fun saveThenLoad_roundTripsEveryField() {
        val net = sampleNet()
        val saved = SavedGame(420, squaresInRowCount, net, FigureType.T_SECOND_FIGURE, 3, 7, FigureType.LONG_FIGURE)
        store.save(saved)

        assertTrue(store.hasSavedGame())
        val loaded = store.load(squaresInRowCount)

        assertEquals(420, loaded!!.score)
        assertEquals(squaresInRowCount, loaded.squaresInRowCount)
        assertEquals(FigureType.T_SECOND_FIGURE, loaded.currentFigureType)
        assertEquals(3, loaded.currentFigureGridX)
        assertEquals(7, loaded.currentFigureGridY)
        assertEquals(FigureType.LONG_FIGURE, loaded.nextFigureType)
        for (row in net.indices) {
            assertArrayEquals(net[row], loaded.net[row])
        }
    }

    @Test
    fun clear_removesTheSave() {
        store.save(SavedGame(10, squaresInRowCount, sampleNet(), FigureType.J_FIGURE, 0, 0, FigureType.Z_FIGURE))
        store.clear()

        assertFalse(store.hasSavedGame())
        assertNull(store.load(squaresInRowCount))
    }

    @Test
    fun load_rejectsMismatchedBoardWidth() {
        store.save(SavedGame(10, squaresInRowCount, sampleNet(), FigureType.J_FIGURE, 0, 0, FigureType.Z_FIGURE))

        // The user changed "squares in a row" in Settings while the game was backgrounded -
        // the saved board no longer matches the board about to be built, must not restore it.
        assertNull(store.load(squaresInRowCount + 2))
    }

    @Test
    fun load_rejectsCorruptedNetCellString() {
        val prefs = context.getSharedPreferences(Values.PREFERENCES_KEY, Context.MODE_PRIVATE)
        store.save(SavedGame(10, squaresInRowCount, sampleNet(), FigureType.J_FIGURE, 0, 0, FigureType.Z_FIGURE))
        // Simulate corruption / a future format change truncating the cell data.
        prefs.edit().putString(Values.SAVED_GAME_NET_CELLS_KEY, "01").commit()

        assertNull(store.load(squaresInRowCount))
    }

    @Test
    fun load_rejectsUnrecognizedFigureTypeName() {
        val prefs = context.getSharedPreferences(Values.PREFERENCES_KEY, Context.MODE_PRIVATE)
        store.save(SavedGame(10, squaresInRowCount, sampleNet(), FigureType.J_FIGURE, 0, 0, FigureType.Z_FIGURE))
        // Simulate a save written by some future/incompatible schema version.
        prefs.edit().putString(Values.SAVED_GAME_CURRENT_TYPE_KEY, "NOT_A_REAL_FIGURE_TYPE").commit()

        assertNull(store.load(squaresInRowCount))
    }

    @Test
    fun load_rejectsWrongSchemaVersion() {
        val prefs = context.getSharedPreferences(Values.PREFERENCES_KEY, Context.MODE_PRIVATE)
        store.save(SavedGame(10, squaresInRowCount, sampleNet(), FigureType.J_FIGURE, 0, 0, FigureType.Z_FIGURE))
        prefs.edit().putInt(Values.SAVED_GAME_VERSION_KEY, Values.SAVED_GAME_SCHEMA_VERSION + 1).commit()

        assertFalse(store.hasSavedGame())
        assertNull(store.load(squaresInRowCount))
    }
}
