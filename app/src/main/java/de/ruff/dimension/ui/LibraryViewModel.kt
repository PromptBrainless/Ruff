package de.ruff.dimension.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.repository.ContentEditorRepository
import de.ruff.dimension.data.repository.SearchHits
import de.ruff.dimension.di.IoDispatcher
import de.ruff.dimension.domain.DataOrigin
import de.ruff.dimension.domain.LibraryData
import de.ruff.dimension.domain.LibrarySource
import de.ruff.dimension.domain.ObserveFavorites
import de.ruff.dimension.domain.SearchLibrary
import de.ruff.dimension.domain.ToggleFavorite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal enum class LibraryTab(
    val label: String,
) {
    CREATURES("Wesen"),
    RULES("Regeln"),
}

/** Ladezustand der Inhalte. */
internal sealed interface LoadState {
    data object Loading : LoadState

    data class Error(
        val message: String,
    ) : LoadState

    data class Ready(
        val data: LibraryData,
    ) : LoadState
}

/** Bedienzustand; liegt im [SavedStateHandle] und übersteht Drehen und Prozessende. */
internal data class LibraryUiState(
    val load: LoadState = LoadState.Loading,
    val tab: LibraryTab = LibraryTab.CREATURES,
    val query: String = "",
    val powerFilter: Int? = null,
    val selectedCreature: Creature? = null,
    val selectedRule: RulePage? = null,
    val searchHits: SearchHits? = null,
    val editing: Boolean = false,
    val editorError: String? = null,
    /** Aktive Favoriten (Wesen-IDs), in Reihenfolge des Favorisierens. */
    val favoriteIds: Set<String> = emptySet(),
    /** Filter „Nur Favoriten“; liegt wie die Machtstufe im SavedStateHandle. */
    val favoritesOnly: Boolean = false,
    val favoriteError: String? = null,
) {
    val creatures: List<Creature> get() = (load as? LoadState.Ready)?.data?.creatures.orEmpty()
    val rules: List<RulePage> get() = (load as? LoadState.Ready)?.data?.rules.orEmpty()
    val canEdit: Boolean get() = (load as? LoadState.Ready)?.data?.origin == DataOrigin.DATABASE

    /** Favoriten brauchen die Datenbank; im Asset-Notbetrieb sind sie ausgeblendet. */
    val favoritesAvailable: Boolean get() = canEdit

    fun isFavorite(creatureId: String): Boolean = creatureId in favoriteIds
}

