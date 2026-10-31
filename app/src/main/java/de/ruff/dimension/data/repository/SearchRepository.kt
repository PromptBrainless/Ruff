package de.ruff.dimension.data.repository

import android.util.Log
import androidx.room.useReaderConnection
import de.ruff.dimension.data.local.FtsQuery
import de.ruff.dimension.data.local.ProfileSearch
import de.ruff.dimension.data.local.RuffDatabase
import de.ruff.dimension.data.local.SearchIndex
import kotlinx.coroutines.CancellationException

/** Volltexttreffer zu einer Suchanfrage, je Typ nach Relevanz (bm25) sortiert. */
internal data class SearchHits(
    val query: String,
    val creatureIds: List<String>,
    val ruleIds: List<String>,
)

/** Volltextsuche über Wesen und Regelseiten. */
internal fun interface SearchRepository {
    /** Liefert `null`, wenn die Volltextsuche nicht verfügbar ist; dann gilt die Teilstring-Suche. */
    suspend fun search(query: String): SearchHits?
}

internal class RoomSearchRepository(
    private val database: RuffDatabase,
) : SearchRepository {
    override suspend fun search(query: String): SearchHits? {
        val match = FtsQuery.build(query) ?: return SearchHits(query, emptyList(), emptyList())
        return try {
            database.useReaderConnection { connection ->
                connection.usePrepared(SearchIndex.SEARCH_SQL) { statement ->
                    statement.bindText(1, match)
                    // LinkedHashSet: Ein Wesen kann über Inhalt und Akte zweimal treffen; der bessere Rang zählt.
                    val creatures = LinkedHashSet<String>()
                    val rules = mutableListOf<String>()
                    while (statement.step()) {
                        val refId = statement.getText(1)
                        when (statement.getText(0)) {
                            SearchIndex.TYPE_CREATURE, ProfileSearch.TYPE -> creatures += refId
                            SearchIndex.TYPE_RULE -> rules += refId
                        }
                    }
                    SearchHits(query, creatures.toList(), rules)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Volltextsuche nicht verfügbar, nutze Teilstring-Suche", e)
            null
        }
    }

    private companion object {
        const val TAG = "RuffSearch"
    }
}
