package de.ruff.dimension.ui.navigation

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import de.ruff.dimension.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Hilfsmittel, kein Qualitätstest: rendert Bildschirme mit Robolectric-Native-Grafik
 * als PNG, nur wenn `RUFF_SHOTS_DIR` gesetzt ist (sonst übersprungen). Keine
 * zusätzliche Abhängigkeit; die Bilder ersetzen keinen Blick auf ein echtes Gerät.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotCaptureTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val dir: File? = System.getenv("RUFF_SHOTS_DIR")?.let(::File)

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        // captureToImage() wartet unter Robolectric vergeblich auf einen Frame; daher direkt zeichnen.
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(bitmap)) }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun walk(prefix: String) {
        assumeTrue("RUFF_SHOTS_DIR nicht gesetzt", dir != null)
        dir!!.mkdirs()
        val ready = compose.awaitReady()
        shot("${prefix}_01_menu")
        compose.onNodeWithText("Wesen").performScrollTo().performClick()
        shot("${prefix}_02_wesen_liste")
        val creature = ready.data.creatures.first { it.portrait.isNotBlank() }
        compose.onNodeWithTag("creature_list").performScrollToNode(hasText(creature.name))
        compose.onAllNodesWithText(creature.name).onFirst().performClick()
        shot("${prefix}_03_wesen_detail")
        compose.pressSystemBack()
        compose.pressSystemBack()
        compose.onNodeWithText("Regelwerk").performScrollTo().performClick()
        shot("${prefix}_04_regel_liste")
        compose.onAllNodesWithText(ready.data.rules[1].title).onFirst().performClick()
        shot("${prefix}_05_regel_seite")
        compose.pressSystemBack()
        compose.pressSystemBack()
        compose.onNodeWithText("Suche").performScrollTo().performClick()
        compose.onNodeWithTag("search_field").performTextInput("ritter")
        shot("${prefix}_06_suche")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun phone() = walk("handy")

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun tablet() = walk("tablet")
}
