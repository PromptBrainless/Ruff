package de.ruff.dimension.domain

import de.ruff.dimension.data.repository.SearchHits
import de.ruff.dimension.data.repository.SearchRepository

/** Volltextsuche für die Bibliothek; leere Eingaben lösen keine Abfrage aus. */
internal class SearchLibrary(
    private val repository: SearchRepository,
) {
    suspend operator fun invoke(query: String): SearchHits? = if (normalizeQuery(query).isEmpty()) null else repository.search(query)
}
