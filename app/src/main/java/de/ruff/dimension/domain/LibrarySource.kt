package de.ruff.dimension.domain

import android.util.Log
import de.ruff.dimension.data.AssetLoader
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.local.SeedSource
import de.ruff.dimension.data.repository.CreatureRepository
import de.ruff.dimension.data.repository.RuleRepository
import kotlinx.coroutines.CancellationException

/** Woher die angezeigten Daten stammen. */
internal enum class DataOrigin {
    DATABASE,
    ASSETS_FALLBACK,
}

/** Alle Inhalte der Bibliothek in der Reihenfolge der Seed-Dateien. */
internal data class LibraryData(
    val creatures: List<Creature>,
    val rules: List<RulePage>,
    val origin: DataOrigin,
    val hasLocalEdits: Boolean = false,
)

/** Liefert die Bibliotheksdaten; blockierend, daher nur außerhalb des Main-Threads aufrufen. */
internal fun interface LibrarySource {
    suspend fun load(): LibraryData
}

/**
 * Spielt bei Bedarf den Seed ein und liest dann aus Room.
 *
 * Schlägt Seed oder Datenbank fehl, werden die gebündelten Assets direkt
 * gelesen (wie vor der Room-Umstellung). Die Inhalte sind dieselben, die App
 * bleibt also vollständig nutzbar; Nutzerdaten gibt es noch keine, die
 * verloren gehen könnten. Erst wenn auch die Assets nicht lesbar sind, wirft
 * [load] und die Oberfläche zeigt eine Fehlermeldung.
 */
internal class SeededLibrarySource(
    private val seed: suspend () -> Unit,
    private val creatures: CreatureRepository,
    private val rules: RuleRepository,
    private val fallback: SeedSource,
    private val onDatabaseError: (Throwable) -> Unit = { Log.w(TAG, "Room nicht nutzbar, lese Assets", it) },
) : LibrarySource {
    override suspend fun load(): LibraryData =
        try {
            seed()
            val loadedCreatures = creatures.getCreatures()
            val loadedRules = rules.getRulePages()
            check(loadedCreatures.isNotEmpty() && loadedRules.isNotEmpty()) { "Datenbank ist leer" }
            LibraryData(loadedCreatures, loadedRules, DataOrigin.DATABASE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onDatabaseError(e)
            LibraryData(
                creatures = fallback.openCreatures().use { AssetLoader.parseCreatures(it) },
                rules = fallback.openRules().use { AssetLoader.parseRules(it) },
                origin = DataOrigin.ASSETS_FALLBACK,
            )
        }

    private companion object {
        const val TAG = "RuffLibrary"
    }
}
