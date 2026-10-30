package de.ruff.dimension.data.repository

import com.google.gson.Gson
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.local.ContentEdit
import de.ruff.dimension.data.local.ContentEditStore
import de.ruff.dimension.domain.LibraryData

/** Verwaltet lokale Bearbeitungen getrennt von den unveränderten Seed-Inhalten. */
internal interface ContentEditorRepository {
    suspend fun apply(data: LibraryData): LibraryData

    suspend fun saveCreature(creature: Creature)

    suspend fun saveRule(rule: RulePage)

    suspend fun restoreCreature(id: String)

    suspend fun restoreRule(source: String)
}

internal class RoomContentEditorRepository(
    private val store: ContentEditStore,
    private val gson: Gson = Gson(),
) : ContentEditorRepository {
    override suspend fun apply(data: LibraryData): LibraryData {
        val edits = store.getAll()
        check(edits.all { it.contentType == CREATURE || it.contentType == RULE }) {
            "Unbekannter Typ in lokalen Inhaltsänderungen."
        }
        val creatureEdits = edits.filter { it.contentType == CREATURE }.associateBy { it.refId }
        val ruleEdits = edits.filter { it.contentType == RULE }.associateBy { it.refId }
        check(creatureEdits.keys.all { id -> data.creatures.any { it.id == id } }) {
            "Lokale Änderung verweist auf ein nicht vorhandenes Wesen."
        }
        check(ruleEdits.keys.all { source -> data.rules.any { it.source == source } }) {
            "Lokale Änderung verweist auf eine nicht vorhandene Regelseite."
        }

        return data.copy(
            creatures =
                data.creatures.map { original ->
                    creatureEdits[original.id]?.let { edit ->
                        gson.fromJson(edit.editedJson, Creature::class.java).also {
                            check(it.id == edit.refId) { "Wesen-ID der gespeicherten Bearbeitung stimmt nicht überein." }
                        }
                    } ?: original
                },
            rules =
                data.rules.map { original ->
                    ruleEdits[original.source]?.let { edit ->
                        gson.fromJson(edit.editedJson, RulePage::class.java).also {
                            check(it.source == edit.refId) { "Quellpfad der gespeicherten Bearbeitung stimmt nicht überein." }
                        }
                    } ?: original
                },
            hasLocalEdits = edits.isNotEmpty(),
        )
    }

    override suspend fun saveCreature(creature: Creature) {
        require(creature.id.isNotBlank()) { "Bearbeitete Wesen brauchen eine ID." }
        store.upsert(ContentEdit(CREATURE, creature.id, gson.toJson(creature)))
    }

    override suspend fun saveRule(rule: RulePage) {
        require(rule.source.isNotBlank()) { "Bearbeitete Regelseiten brauchen einen Quellpfad." }
        store.upsert(ContentEdit(RULE, rule.source, gson.toJson(rule)))
    }

    override suspend fun restoreCreature(id: String) {
        store.delete(CREATURE, id)
    }

    override suspend fun restoreRule(source: String) {
        store.delete(RULE, source)
    }

    private companion object {
        const val CREATURE = "creature"
        const val RULE = "rule"
    }
}
