package de.ruff.dimension.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import de.ruff.dimension.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Navigation unter Robolectric mit der echten App (Hilt, Room, Seed). Standard-
 * Bildschirm ist ein schmales Handy (eine Spalte); [tabletShowsListAndDetailSideBySide]
 * prüft die zweispaltige Darstellung. Ersetzt keinen Test auf einem Gerät.
 */
@RunWith(RobolectricTestRunner::class)
class NavigationUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun openFirstCreature(): String {
        val ready = compose.awaitReady()
        compose.onNodeWithText("Wesen").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("creature_list").assertIsDisplayed()
        val first = ready.data.creatures.first()
        compose.onAllNodesWithText(first.name).onFirst().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("creature_detail").assertIsDisplayed()
        return first.id
    }

    @Test
    fun startsInMenuWithThreeEntries() {
        compose.awaitReady()
        compose.onNodeWithTag("menu").assertIsDisplayed()
        listOf("Wesen", "Regelwerk", "Suche").forEach { compose.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
    }

    @Test
    fun systemBackGoesDetailToListToMenuAndOnlyThenExits() {
        val id = openFirstCreature()
        assertEquals(
            id,
            compose
                .viewModel()
                .uiState.value.selectedCreature
                ?.id,
        )

        compose.pressSystemBack()
        compose.onNodeWithTag("creature_list").assertIsDisplayed()
        compose.onNodeWithTag("creature_detail").assertDoesNotExist()
        assertNull(
            compose
                .viewModel()
                .uiState.value.selectedCreature,
        )
        assertFalse(compose.activity.isFinishing)

        compose.pressSystemBack()
        compose.onNodeWithTag("menu").assertIsDisplayed()
        assertFalse(compose.activity.isFinishing)

        compose.pressSystemBack()
        assertTrue(compose.activity.isFinishing)
    }

    @Test
    fun topBarBackBehavesLikeSystemBack() {
        openFirstCreature()
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("creature_list").assertIsDisplayed()
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("menu").assertIsDisplayed()
    }

    @Test
    fun opensRulePage() {
        val ready = compose.awaitReady()
        compose.onNodeWithText("Regelwerk").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("rule_list").assertIsDisplayed()
        val rule = ready.data.rules.first()
        compose.onAllNodesWithText(rule.title).onFirst().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("rule_detail").assertIsDisplayed()
        compose.onNodeWithText("Regelwerk in Arbeit", substring = true).assertExists()
        assertEquals(
            rule.source,
            compose
                .viewModel()
                .uiState.value.selectedRule
                ?.source,
        )

        compose.pressSystemBack()
        compose.onNodeWithTag("rule_list").assertIsDisplayed()
    }

    @Test
    fun combinedSearchFindsCreaturesAndRulesAndBackReturnsToSearch() {
        compose.awaitReady()
        compose.onNodeWithText("Suche").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("search_field").performTextInput("ritter")
        compose.waitForIdle()
        compose.onNodeWithTag("search_results").assertIsDisplayed()
        compose.onNodeWithText("Wesen ·", substring = true).assertExists()
        compose.onNodeWithText("Regeln ·", substring = true).assertExists()
        compose.onNodeWithText("Morgan le Fay").assertExists()

        compose.onNodeWithText("Morgan le Fay").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("creature_detail").assertIsDisplayed()
        compose.pressSystemBack()
        compose.onNodeWithTag("search_results").assertIsDisplayed()

        // Regelseite mit Sonderzeichen im Pfad (Leerzeichen, Schrägstrich, Emoji) als Route.
        compose.onNodeWithTag("search_results").performScrollToNode(hasText("Kampfsystem Ruff v2"))
        compose.onNodeWithText("Kampfsystem Ruff v2").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("rule_detail").assertIsDisplayed()
        assertEquals(
            "Kampfsystem Ruff v2",
            compose
                .viewModel()
                .uiState.value.selectedRule
                ?.title,
        )
        compose.pressSystemBack()
        compose.onNodeWithTag("search_results").assertIsDisplayed()
    }

    @Test
    fun editorIsReachableAndBackCancelsEditing() {
        openFirstCreature()
        assertTrue(
            compose
                .viewModel()
                .uiState.value.canEdit,
        )
        compose.onNodeWithText("Inhalt bearbeiten").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Wesen bearbeiten").assertExists()
        assertTrue(
            compose
                .viewModel()
                .uiState.value.editing,
        )

        compose.pressSystemBack()
        assertFalse(
            compose
                .viewModel()
                .uiState.value.editing,
        )
        compose.onNodeWithTag("creature_detail").assertIsDisplayed()
    }

    @Test
    fun detailSurvivesRecreation() {
        val id = openFirstCreature()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("creature_detail").assertIsDisplayed()
        assertEquals(
            id,
            compose
                .viewModel()
                .uiState.value.selectedCreature
                ?.id,
        )
        // Nach dem Neuaufbau stimmt auch der Zurück-Stapel noch.
        compose.pressSystemBack()
        compose.onNodeWithTag("creature_list").assertIsDisplayed()
        compose.pressSystemBack()
        compose.onNodeWithTag("menu").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun tabletShowsListAndDetailSideBySide() {
        openFirstCreature()
        compose.onNodeWithTag("creature_list").assertIsDisplayed()
        compose.pressSystemBack()
        compose.onNodeWithTag("creature_list").assertIsDisplayed()
        assertNull(
            compose
                .viewModel()
                .uiState.value.selectedCreature,
        )
        compose.pressSystemBack()
        compose.onNodeWithTag("menu").assertIsDisplayed()
    }
}
