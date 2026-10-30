package de.ruff.dimension.ui.common

import android.provider.Settings
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import de.ruff.dimension.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Hauptmenü: Partikel im Normalmodus vorhanden, im reduzierten Modus nicht komponiert. */
@RunWith(RobolectricTestRunner::class)
class GlutPartikelUiTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun launchWithScale(scale: Float): ActivityScenario<MainActivity> {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
        return ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun reset() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

    @Test
    fun menueZeigtGlutPartikel() {
        launchWithScale(1f).use {
            compose.waitForIdle()
            compose.onNodeWithTag("menu").assertExists()
            compose.onNodeWithTag("glut_partikel").assertExists()
        }
    }

    @Test
    fun reduzierterModusOhnePartikel() {
        launchWithScale(0f).use {
            compose.waitForIdle()
            compose.onNodeWithTag("menu").assertExists()
            compose.onNodeWithTag("glut_partikel").assertDoesNotExist()
        }
    }
}
