package de.ruff.dimension.data.repository

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonParser
import de.ruff.dimension.data.AdminAppearance
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

    suspend fun archiveCreature(creature: Creature)

    suspend fun archiveRule(rule: RulePage)

    suspend fun unarchiveCreature(creature: Creature)

    suspend fun unarchiveRule(rule: RulePage)

    suspend fun saveAppearance(appearance: AdminAppearance)

    suspend fun setAppearanceBackground(uri: Uri)

    suspend fun setAppearanceFont(uri: Uri)

    suspend fun addCreatureMedia(
        creature: Creature,
        uris: List<Uri>,
        kind: MediaKind,
    )

    suspend fun addRuleMedia(
        rule: RulePage,
        uris: List<Uri>,
        kind: MediaKind,
    )
}

internal class RoomContentEditorRepository(
    private val store: ContentEditStore,
    private val mediaImporter: MediaImporter,
    private val gson: Gson = Gson(),
) : ContentEditorRepository {
    override suspend fun apply(data: LibraryData): LibraryData {
        val edits = store.getAll()
        check(edits.all { it.contentType in setOf(CREATURE, RULE, APPEARANCE) }) {
            "Unbekannter Typ in lokalen Inhaltsänderungen."
        }
        val creatureEdits = edits.filter { it.contentType == CREATURE }.associateBy { it.refId }
        val ruleEdits = edits.filter { it.contentType == RULE }.associateBy { it.refId }
        val creatureResults =
            data.creatures.map { original ->
                val saved = creatureEdits[original.id]?.let(::creaturePayload)
                (saved?.content ?: original) to (saved?.archived == true)
            }
        val ruleResults =
            data.rules.map { original ->
                val saved = ruleEdits[original.source]?.let(::rulePayload)
                (saved?.content ?: original) to (saved?.archived == true)
            }
        val appearance =
            edits.firstOrNull { it.contentType == APPEARANCE && it.refId == GLOBAL }?.let {
                gson.fromJson(it.editedJson, AdminAppearance::class.java)
            } ?: AdminAppearance()

        return data.copy(
            creatures = creatureResults.filterNot { it.second }.map { it.first },
            rules = ruleResults.filterNot { it.second }.map { it.first },
            archivedCreatures = creatureResults.filter { it.second }.map { it.first },
            archivedRules = ruleResults.filter { it.second }.map { it.first },
            appearance = appearance,
            hasLocalEdits = creatureEdits.isNotEmpty() || ruleEdits.isNotEmpty(),
        )
    }

    override suspend fun saveCreature(creature: Creature) {
        require(creature.id.isNotBlank()) { "Bearbeitete Wesen brauchen eine ID." }
        val old = store.getAll().firstOrNull { it.contentType == CREATURE && it.refId == creature.id }
        val archived = old?.let(::creaturePayload)?.archived ?: false
        store.upsert(ContentEdit(CREATURE, creature.id, encode(creature, archived)))
    }

    override suspend fun saveRule(rule: RulePage) {
        require(rule.source.isNotBlank()) { "Bearbeitete Regelseiten brauchen einen Quellpfad." }
        val old = store.getAll().firstOrNull { it.contentType == RULE && it.refId == rule.source }
        val archived = old?.let(::rulePayload)?.archived ?: false
        store.upsert(ContentEdit(RULE, rule.source, encode(rule, archived)))
    }

    override suspend fun restoreCreature(id: String) {
        store.delete(CREATURE, id)
    }

    override suspend fun restoreRule(source: String) {
        store.delete(RULE, source)
    }

    override suspend fun archiveCreature(creature: Creature) {
        updateCreature(creature) { it.copy(archived = true) }
    }

    override suspend fun archiveRule(rule: RulePage) {
        updateRule(rule) { it.copy(archived = true) }
    }

    override suspend fun unarchiveCreature(creature: Creature) {
        updateCreature(creature) { it.copy(archived = false) }
    }

    override suspend fun unarchiveRule(rule: RulePage) {
        updateRule(rule) { it.copy(archived = false) }
    }

    override suspend fun saveAppearance(appearance: AdminAppearance) {
        store.upsert(ContentEdit(APPEARANCE, GLOBAL, gson.toJson(appearance)))
    }

    override suspend fun setAppearanceBackground(uri: Uri) {
        val imported = mediaImporter.import(uri, MediaKind.IMAGE)
        saveAppearance(currentAppearance().copy(backgroundPath = imported.path))
    }

    override suspend fun setAppearanceFont(uri: Uri) {
        val imported = mediaImporter.import(uri, MediaKind.FONT)
        saveAppearance(currentAppearance().copy(fontPath = imported.path))
    }

    private suspend fun currentAppearance(): AdminAppearance =
        store
            .getAll()
            .firstOrNull { it.contentType == APPEARANCE && it.refId == GLOBAL }
            ?.let { gson.fromJson(it.editedJson, AdminAppearance::class.java) }
            ?: AdminAppearance()

    override suspend fun addCreatureMedia(
        creature: Creature,
        uris: List<Uri>,
        kind: MediaKind,
    ) {
        require(uris.isNotEmpty()) { "Mindestens eine Mediendatei auswählen." }
        val imported = uris.map { mediaImporter.import(it, kind) }
        val presentation =
            when (kind) {
                MediaKind.BACKGROUND -> creature.presentation.copy(backgroundPath = imported.last().path)
                MediaKind.FONT -> creature.presentation.copy(fontPath = imported.last().path)
                MediaKind.IMAGE, MediaKind.VIDEO -> creature.presentation.copy(media = creature.presentation.media + imported)
            }
        saveCreature(creature.copy(presentation = presentation))
    }

    override suspend fun addRuleMedia(
        rule: RulePage,
        uris: List<Uri>,
        kind: MediaKind,
    ) {
        require(uris.isNotEmpty()) { "Mindestens eine Mediendatei auswählen." }
        val imported = uris.map { mediaImporter.import(it, kind) }
        val presentation =
            when (kind) {
                MediaKind.BACKGROUND -> rule.presentation.copy(backgroundPath = imported.last().path)
                MediaKind.FONT -> rule.presentation.copy(fontPath = imported.last().path)
                MediaKind.IMAGE, MediaKind.VIDEO -> rule.presentation.copy(media = rule.presentation.media + imported)
            }
        saveRule(rule.copy(presentation = presentation))
    }

    private suspend fun updateCreature(
        creature: Creature,
        update: (CreaturePayload) -> CreaturePayload,
    ) {
        val existing = store.getAll().firstOrNull { it.contentType == CREATURE && it.refId == creature.id }
        val current = existing?.let(::creaturePayload) ?: CreaturePayload(content = creature)
        val updated = update(current)
        store.upsert(
            ContentEdit(CREATURE, creature.id, gson.toJson(updated.copy(contentJson = gson.toJson(updated.content), content = null))),
        )
    }

    private suspend fun updateRule(
        rule: RulePage,
        update: (RulePayload) -> RulePayload,
    ) {
        val existing = store.getAll().firstOrNull { it.contentType == RULE && it.refId == rule.source }
        val current = existing?.let(::rulePayload) ?: RulePayload(content = rule)
        val updated = update(current)
        store.upsert(ContentEdit(RULE, rule.source, gson.toJson(updated.copy(contentJson = gson.toJson(updated.content), content = null))))
    }

    private fun creaturePayload(edit: ContentEdit): CreaturePayload {
        val json = JsonParser.parseString(edit.editedJson)
        return if (json.isJsonObject && json.asJsonObject.has("contentJson")) {
            gson
                .fromJson(edit.editedJson, CreaturePayload::class.java)
                .also {
                    check(it.contentJson.isNotBlank()) { "Gespeicherte Wesen-Bearbeitung ist unvollständig." }
                }.let { it.copy(content = gson.fromJson(it.contentJson, Creature::class.java)) }
        } else {
            CreaturePayload(content = gson.fromJson(edit.editedJson, Creature::class.java))
        }
    }

    private fun rulePayload(edit: ContentEdit): RulePayload {
        val json = JsonParser.parseString(edit.editedJson)
        return if (json.isJsonObject && json.asJsonObject.has("contentJson")) {
            gson
                .fromJson(edit.editedJson, RulePayload::class.java)
                .also {
                    check(it.contentJson.isNotBlank()) { "Gespeicherte Regelseiten-Bearbeitung ist unvollständig." }
                }.let { it.copy(content = gson.fromJson(it.contentJson, RulePage::class.java)) }
        } else {
            RulePayload(content = gson.fromJson(edit.editedJson, RulePage::class.java))
        }
    }

    private fun encode(
        creature: Creature,
        archived: Boolean,
    ) = gson.toJson(CreaturePayload(gson.toJson(creature), archived))

    private fun encode(
        rule: RulePage,
        archived: Boolean,
    ) = gson.toJson(RulePayload(gson.toJson(rule), archived))

    private data class CreaturePayload(
        val contentJson: String = "",
        val archived: Boolean = false,
        val content: Creature? = null,
    )

    private data class RulePayload(
        val contentJson: String = "",
        val archived: Boolean = false,
        val content: RulePage? = null,
    )

    private companion object {
        const val CREATURE = "creature"
        const val RULE = "rule"
        const val APPEARANCE = "appearance"
        const val GLOBAL = "global"
    }
}
