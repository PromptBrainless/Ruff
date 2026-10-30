package de.ruff.dimension.ui.navigation

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import de.ruff.dimension.MainActivity
import de.ruff.dimension.ui.LibraryViewModel
import de.ruff.dimension.ui.LoadState

internal typealias RuffRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

internal fun RuffRule.viewModel(): LibraryViewModel = ViewModelProvider(activity)[LibraryViewModel::class.java]

/** Wartet, bis Seed/Room geladen sind, und liefert die geladenen Daten. */
internal fun RuffRule.awaitReady(): LoadState.Ready {
    waitUntil(timeoutMillis = 60_000) { viewModel().uiState.value.load is LoadState.Ready }
    waitForIdle()
    return viewModel().uiState.value.load as LoadState.Ready
}

/** Dieselbe Zurück-Aktion wie die System-Taste/-Geste. */
internal fun RuffRule.pressSystemBack() {
    runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
    waitForIdle()
}
