package de.ruff.dimension

import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import de.ruff.dimension.domain.DataOrigin
import de.ruff.dimension.ui.LibraryViewModel
import de.ruff.dimension.ui.LoadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Startet die echte App (RuffApplication mit Hilt, MainActivity mit Compose)
 * unter Robolectric: Hilt-Graph, Room-Datei-Datenbank, Seed und erste
 * Komposition laufen ohne Absturz. Ersetzt keinen Test auf einem Gerät.
 * Manifest und Ressourcen kommen über `unitTests.isIncludeAndroidResources`,
 * geprüft wird also auch der Manifest-Eintrag der Application-Klasse.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivitySmokeTest {
    @Test
    fun appStartsSeedsRoomAndShowsLibrary() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertTrue(activity.application is RuffApplication)
        val viewModel = ViewModelProvider(activity)[LibraryViewModel::class.java]

        val deadline = System.currentTimeMillis() + 30_000
        while (viewModel.uiState.value.load is LoadState.Loading && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
            Thread.sleep(20)
        }
        val load = viewModel.uiState.value.load
        assertTrue("Zustand: $load", load is LoadState.Ready)
        load as LoadState.Ready
        assertEquals(DataOrigin.DATABASE, load.data.origin)
        assertEquals(187, load.data.creatures.size)
        assertEquals(12, load.data.rules.size)

        // Wechsel in eine Detailansicht und zurück, inklusive Neuaufbau nach Drehen.
        viewModel.openCreature(load.data.creatures.first())
        controller.recreate()
        shadowOf(Looper.getMainLooper()).idle()
        val afterRotation = ViewModelProvider(controller.get())[LibraryViewModel::class.java]
        assertEquals(load.data.creatures.first(), afterRotation.uiState.value.selectedCreature)
        assertFalse(controller.get().isFinishing)
        controller.pause().stop().destroy()
    }
}
