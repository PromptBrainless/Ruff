package de.ruff.dimension.ui.akte

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ruff.dimension.data.repository.CreatureProfileRepository
import de.ruff.dimension.di.IoDispatcher
import de.ruff.dimension.domain.akte.AkteEntwurf
import de.ruff.dimension.domain.akte.CreatureProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Bedienzustand der Kreaturen-Akten. */
@Immutable
internal data class AkteUiState(
    /** Aktive Akten je Wesen-ID; leer, solange nichts geladen ist. */
    val profiles: Map<String, CreatureProfile> = emptyMap(),
    /** false, wenn die Akten-Tabelle nicht gelesen werden kann (Abschnitte bleiben dann aus). */
    val available: Boolean = true,
    /** Wesen, dessen Akte gerade im Editor offen ist. */
    val editingId: String? = null,
    /** Prüffehler des Editors je Entwurfsschlüssel. */
    val feldFehler: Map<String, String> = emptyMap(),
    val error: String? = null,
    val busy: Boolean = false,
    /** Kurze Rückmeldung, z. B. nach „Werte bestätigen“. */
    val hinweis: String? = null,
) {
    fun profile(creatureId: String): CreatureProfile? = profiles[creatureId]

    fun isEditing(creatureId: String): Boolean = editingId == creatureId
}

/** Was die Detailansicht von den Akten braucht; über [LocalCreatureProfiles] bereitgestellt. */
@Immutable
internal data class AkteBinding(
    val state: AkteUiState,
    val onEdit: (String) -> Unit,
    val onCancel: () -> Unit,
    val onSave: (creatureId: String, entwurf: Map<String, String>) -> Unit,
    val onConfirm: (String) -> Unit,
    val onDismissHinweis: () -> Unit,
)

/**
 * Akten für die Detailansicht. `null` (Standard, etwa in Tests und Vorschauen) blendet
 * die Akten-Abschnitte aus; die übrige Detailansicht bleibt unverändert.
 */
internal val LocalCreatureProfiles = compositionLocalOf<AkteBinding?> { null }

/**
 * Liest und bearbeitet Kreaturen-Akten. Eigenes ViewModel, damit die Bibliothek
 * (LibraryViewModel) unverändert bleibt. Der offene Editor liegt im
 * [SavedStateHandle]; der Entwurf selbst im Editor (rememberSaveable).
 */
@HiltViewModel
internal class CreatureProfileViewModel
    @Inject
    constructor(
        private val repository: CreatureProfileRepository,
        private val savedStateHandle: SavedStateHandle,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val local =
            MutableStateFlow(AkteUiState(editingId = savedStateHandle.get<String>(KEY_EDITING)))

        private data class Geladen(
            val profiles: Map<String, CreatureProfile>,
            val available: Boolean,
            val error: String?,
        )

        private val geladen =
            repository
                .observeProfiles()
                .map { Geladen(it, available = true, error = null) }
                .flowOn(ioDispatcher)
                .catch { e ->
                    if (e is CancellationException) throw e
                    emit(Geladen(emptyMap(), available = false, error = "Akten konnten nicht geladen werden: ${e.message}"))
                }

        val uiState: StateFlow<AkteUiState> =
            combine(local, geladen) { l, g ->
                l.copy(profiles = g.profiles, available = g.available, error = l.error ?: g.error)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), local.value)

        fun startEdit(creatureId: String) {
            savedStateHandle[KEY_EDITING] = creatureId
            local.update { it.copy(editingId = creatureId, feldFehler = emptyMap(), error = null, hinweis = null) }
        }

        fun cancelEdit() {
            savedStateHandle[KEY_EDITING] = null
            local.update { it.copy(editingId = null, feldFehler = emptyMap(), error = null) }
        }

        fun save(
            creatureId: String,
            entwurf: Map<String, String>,
        ) {
            if (local.value.busy) return
            val quellen =
                uiState.value
                    .profile(creatureId)
                    ?.quellen
                    .orEmpty()
            when (val ergebnis = AkteEntwurf.zuProfil(creatureId, entwurf, quellen)) {
                is AkteEntwurf.Ergebnis.Fehler ->
                    local.update { it.copy(feldFehler = ergebnis.fehler, error = "Bitte markierte Felder prüfen.") }
                is AkteEntwurf.Ergebnis.Ok -> {
                    local.update { it.copy(busy = true, feldFehler = emptyMap(), error = null) }
                    viewModelScope.launch {
                        try {
                            withContext(ioDispatcher) { repository.save(ergebnis.profile) }
                            savedStateHandle[KEY_EDITING] = null
                            local.update { it.copy(busy = false, editingId = null, hinweis = "Akte gespeichert") }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            local.update { it.copy(busy = false, error = "Speichern fehlgeschlagen: ${e.message}") }
                        }
                    }
                }
            }
        }

        fun confirmDerived(creatureId: String) {
            if (local.value.busy) return
            local.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                try {
                    val n = withContext(ioDispatcher) { repository.confirmDerived(creatureId) }
                    local.update { it.copy(busy = false, hinweis = if (n == 1) "1 Wert bestätigt" else "$n Werte bestätigt") }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    local.update { it.copy(busy = false, error = "Bestätigen fehlgeschlagen: ${e.message}") }
                }
            }
        }

        fun dismissHinweis() {
            local.update { it.copy(hinweis = null) }
        }

        private companion object {
            const val KEY_EDITING = "akte.editing"
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
