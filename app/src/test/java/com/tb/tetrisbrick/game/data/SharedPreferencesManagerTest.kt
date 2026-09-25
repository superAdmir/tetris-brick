package com.tb.tetrisbrick.game.data

import android.content.Context
import com.tb.tetrisbrick.game.Values
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class SharedPreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var manager: SharedPreferencesManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(Values.PREFERENCES_KEY, Context.MODE_PRIVATE)
            .edit().clear().commit()
        manager = SharedPreferencesManager(context)
    }

    @Test
    fun freshInstall_hasNoLegacyOrCurrentData_returnsDefaultColorKey() {
        assertEquals(Values.DEFAULT_FIGURE_COLOR_KEY, manager.figuresColorKey)
    }

    @Test
    fun getBestScore_matchesFirstValue_andReflectsStateBeforeANewPutNewScoreCall() {
        assertEquals(0, manager.bestScore)

        manager.putNewScore(60)
        assertEquals(60, manager.bestScore)

        // The caller must read getBestScore() BEFORE calling putNewScore() to compare
        // against the previous best, not the value it just overwrote - this asserts the
        // method itself always reflects current persisted state, matching getFirstValue().
        val previousBest = manager.bestScore
        manager.putNewScore(45)
        assertEquals("45 didn't beat the previous best of 60, so bestScore must be unchanged",
            previousBest, manager.bestScore)
    }

    @Test
    fun legacyIntOnlyInstall_migratesToDefaultColorKeyAndRetiresLegacyEntry() {
        // Simulate a pre-upgrade install that only ever wrote the old int-based key -
        // exactly what SharedPreferencesManager.setFiguresColor(int) used to do.
        val prefs = context.getSharedPreferences(Values.PREFERENCES_KEY, Context.MODE_PRIVATE)
        prefs.edit().putInt(Values.LEGACY_FIGURE_COLOR_KEY, 12345 /* some old, untrustworthy resource id */).commit()

        val migratedKey = manager.figuresColorKey

        assertEquals("must fall back to the default color, never reinterpret the old int",
            Values.DEFAULT_FIGURE_COLOR_KEY, migratedKey)
        assertFalse("legacy entry must be retired so it's never read again",
            prefs.contains(Values.LEGACY_FIGURE_COLOR_KEY))
        assertTrue("the new key must now be persisted", prefs.contains(Values.FIGURE_COLOR_KEY))

        // A second read must be stable (no re-migration, no crash reading the retired key).
        assertEquals(Values.DEFAULT_FIGURE_COLOR_KEY, manager.figuresColorKey)
    }

    @Test
    fun currentFormatData_isReturnedDirectly_migrationDoesNotInterfere() {
        manager.setFiguresColor(Values.FIGURE_COLOR_L)

        assertEquals(Values.FIGURE_COLOR_L, manager.figuresColorKey)
    }

    @Test
    fun bothLegacyAndCurrentDataPresent_currentFormatWins() {
        val prefs = context.getSharedPreferences(Values.PREFERENCES_KEY, Context.MODE_PRIVATE)
        prefs.edit().putInt(Values.LEGACY_FIGURE_COLOR_KEY, 12345).commit()
        manager.setFiguresColor(Values.FIGURE_COLOR_J)

        assertEquals(Values.FIGURE_COLOR_J, manager.figuresColorKey)
    }

    // Simulates an actual upgrading install: high scores, gameplay settings, and the old
    // int-based color preference all present together (not a fresh install, and not just
    // the color key in isolation) - the scenario the "do not claim a successful upgrade
    // migration based solely on fresh-install tests" requirement is about.
    @Test
    fun upgradeInstall_highScoresAndSettingsSurviveAlongsideColorMigration() {
        val prefs = context.getSharedPreferences(Values.PREFERENCES_KEY, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(Values.LEGACY_FIGURE_COLOR_KEY, 98765) // old, untrustworthy resource id
            .putInt(Values.FIRST_VALUE_KEY, 500)
            .putInt(Values.SECOND_VALUE_KEY, 300)
            .putInt(Values.THIRD_VALUE_KEY, 100)
            .putLong(Values.FIGURE_SPEED_KEY, 900L)
            .putBoolean(Values.ENABLE_HINTS_KEY, false)
            .putInt(Values.SQUARES_COUNT_IN_ROW_KEY, 12)
            .commit()

        // Existing high scores and settings must read back exactly as they were.
        assertEquals("500", manager.firstValue)
        assertEquals("300", manager.secondValue)
        assertEquals("100", manager.thirdValue)
        assertEquals(900L, manager.figuresSpeed)
        assertFalse(manager.isHintsEnabled)
        assertEquals(12, manager.squaresCountInRow)

        // The color preference migrates safely, same as in isolation.
        assertEquals(Values.DEFAULT_FIGURE_COLOR_KEY, manager.figuresColorKey)
        assertFalse(prefs.contains(Values.LEGACY_FIGURE_COLOR_KEY))

        // A new high score set after the upgrade must correctly slot in above the old
        // ones without disturbing the settings that were just verified above.
        manager.putNewScore(700)
        assertEquals("700", manager.firstValue)
        assertEquals("500", manager.secondValue)
        assertEquals("300", manager.thirdValue)
        assertEquals(900L, manager.figuresSpeed)
        assertEquals(12, manager.squaresCountInRow)
    }
}
