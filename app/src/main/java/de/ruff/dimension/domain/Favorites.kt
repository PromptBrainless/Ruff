package de.ruff.dimension.domain

import de.ruff.dimension.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

/** Use Case: Favoritenstatus eines Wesens umschalten; liefert den neuen Status. */
internal class ToggleFavorite(
    private val repository: FavoritesRepository,
) {
    suspend operator fun invoke(creatureId: String): Boolean = repository.toggle(creatureId)
}

/** Use Case: aktive Favoriten beobachten (Wesen-IDs). */
internal class ObserveFavorites(
    private val repository: FavoritesRepository,
) {
    operator fun invoke(): Flow<Set<String>> = repository.observeFavoriteIds()
}
