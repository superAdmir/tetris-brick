package com.tb.tetrisbrick.game;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        Context appContext = ApplicationProvider.getApplicationContext();

        // Not a hardcoded literal: debug builds carry an applicationIdSuffix
        // (com.tb.tetrisbrick.game.debug) so they can install alongside a release
        // build without conflicting - BuildConfig.APPLICATION_ID always matches
        // whatever variant this test is actually compiled/running as.
        assertEquals(BuildConfig.APPLICATION_ID, appContext.getPackageName());
    }
}