@HiltViewModel
internal class LibraryViewModel
    @Inject
    constructor(
        private val savedState: SavedStateHandle,
        private val source: LibrarySource,
        private val editor: ContentEditorRepository,
        private val search: SearchLibrary,
        private val observeFavorites: ObserveFavorites,
        private val toggleFavoriteUseCase: ToggleFavorite,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val load = MutableStateFlow<LoadState>(LoadState.Loading)
        private val searchHits = MutableStateFlow<SearchHits?>(null)
        private val editing = MutableStateFlow(false)
        private val editorError = MutableStateFlow<String?>(null)
        private val favoriteIds = MutableStateFlow<Set<String>>(emptySet())
        private val favoriteError = MutableStateFlow<String?>(null)

        private data class EditState(
            val editing: Boolean,
            val error: String?,
        )

        private data class FavoriteState(
            val ids: Set<String>,
            val only: Boolean,
            val error: String?,
        )

        private val editState = combine(editing, editorError, ::EditState)

        private val favoriteState =
            combine(favoriteIds, savedState.getStateFlow(KEY_FAVORITES_ONLY, false), favoriteError, ::FavoriteState)

        private data class Selection(
            val tab: LibraryTab,
            val query: String,
            val powerFilter: Int?,
            val creatureId: String?,
            val ruleId: String?,
        )

        private val selection =
            combine(
                savedState.getStateFlow(KEY_TAB, LibraryTab.CREATURES.name),
                savedState.getStateFlow(KEY_QUERY, ""),
                savedState.getStateFlow<Int?>(KEY_POWER, null),
                savedState.getStateFlow<String?>(KEY_CREATURE, null),
                savedState.getStateFlow<String?>(KEY_RULE, null),
            ) { tab, query, power, creatureId, ruleId ->
                Selection(LibraryTab.valueOf(tab), query, power, creatureId, ruleId)
            }

        val uiState: StateFlow<LibraryUiState> =
            combine(load.asStateFlow(), selection, searchHits, editState, favoriteState) { loadState, sel, hits, edit, fav ->
                val data = (loadState as? LoadState.Ready)?.data
                LibraryUiState(
                    load = loadState,
                    tab = sel.tab,
                    query = sel.query,
                    powerFilter = sel.powerFilter,
                    selectedCreature = sel.creatureId?.let { id -> data?.creatures?.firstOrNull { it.id == id } },
                    selectedRule = sel.ruleId?.let { id -> data?.rules?.firstOrNull { it.source == id } },
                    searchHits = hits,
                    editing = edit.editing,
                    editorError = edit.error,
                    favoriteIds = fav.ids,
                    favoritesOnly = fav.only,
                    favoriteError = fav.error,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryUiState())

        init {
            reload()
            viewModelScope.launch {
                observeFavorites()
                    .catch { e ->
                        if (e is CancellationException) throw e
                        favoriteError.value = "Favoriten konnten nicht gelesen werden (${e.javaClass.simpleName})."
                    }.collect { favoriteIds.value = it }
            }
            // Volltextsuche nur, wenn die Daten aus Room kommen; sonst bleibt es bei der Teilstring-Suche.
            viewModelScope.launch {
                combine(load, savedState.getStateFlow(KEY_QUERY, "")) { loadState, query -> loadState to query }
                    .collectLatest { (loadState, query) ->
                        val data = (loadState as? LoadState.Ready)?.data
                        val useFullText = data?.origin == DataOrigin.DATABASE && !data.hasLocalEdits
                        searchHits.value =
                            if (useFullText) {
                                try {
                                    withContext(ioDispatcher) { search(query) }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    null
                                }
                            } else {
                                null
                            }
                    }
            }
        }

        /** Lädt die Inhalte im Hintergrund; beim ersten Start inklusive Seed. */
        fun reload() {
            load.value = LoadState.Loading
            viewModelScope.launch {
                load.value =
                    try {
                        LoadState.Ready(
                            withContext(ioDispatcher) {
                                source.load().let { data ->
                                    if (data.origin == DataOrigin.DATABASE) editor.apply(data) else data
                                }
                            },
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        LoadState.Error("Die Inhalte konnten nicht geladen werden (${e.javaClass.simpleName}).")
                    }
            }
        }

        fun selectTab(tab: LibraryTab) {
            savedState[KEY_TAB] = tab.name
            savedState[KEY_POWER] = null
            savedState[KEY_FAVORITES_ONLY] = false
        }

        fun setQuery(query: String) {
            savedState[KEY_QUERY] = query
        }

        fun setPowerFilter(level: Int?) {
            savedState[KEY_POWER] = level
        }

        fun setFavoritesOnly(value: Boolean) {
            savedState[KEY_FAVORITES_ONLY] = value
        }

        /**
         * Schaltet den Favoritenstatus um. Die Oberfläche folgt dem Datenbank-Flow,
         * damit Anzeige und gespeicherter Stand nie auseinanderlaufen.
         */
        fun toggleFavorite(creatureId: String) {
            if (!uiState.value.favoritesAvailable) return
            favoriteError.value = null
            viewModelScope.launch {
                try {
                    withContext(ioDispatcher) { toggleFavoriteUseCase(creatureId) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    favoriteError.value = "Favorit konnte nicht gespeichert werden (${e.javaClass.simpleName})."
                }
            }
        }

        fun openCreature(creature: Creature) {
            selectCreatureId(creature.id)
        }

        fun openRule(rule: RulePage) {
            selectRuleSource(rule.source)
        }

        /**
         * Gleicht die Auswahl mit der Navigation ab (`null` = keine Auswahl). Nur ein
         * echter Wechsel beendet eine offene Bearbeitung; Drehen lässt sie bestehen.
         */
        fun selectCreatureId(id: String?) {
            if (savedState.get<String?>(KEY_CREATURE) == id) return
            savedState[KEY_CREATURE] = id
            editing.value = false
            editorError.value = null
        }

        /** Wie [selectCreatureId] für Regelseiten (ID = `source`). */
        fun selectRuleSource(source: String?) {
            if (savedState.get<String?>(KEY_RULE) == source) return
            savedState[KEY_RULE] = source
            editing.value = false
            editorError.value = null
        }

        fun closeDetails() {
            savedState[KEY_CREATURE] = null
            savedState[KEY_RULE] = null
            editing.value = false
            editorError.value = null
        }

        fun setEditing(value: Boolean) {
            editing.value = value
            editorError.value = null
        }

        fun saveCreature(creature: Creature) {
            saveContent { editor.saveCreature(creature) }
        }

        fun saveRule(rule: RulePage) {
            saveContent { editor.saveRule(rule) }
        }

        fun restoreCreature(id: String) {
            saveContent { editor.restoreCreature(id) }
        }

        fun restoreRule(source: String) {
            saveContent { editor.restoreRule(source) }
        }

        private fun saveContent(operation: suspend () -> Unit) {
            editorError.value = null
            viewModelScope.launch {
                try {
                    withContext(ioDispatcher) { operation() }
                    editing.value = false
                    reload()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    editorError.value = "Die Änderung konnte nicht gespeichert werden (${e.javaClass.simpleName})."
                }
            }
        }

        internal companion object {
            const val KEY_TAB = "tab"
            const val KEY_QUERY = "query"
            const val KEY_POWER = "powerFilter"
            const val KEY_CREATURE = "creatureId"
            const val KEY_RULE = "ruleId"
            const val KEY_FAVORITES_ONLY = "favoritesOnly"
        }
    }
