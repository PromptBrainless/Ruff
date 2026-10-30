package de.ruff.dimension.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import de.ruff.dimension.data.local.FavoriteEntity
import de.ruff.dimension.data.local.RuffDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Favoriten des Nutzers (Wesen-IDs). Unabhängig vom Seed, übersteht Neustart und Re-Import. */
internal interface FavoritesRepository {
    /** Aktive Favoriten in Reihenfolge des Favorisierens. */
    fun observeFavoriteIds(): Flow<Set<String>>

    suspend fun isFavorite(creatureId: String): Boolean

    /**
     * Setzt den Favoritenstatus. Idempotent: Ein Wiederholen desselben Werts
     * ändert weder Zeitstempel noch Revision. Gibt den neuen Status zurück.
     */
    suspend fun setFavorite(
        creatureId: String,
        favorite: Boolean,
    ): Boolean

    /** Kehrt den Status atomar um und gibt den neuen Status zurück. */
    suspend fun toggle(creatureId: String): Boolean
}

internal class RoomFavoritesRepository(
    private val database: RuffDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : FavoritesRepository {
    private val dao get() = database.favoriteDao()

    override fun observeFavoriteIds(): Flow<Set<String>> =
        dao
            .observeActiveCreatureIds()
            .map { it.toCollection(LinkedHashSet()) }
            .distinctUntilChanged()

    override suspend fun isFavorite(creatureId: String): Boolean = dao.getByCreatureId(creatureId)?.active == true

    override suspend fun setFavorite(
        creatureId: String,
        favorite: Boolean,
    ): Boolean = write(creatureId) { favorite }

    override suspend fun toggle(creatureId: String): Boolean = write(creatureId) { current -> current?.active != true }

    /**
     * Liest den aktuellen Eintrag, entscheidet den Zielstatus und schreibt – alles in
     * einer IMMEDIATE-Transaktion, damit schnelle Doppeltipps nichts verlieren.
     */
    private suspend fun write(
        creatureId: String,
        target: (FavoriteEntity?) -> Boolean,
    ): Boolean {
        require(creatureId.isNotBlank()) { "creatureId fehlt" }
        return database.useWriterConnection { connection ->
            connection.immediateTransaction {
                val current = dao.getByCreatureId(creatureId)
                val favorite = target(current)
                apply(current, creatureId, favorite)
                favorite
            }
        }
    }

    private suspend fun apply(
        current: FavoriteEntity?,
        creatureId: String,
        favorite: Boolean,
    ) {
        val now = clock()
        when {
            current == null && favorite ->
                dao.insert(
                    FavoriteEntity(
                        id = newId(),
                        creatureId = creatureId,
                        createdAt = now,
                        updatedAt = now,
                        revision = 1,
                    ),
                )
            current == null -> Unit
            current.active == favorite -> Unit
            favorite -> dao.update(current.copy(deletedAt = null, updatedAt = now, revision = current.revision + 1))
            else -> dao.update(current.copy(deletedAt = now, updatedAt = now, revision = current.revision + 1))
        }
    }
}
