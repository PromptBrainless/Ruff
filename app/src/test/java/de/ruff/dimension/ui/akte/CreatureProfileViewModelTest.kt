package de.ruff.dimension.ui.akte

import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import de.ruff.dimension.data.repository.CreatureProfileRepository
import de.ruff.dimension.domain.akte.AkteEntwurf
import de.ruff.dimension.domain.akte.Angabe
import de.ruff.dimension.domain.akte.CreatureProfile
import de.ruff.dimension.domain.akte.GanzzahlSpanne
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Akten-ViewModel mit Fake-Repository; Robolectric stellt den Main-Looper. */
@RunWith(RobolectricTestRunner::class)
class CreatureProfileViewModelTest {
    private val troll = CreatureProfile("V2-090", iq = Angabe(GanzzahlSpanne(60, 85), "Lore"))

    private class FakeRepo(
        initial: Map<String, CreatureProfile>,
    ) : CreatureProfileRepository {
        val profiles = MutableStateFlow(initial)
        val saved = mutableListOf<CreatureProfile>()
        var failObserve = false

        override fun observeProfiles(): Flow<Map<String, CreatureProfile>> =
            if (failObserve) flow { throw IllegalStateException("kaputt") } else profiles

        override suspend fun get(creatureId: String) = profiles.value[creatureId]

        override suspend fun save(edited: CreatureProfile): CreatureProfile {
            saved += edited
            profiles.value = profiles.value + (edited.creatureId to edited)
            return edited
        }

        override suspend fun confirmDerived(creatureId: String) = 3

        override suspend fun exportJson() = "{}"

        override suspend fun importJson(json: String) = 0
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    @After
    fun tearDown() = scope.cancel()

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun viewModel(
        repo: FakeRepo,
        handle: SavedStateHandle = SavedStateHandle(),
    ) = CreatureProfileViewModel(repo, handle, Dispatchers.Unconfined).also { vm ->
        scope.launch { vm.uiState.collect {} }
        idle()
    }

    @Test
    fun `Akten werden geladen`() {
        val vm = viewModel(FakeRepo(mapOf("V2-090" to troll)))
        assertEquals(troll, vm.uiState.value.profile("V2-090"))
        assertTrue(vm.uiState.value.available)
    }

    @Test
    fun `Lesefehler blendet die Akten aus statt abzustuerzen`() {
        val repo = FakeRepo(emptyMap()).apply { failObserve = true }
        val vm = viewModel(repo)
        assertFalse(vm.uiState.value.available)
        assertTrue(
            vm.uiState.value.error!!
                .contains("kaputt"),
        )
    }

    @Test
    fun `offener Editor uebersteht Prozessende`() {
        val handle = SavedStateHandle()
        val vm = viewModel(FakeRepo(emptyMap()), handle)
        vm.startEdit("V2-090")
        idle()
        assertTrue(vm.uiState.value.isEditing("V2-090"))
        assertTrue(viewModel(FakeRepo(emptyMap()), handle).uiState.value.isEditing("V2-090"))
        vm.cancelEdit()
        idle()
        assertNull(vm.uiState.value.editingId)
    }

    @Test
    fun `fehlerhafter Entwurf zeigt Feldfehler und speichert nicht`() {
        val repo = FakeRepo(mapOf("V2-090" to troll))
        val vm = viewModel(repo)
        vm.startEdit("V2-090")
        vm.save("V2-090", AkteEntwurf.aus(troll) + ("iq.min" to "viel"))
        idle()
        assertTrue(
            vm.uiState.value.feldFehler
                .containsKey("iq.min"),
        )
        assertTrue(vm.uiState.value.isEditing("V2-090"))
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun `gueltiger Entwurf wird gespeichert und schliesst den Editor`() {
        val repo = FakeRepo(mapOf("V2-090" to troll))
        val vm = viewModel(repo)
        vm.startEdit("V2-090")
        vm.save("V2-090", AkteEntwurf.aus(troll) + ("iq.max" to "95"))
        idle()
        assertEquals(
            GanzzahlSpanne(60, 95),
            repo.saved
                .single()
                .iq!!
                .wert,
        )
        assertNull(vm.uiState.value.editingId)
        assertEquals("Akte gespeichert", vm.uiState.value.hinweis)
    }

    @Test
    fun `Bestaetigen meldet die Anzahl`() {
        val vm = viewModel(FakeRepo(mapOf("V2-090" to troll)))
        vm.confirmDerived("V2-090")
        idle()
        assertEquals("3 Werte bestätigt", vm.uiState.value.hinweis)
        vm.dismissHinweis()
        idle()
        assertNull(vm.uiState.value.hinweis)
    }
}
