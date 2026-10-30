package de.ruff.dimension.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.di.IoDispatcher
import de.ruff.dimension.domain.LibraryData
import de.ruff.dimension.domain.LibrarySource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) {
    val creatures: List<Creature> get() = (load as? LoadState.Ready)?.data?.creatures.orEmpty()
    val rules: List<RulePage> get() = (load as? LoadState.Ready)?.data?.rules.orEmpty()
}

@HiltViewModel
internal class LibraryViewModel
    @Inject
    constructor(
        private val savedState: SavedStateHandle,
        private val source: LibrarySource,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val load = MutableStateFlow<LoadState>(LoadState.Loading)

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
            combine(load.asStateFlow(), selection) { loadState, sel ->
                val data = (loadState as? LoadState.Ready)?.data
                LibraryUiState(
                    load = loadState,
                    tab = sel.tab,
                    query = sel.query,
                    powerFilter = sel.powerFilter,
                    selectedCreature = sel.creatureId?.let { id -> data?.creatures?.firstOrNull { it.id == id } },
                    selectedRule = sel.ruleId?.let { id -> data?.rules?.firstOrNull { it.source == id } },
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryUiState())

        init {
            reload()
        }

        /** Lädt die Inhalte im Hintergrund; beim ersten Start inklusive Seed. */
        fun reload() {
            load.value = LoadState.Loading
            viewModelScope.launch {
                load.value =
                    try {
                        LoadState.Ready(withContext(ioDispatcher) { source.load() })
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
        }

        fun setQuery(query: String) {
            savedState[KEY_QUERY] = query
        }

        fun setPowerFilter(level: Int?) {
            savedState[KEY_POWER] = level
        }

        fun openCreature(creature: Creature) {
            savedState[KEY_CREATURE] = creature.id
        }

        fun openRule(rule: RulePage) {
            savedState[KEY_RULE] = rule.source
        }

        fun closeDetails() {
            savedState[KEY_CREATURE] = null
            savedState[KEY_RULE] = null
        }

        internal companion object {
            const val KEY_TAB = "tab"
            const val KEY_QUERY = "query"
            const val KEY_POWER = "powerFilter"
            const val KEY_CREATURE = "creatureId"
            const val KEY_RULE = "ruleId"
        }
    }
