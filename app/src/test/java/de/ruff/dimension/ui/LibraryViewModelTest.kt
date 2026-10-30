package de.ruff.dimension.ui

import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RuleBlock
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.repository.ContentEditorRepository
import de.ruff.dimension.data.repository.FavoritesRepository
import de.ruff.dimension.data.repository.SearchRepository
import de.ruff.dimension.domain.DataOrigin
import de.ruff.dimension.domain.LibraryData
import de.ruff.dimension.domain.LibrarySource
import de.ruff.dimension.domain.ObserveFavorites
import de.ruff.dimension.domain.SearchLibrary
import de.ruff.dimension.domain.ToggleFavorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private val editor =
        object : ContentEditorRepository {
            override suspend fun apply(data: LibraryData) = data

            override suspend fun saveCreature(creature: Creature) = Unit

            override suspend fun saveRule(rule: RulePage) = Unit

            override suspend fun restoreCreature(id: String) = Unit

            override suspend fun restoreRule(source: String) = Unit
        }

    /** Favoriten im Speicher; [failWrites] simuliert einen Schreibfehler der Datenbank. */
    private class FakeFavorites : FavoritesRepository {
        val ids = MutableStateFlow<Set<String>>(emptySet())
        var failWrites = false
        var writes = 0

        override fun observeFavoriteIds(): Flow<Set<String>> = ids

        override suspend fun isFavorite(creatureId: String) = creatureId in ids.value

        override suspend fun setFavorite(
            creatureId: String,
            favorite: Boolean,
        ): Boolean {
            writes++
            if (failWrites) throw IOException("Platte voll")
            ids.value = if (favorite) ids.value + creatureId else ids.value - creatureId
            return favorite
        }

        override suspend fun toggle(creatureId: String) = setFavorite(creatureId, creatureId !in ids.value)
    }

    private val favorites = FakeFavorites()

    private fun viewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        source: LibrarySource = LibrarySource { data },
        favoritesRepository: FavoritesRepository = favorites,
    ) = LibraryViewModel(
        handle,
        source,
        editor,
        SearchLibrary(SearchRepository { null }),
        ObserveFavorites(favoritesRepository),
        ToggleFavorite(favoritesRepository),
        Dispatchers.Unconfined,
    ).also { idle() }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun loadsDataIntoReadyState() {
        val state = viewModel().uiState.value
        assertEquals(LoadState.Ready(data), state.load)
        assertEquals(listOf(fuchs, drache), state.creatures)
        assertEquals(listOf(rule), state.rules)
        assertEquals(LibraryTab.CREATURES, state.tab)
        assertNull(state.selectedCreature)
        assertTrue(state.canEdit)
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
    fun editorModeOpensAndClosesWithDetails() {
        val vm = viewModel()
        vm.openCreature(fuchs)
        vm.setEditing(true)
        idle()
        assertTrue(vm.uiState.value.editing)

        vm.closeDetails()
        idle()
        assertFalse(vm.uiState.value.editing)
    }

    @Test
    fun localEditsUseSubstringSearchInsteadOfStaleFtsIndex() {
        var searchCalls = 0
        val editedData = data.copy(hasLocalEdits = true)
        val editedSource = LibrarySource { editedData }
        val editedRepository =
            object : ContentEditorRepository {
                override suspend fun apply(data: LibraryData) = editedData

                override suspend fun saveCreature(creature: Creature) = Unit

                override suspend fun saveRule(rule: RulePage) = Unit

                override suspend fun restoreCreature(id: String) = Unit

                override suspend fun restoreRule(source: String) = Unit
            }
        val vm =
            LibraryViewModel(
                SavedStateHandle(),
                editedSource,
                editedRepository,
                SearchLibrary(
                    SearchRepository {
                        searchCalls++
                        null
                    },
                ),
                ObserveFavorites(favorites),
                ToggleFavorite(favorites),
                Dispatchers.Unconfined,
            )
        idle()
        vm.setQuery("bearbeitet")
        idle()

        assertEquals(0, searchCalls)
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

    @Test
    fun toggleFavoriteFollowsRepositoryState() {
        val vm = viewModel()
        assertTrue(vm.uiState.value.favoritesAvailable)
        assertFalse(vm.uiState.value.isFavorite("A-1"))

        vm.toggleFavorite("A-1")
        idle()
        assertEquals(setOf("A-1"), vm.uiState.value.favoriteIds)

        vm.toggleFavorite("A-1")
        idle()
        assertEquals(emptySet<String>(), vm.uiState.value.favoriteIds)
    }

    @Test
    fun favoritesOnlyIsSavedAndResetOnTabChange() {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.setFavoritesOnly(true)
        idle()
        assertTrue(vm.uiState.value.favoritesOnly)
        assertEquals(true, handle.get<Boolean>(LibraryViewModel.KEY_FAVORITES_ONLY))

        // Neu erzeugtes ViewModel (Prozess-Tod) übernimmt den Filter.
        assertTrue(viewModel(handle).uiState.value.favoritesOnly)

        vm.selectTab(LibraryTab.RULES)
        idle()
        assertFalse(vm.uiState.value.favoritesOnly)
    }

    @Test
    fun favoritesFilterKeepsOnlyFavoritesAndIgnoresOrphans() {
        favorites.ids.value = setOf("A-2", "gibt-es-nicht")
        val vm = viewModel()
        vm.setFavoritesOnly(true)
        idle()
        val state = vm.uiState.value
        assertEquals(
            listOf(drache),
            de.ruff.dimension.domain
                .searchCreatures(state.creatures, "", null, null, state.favoritesFilter),
        )
    }

    @Test
    fun writeFailureShowsErrorAndKeepsState() {
        favorites.failWrites = true
        val vm = viewModel()
        vm.toggleFavorite("A-1")
        idle()
        assertEquals(emptySet<String>(), vm.uiState.value.favoriteIds)
        assertTrue(
            vm.uiState.value.favoriteError!!
                .contains("IOException"),
        )

        favorites.failWrites = false
        vm.toggleFavorite("A-1")
        idle()
        assertNull(vm.uiState.value.favoriteError)
        assertEquals(setOf("A-1"), vm.uiState.value.favoriteIds)
    }

    @Test
    fun readFailureShowsError() {
        val broken =
            object : FavoritesRepository by favorites {
                override fun observeFavoriteIds(): Flow<Set<String>> = flow { throw IOException("kaputt") }
            }
        val vm = viewModel(favoritesRepository = broken)
        assertTrue(
            vm.uiState.value.favoriteError!!
                .contains("IOException"),
        )
    }

    @Test
    fun noFavoritesWithoutDatabase() {
        val assetsOnly = data.copy(origin = DataOrigin.ASSETS_FALLBACK)
        val vm = viewModel(source = LibrarySource { assetsOnly })
        vm.setFavoritesOnly(true)
        vm.toggleFavorite("A-1")
        idle()
        val state = vm.uiState.value
        assertFalse(state.favoritesAvailable)
        assertFalse(state.favoritesFilter.only)
        assertEquals(0, favorites.writes)
    }
}
