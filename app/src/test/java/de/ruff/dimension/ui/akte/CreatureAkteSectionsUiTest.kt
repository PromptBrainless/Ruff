package de.ruff.dimension.ui.akte

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.ruff.dimension.domain.akte.Allergie
import de.ruff.dimension.domain.akte.AllergieArt
import de.ruff.dimension.domain.akte.Angabe
import de.ruff.dimension.domain.akte.Blutdruck
import de.ruff.dimension.domain.akte.CreatureProfile
import de.ruff.dimension.domain.akte.GanzzahlSpanne
import de.ruff.dimension.domain.akte.Herkunft
import de.ruff.dimension.domain.akte.Schwere
import de.ruff.dimension.domain.akte.Spanne
import de.ruff.dimension.ui.common.LocalRuffMotion
import de.ruff.dimension.ui.common.RuffMotion
import de.ruff.dimension.ui.theme.RuffTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Akten-Abschnitte und Editor in der Detailansicht (reduzierter Modus: keine Animationen). */
@RunWith(RobolectricTestRunner::class)
class CreatureAkteSectionsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val draugr =
        CreatureProfile(
            creatureId = "V2-078",
            groesseM = Angabe(Spanne(2.0, 2.5), "Katalog-Spielwert"),
            pulsBpm = Angabe(GanzzahlSpanne(0, 0), "Untoter"),
            blutdruck = Angabe(Blutdruck(hinweis = "kein Kreislauf"), "Untoter"),
            allergien = Angabe(listOf(Allergie("Eisen", AllergieArt.UNVERTRAEGLICHKEIT, Schwere.SCHWER, "gebannt")), "Schwächen"),
            iq = Angabe(GanzzahlSpanne(70, 95), "Lore", Herkunft.MANUELL),
            wahnhaft = Angabe(true, "Fixierung auf das Grab"),
        )

    private val edits = mutableListOf<String>()
    private val confirms = mutableListOf<String>()
    private val saves = mutableListOf<Map<String, String>>()

    private fun binding(state: AkteUiState) =
        AkteBinding(
            state = state,
            onEdit = { edits += it },
            onCancel = {},
            onSave = { _, e -> saves += e },
            onConfirm = { confirms += it },
            onDismissHinweis = {},
        )

    private fun show(state: AkteUiState) {
        compose.setContent {
            CompositionLocalProvider(LocalRuffMotion provides RuffMotion(animatorScale = 0f)) {
                RuffTheme {
                    CreatureAkteSections("V2-078", "Draugr", canEdit = true, binding = binding(state))
                }
            }
        }
    }

    @Test
    fun abschnitteWerteUndHerkunft() {
        show(AkteUiState(profiles = mapOf("V2-078" to draugr)))
        compose.onNodeWithText("Körperliche Werte").assertExists()
        compose.onNodeWithText("Verfassung & Allergien").assertExists()
        compose.onNodeWithText("Kognition").assertExists()
        compose.onNodeWithText("kein Herzschlag").assertExists()
        compose.onNodeWithText("kein Kreislauf").assertExists()
        compose.onNodeWithText("• Eisen – Unverträglichkeit, schwer").assertExists()
        compose.onNodeWithText("ja").assertExists()
        assertEquals(5, compose.onAllNodesWithContentDescription("Herkunft: abgeleitet").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithContentDescription("Herkunft: manuell").fetchSemanticsNodes().size)
    }

    @Test
    fun tippZeigtBegruendung() {
        show(AkteUiState(profiles = mapOf("V2-078" to draugr)))
        compose.onNodeWithText("Begründung: Fixierung auf das Grab").assertDoesNotExist()
        compose.onNodeWithTag("akte_feld_WAHNHAFT").performClick()
        compose.onNodeWithText("Begründung: Fixierung auf das Grab").assertIsDisplayed()
    }

    @Test
    fun bestaetigenUndBearbeiten() {
        show(AkteUiState(profiles = mapOf("V2-078" to draugr)))
        compose.onNodeWithText("Werte bestätigen (5)").performClick()
        compose.onNodeWithTag("akte_bearbeiten").performClick()
        assertEquals(listOf("V2-078"), confirms)
        assertEquals(listOf("V2-078"), edits)
    }

    @Test
    fun ohneAkteLaesstSichEineAnlegen() {
        show(AkteUiState())
        compose.onNodeWithText("Für Draugr gibt es noch keine Akte.").assertExists()
        compose.onNodeWithText("Akte anlegen").performClick()
        assertEquals(listOf("V2-078"), edits)
    }

    @Test
    fun editorSpeichertEntwurf() {
        compose.setContent {
            CompositionLocalProvider(LocalRuffMotion provides RuffMotion(animatorScale = 0f)) {
                RuffTheme {
                    CreatureProfileEditor(
                        creatureId = "V2-078",
                        creatureName = "Draugr",
                        profile = draugr,
                        feldFehler = mapOf("iq.min" to "Keine gültige ganze Zahl"),
                        error = null,
                        busy = false,
                        onCancel = {},
                        onSave = { saves += it },
                    )
                }
            }
        }
        compose.onNodeWithText("Keine gültige ganze Zahl").assertExists()
        compose.onNodeWithTag("akte_speichern").performClick()
        assertEquals("2", saves.single()["groesse.min"])
        assertEquals("ja", saves.single()["wahnhaft.wert"])
        assertEquals("kein Kreislauf", saves.single()["blutdruck.hinweis"])
    }
}
