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
}
