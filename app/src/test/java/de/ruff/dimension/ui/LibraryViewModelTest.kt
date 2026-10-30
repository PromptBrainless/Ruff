package de.ruff.dimension.ui

import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RuleBlock
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.domain.DataOrigin
import de.ruff.dimension.domain.LibraryData
import de.ruff.dimension.domain.LibrarySource
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.IOException

/** ViewModel mit Fake-Datenquelle; Robolectric stellt den Main-Looper für viewModelScope. */
@RunWith(RobolectricTestRunner::class)
class LibraryViewModelTest {
    private val fuchs = Creature(id = "A-1", name = "Fuchs", powerLevel = 1, powerLabel = "Stufe I")
    private val drache = Creature(id = "A-2", name = "Drache", powerLevel = 3, powerLabel = "Stufe III")
    private val rule = RulePage(title = "Kampf", source = "regeln/kampf.md", blocks = listOf(RuleBlock("p", "Text")))
    private val data = LibraryData(listOf(fuchs, drache), listOf(rule), DataOrigin.DATABASE)

    private fun viewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        source: LibrarySource = LibrarySource { data },
    ) = LibraryViewModel(handle, source, Dispatchers.Unconfined).also { idle() }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun loadsDataIntoReadyState() {
        val state = viewModel().uiState.value
        assertEquals(LoadState.Ready(data), state.load)
        assertEquals(listOf(fuchs, drache), state.creatures)
        assertEquals(listOf(rule), state.rules)
        assertEquals(LibraryTab.CREATURES, state.tab)
        assertNull(state.selectedCreature)
    }

    @Test
    fun tabChangeResetsPowerFilterButKeepsQuery() {
        val vm = viewModel()
        vm.setQuery("fu")
        vm.setPowerFilter(3)
        vm.selectTab(LibraryTab.RULES)
        idle()
        val state = vm.uiState.value
        assertEquals(LibraryTab.RULES, state.tab)
        assertNull(state.powerFilter)
        assertEquals("fu", state.query)
    }

    @Test
    fun selectionSurvivesRecreationWithSameSavedState() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.setQuery("drache")
        first.setPowerFilter(3)
        first.openCreature(drache)
        idle()
        assertEquals(drache, first.uiState.value.selectedCreature)

        val recreated = viewModel(handle).uiState.value
        assertEquals(drache, recreated.selectedCreature)
        assertEquals("drache", recreated.query)
        assertEquals(3, recreated.powerFilter)
    }

    @Test
    fun ruleSelectionAndCloseDetails() {
        val vm = viewModel()
        vm.openRule(rule)
        idle()
        assertEquals(rule, vm.uiState.value.selectedRule)
        vm.closeDetails()
        idle()
        assertNull(vm.uiState.value.selectedRule)
        assertNull(vm.uiState.value.selectedCreature)
    }

    @Test
    fun errorStateAndSuccessfulRetry() {
        var calls = 0
        val vm =
            viewModel(
                source = {
                    calls++
                    if (calls == 1) throw IOException("kaputt") else data
                },
            )
        assertTrue(vm.uiState.value.load is LoadState.Error)
        assertTrue(
            vm.uiState.value.creatures
                .isEmpty(),
        )
        vm.reload()
        idle()
        assertEquals(LoadState.Ready(data), vm.uiState.value.load)
        assertEquals(2, calls)
    }
}
