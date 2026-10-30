package de.ruff.dimension.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import de.ruff.dimension.MainActivity
import de.ruff.dimension.ui.common.favoriteHeartTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Herz-Toggle und Filter „Nur Favoriten“ mit der echten App (Hilt, Room, Seed) unter Robolectric. */
@RunWith(RobolectricTestRunner::class)
class FavoritesUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun openCreatureList() {
        compose.awaitReady()
        compose.onNodeWithText("Wesen").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("creature_list").assertIsDisplayed()
    }

    private fun awaitFavorite(
        id: String,
        expected: Boolean,
    ) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose
                .viewModel()
                .uiState.value
                .isFavorite(id) == expected
        }
        compose.waitForIdle()
    }

    @Test
    fun heartOnCardTogglesFavoriteAndBack() {
        openCreatureList()
        val first =
            compose
                .viewModel()
                .uiState.value.creatures
                .first()
        val heart = compose.onNodeWithTag(favoriteHeartTag(first.id, "liste"))

        heart.assertIsOff()
        heart.performClick()
        awaitFavorite(first.id, true)
        heart.assertIsOn()
        // Tippen aufs Herz öffnet das Wesen nicht.
        compose.onNodeWithTag("creature_detail").assertDoesNotExist()

        heart.performClick()
        awaitFavorite(first.id, false)
        heart.assertIsOff()
    }

    @Test
    fun heartInDetailMatchesCardState() {
        openCreatureList()
        val first =
            compose
                .viewModel()
                .uiState.value.creatures
                .first()
        compose.onAllNodesWithText(first.name).onFirst().performClick()
        compose.waitForIdle()
        val heart = compose.onNodeWithTag(favoriteHeartTag(first.id, "detail"))
        heart.performScrollTo().assertIsOff()

        heart.performClick()
        awaitFavorite(first.id, true)
        heart.assertIsOn()

        compose.pressSystemBack()
        compose.onNodeWithTag(favoriteHeartTag(first.id, "liste")).assertIsOn()
    }

    @Test
    fun favoritesOnlyFilterShowsEmptyStateThenOnlyFavorites() {
        openCreatureList()
        val state = compose.viewModel().uiState.value
        val first = state.creatures.first()
        val third = state.creatures[2]

        compose.onNodeWithTag("filter_nur_favoriten").performClick()
        compose.waitForIdle()
        assertTrue(
            compose
                .viewModel()
                .uiState.value.favoritesOnly,
        )
        compose.onNodeWithText("Noch keine Favoriten", substring = true).assertIsDisplayed()

        // Zurück zu allen, zwei Wesen favorisieren, dann filtern.
        compose.onNodeWithTag("filter_nur_favoriten").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(favoriteHeartTag(first.id, "liste")).performClick()
        awaitFavorite(first.id, true)
        compose.onNodeWithTag("creature_list").performScrollToNode(hasTestTag(favoriteHeartTag(third.id, "liste")))
        compose.onNodeWithTag(favoriteHeartTag(third.id, "liste")).performClick()
        awaitFavorite(third.id, true)

        compose.onNodeWithTag("filter_nur_favoriten").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("2 von ${state.creatures.size} Wesen").assertIsDisplayed()
        compose.onNodeWithTag(favoriteHeartTag(first.id, "liste")).assertIsOn()
        compose.onNodeWithTag(favoriteHeartTag(third.id, "liste")).assertIsOn()
        compose.onNodeWithTag(favoriteHeartTag(state.creatures[1].id, "liste")).assertDoesNotExist()

        // Entfernen im Filter lässt das Wesen aus der Liste verschwinden.
        compose.onNodeWithTag(favoriteHeartTag(first.id, "liste")).performClick()
        awaitFavorite(first.id, false)
        compose.onNodeWithTag(favoriteHeartTag(first.id, "liste")).assertDoesNotExist()
        assertEquals(
            setOf(third.id),
            compose
                .viewModel()
                .uiState.value.favoriteIds,
        )
        assertFalse(
            compose
                .viewModel()
                .uiState.value
                .isFavorite(first.id),
        )
    }
}
