package de.ruff.dimension.data.repository

import android.net.Uri
import de.ruff.dimension.data.AdminAppearance
import de.ruff.dimension.data.ContentPresentation
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.MediaAttachment
import de.ruff.dimension.data.RuleBlock
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.local.ContentEdit
import de.ruff.dimension.data.local.ContentEditStore
import de.ruff.dimension.domain.DataOrigin
import de.ruff.dimension.domain.LibraryData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentEditorRepositoryTest {
    private val store = InMemoryContentEditStore()
    private val repository =
        RoomContentEditorRepository(
            store,
            MediaImporter { uri, kind ->
                val type =
                    when (kind) {
                        MediaKind.IMAGE, MediaKind.BACKGROUND -> "image/png"
                        MediaKind.VIDEO -> "video/mp4"
                        MediaKind.FONT -> "font/ttf"
                    }
                MediaAttachment(uri.toString(), type, "test.png")
            },
        )
    private val originalCreature = Creature(id = "creature-1", name = "Original")
    private val originalRule = RulePage(title = "Original", source = "rules/original", blocks = listOf(RuleBlock("p", "Text")))
    private val seedData = LibraryData(listOf(originalCreature), listOf(originalRule), DataOrigin.DATABASE)

    @Test
    fun savesAndAppliesLocalChangesWithoutChangingSeedModels() =
        runBlocking {
            repository.saveCreature(originalCreature.copy(name = "Bearbeitet"))
            repository.saveRule(originalRule.copy(title = "Bearbeitet"))

            val edited = repository.apply(seedData)

            assertEquals("Original", originalCreature.name)
            assertEquals("Original", originalRule.title)
            assertTrue(edited.hasLocalEdits)
            assertEquals("Bearbeitet", edited.creatures.single().name)
            assertEquals("Bearbeitet", edited.rules.single().title)
        }

    @Test
    fun restoringRemovesOverridesAndShowsSeedModelsAgain() =
        runBlocking {
            repository.saveCreature(originalCreature.copy(name = "Bearbeitet"))
            repository.saveRule(originalRule.copy(title = "Bearbeitet"))

            repository.restoreCreature(originalCreature.id)
            repository.restoreRule(originalRule.source)

            val restored = repository.apply(seedData)

            assertEquals(seedData, restored)
            assertFalse(restored.hasLocalEdits)
        }

    @Test
    fun archivesAndRestoresWithoutDeletingSeedContent() =
        runBlocking {
            repository.archiveCreature(originalCreature)
            repository.archiveRule(originalRule)

            val archived = repository.apply(seedData)

            assertTrue(archived.creatures.isEmpty())
            assertTrue(archived.rules.isEmpty())
            assertEquals(listOf(originalCreature), archived.archivedCreatures)
            assertEquals(listOf(originalRule), archived.archivedRules)

            repository.unarchiveCreature(originalCreature)
            repository.unarchiveRule(originalRule)

            assertEquals(seedData, repository.apply(seedData))
        }

    @Test
    fun addsMoreThanOneImageAndKeepsAttachmentsInTheLocalOverride() =
        runBlocking {
            repository.addCreatureMedia(originalCreature, listOf(Uri.parse("content://one")), MediaKind.IMAGE)
            val onceEdited = repository.apply(seedData).creatures.single()
            repository.addCreatureMedia(
                onceEdited,
                listOf(Uri.parse("content://two"), Uri.parse("content://three")),
                MediaKind.IMAGE,
            )

            val saved = repository.apply(seedData).creatures.single()

            assertEquals(3, saved.presentation.media.size)
        }

    @Test
    fun perContentBackgroundAndFontAreSavedWithoutReplacingMedia() =
        runBlocking {
            val edited =
                originalCreature.copy(
                    presentation = ContentPresentation(media = listOf(MediaAttachment("image", "image/png", "one.png"))),
                )
            repository.addCreatureMedia(edited, listOf(Uri.parse("content://background")), MediaKind.BACKGROUND)
            val withBackground = repository.apply(seedData).creatures.single()
            repository.addCreatureMedia(withBackground, listOf(Uri.parse("content://font")), MediaKind.FONT)

            val saved = repository.apply(seedData).creatures.single()

            assertEquals("content://background", saved.presentation.backgroundPath)
            assertEquals("content://font", saved.presentation.fontPath)
            assertEquals(listOf(MediaAttachment("image", "image/png", "one.png")), saved.presentation.media)
        }

    @Test
    fun savesGlobalAppearanceSeparatelyFromSeedContent() =
        runBlocking {
            val appearance = AdminAppearance(backgroundPath = "/private/background.png", accentColor = "#123456")
            repository.saveAppearance(appearance)

            val saved = repository.apply(seedData)

            assertEquals(appearance, saved.appearance)
            assertEquals(seedData.creatures, saved.creatures)
            assertEquals(seedData.rules, saved.rules)
        }

    @Test
    fun archivingKeepsTheEditedContentAvailableForRestore() =
        runBlocking {
            val edited = originalCreature.copy(name = "Lokal angepasst")
            repository.saveCreature(edited)
            repository.archiveCreature(originalCreature)

            val archived = repository.apply(seedData)

            assertEquals(listOf("Lokal angepasst"), archived.archivedCreatures.map { it.name })
            repository.unarchiveCreature(originalCreature)
            assertEquals(
                "Lokal angepasst",
                repository
                    .apply(seedData)
                    .creatures
                    .single()
                    .name,
            )
        }

    private class InMemoryContentEditStore : ContentEditStore {
        private val edits = mutableMapOf<Pair<String, String>, ContentEdit>()

        override suspend fun getAll(): List<ContentEdit> = edits.values.toList()

        override suspend fun upsert(edit: ContentEdit) {
            edits[edit.contentType to edit.refId] = edit
        }

        override suspend fun delete(
            contentType: String,
            refId: String,
        ) {
            edits.remove(contentType to refId)
        }
    }
}
