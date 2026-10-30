package de.ruff.dimension.ui.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.ruff.dimension.ui.LibraryTab
import de.ruff.dimension.ui.LibraryUiState
import de.ruff.dimension.ui.LibraryViewModel
import de.ruff.dimension.ui.LoadState
import de.ruff.dimension.ui.about.LicensesScreen
import de.ruff.dimension.ui.common.LoadGate
import de.ruff.dimension.ui.common.RuffTopBar
import de.ruff.dimension.ui.common.safeHorizontalPadding
import de.ruff.dimension.ui.creature.CreatureDetailPane
import de.ruff.dimension.ui.creature.CreatureListPane
import de.ruff.dimension.ui.menu.MenuScreen
import de.ruff.dimension.ui.rule.RuleDetailPane
import de.ruff.dimension.ui.rule.RuleListPane
import de.ruff.dimension.ui.search.SearchScreen

/** String-Routen der App (bewusst ohne typsichere Serialization-Routen, kein zusätzliches Plugin). */
internal object Routes {
    const val MENU = "menu"
    const val CREATURES = "wesen"
    const val CREATURE = "wesen/{id}"
    const val RULES = "regeln"
    const val RULE = "regeln/{source}"
    const val SEARCH = "suche"
    const val LICENSES = "lizenzen"

    fun creature(id: String) = "wesen/${Uri.encode(id)}"

    fun rule(source: String) = "regeln/${Uri.encode(source)}"
}

/**
 * Navigation: Menü → Wesen | Regelwerk | Suche | Lizenzen. Wesen und Regelwerk sind
 * Liste/Detail (Handy nacheinander, ab mittlerer Breite nebeneinander). Die
 * System-Zurück-Taste führt Detail → Liste → Menü; nur im Menü schließt sie die App.
 */
@Composable
internal fun RuffNavHost(
    state: LibraryUiState,
    viewModel: LibraryViewModel,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MENU,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        composable(Routes.MENU) {
            MenuScreen(
                load = state.load,
                onCreatures = {
                    viewModel.selectTab(LibraryTab.CREATURES)
                    navController.navigate(Routes.CREATURES) { launchSingleTop = true }
                },
                onRules = {
                    viewModel.selectTab(LibraryTab.RULES)
                    navController.navigate(Routes.RULES) { launchSingleTop = true }
                },
                onSearch = { navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
                onLicenses = { navController.navigate(Routes.LICENSES) { launchSingleTop = true } },
                onRetry = viewModel::reload,
            )
        }
        composable(Routes.CREATURES) { CreatureSection(state, viewModel) }
        composable(Routes.RULES) { RuleSection(state, viewModel) }
        composable(Routes.CREATURE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            CreatureStandalone(id, state, viewModel)
        }
        composable(Routes.RULE, arguments = listOf(navArgument("source") { type = NavType.StringType })) { entry ->
            val source = entry.arguments?.getString("source").orEmpty()
            RuleStandalone(source, state, viewModel)
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                state = state,
                onQueryChange = viewModel::setQuery,
                onRetry = viewModel::reload,
                onCreatureClick = { navController.navigate(Routes.creature(it.id)) },
                onRuleClick = { navController.navigate(Routes.rule(it.source)) },
            )
        }
        composable(Routes.LICENSES) { LicensesScreen() }
    }
}

/**
 * Zurück löscht zuerst die Auswahl (auch zweispaltig auf dem Tablet, wo Liste und
 * Detail gleichzeitig sichtbar sind) und führt erst danach ins Menü.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val BackBehavior = BackNavigationBehavior.PopUntilContentChange

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun CreatureSection(
    state: LibraryUiState,
    viewModel: LibraryViewModel,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val selectedId = navigator.currentDestination?.content
    LaunchedEffect(selectedId) { viewModel.selectCreatureId(selectedId) }
    BackHandler(navigator.canNavigateBack(BackBehavior)) { navigator.navigateBack(BackBehavior) }
    // Zuletzt registriert, hat also Vorrang: Zurück bricht zuerst eine Bearbeitung ab.
    BackHandler(state.editing) { viewModel.setEditing(false) }

    Column(Modifier.fillMaxSize().safeHorizontalPadding()) {
        if (state.load !is LoadState.Ready) {
            RuffTopBar(title = "Wesen", showBack = true)
            LoadGate(state, viewModel::reload) {}
        } else {
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                listPane = {
                    AnimatedPane {
                        CreatureListPane(
                            state = state,
                            selectedId = selectedId,
                            onQueryChange = viewModel::setQuery,
                            onPowerFilterChange = viewModel::setPowerFilter,
                            onCreatureClick = { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, it.id) },
                        )
                    }
                },
                detailPane = {
                    AnimatedPane {
                        CreatureDetailPane(
                            creature = selectedId?.let { id -> state.creatures.firstOrNull { it.id == id } },
                            state = state,
                            showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden,
                            onEdit = { viewModel.setEditing(true) },
                            onCancelEdit = { viewModel.setEditing(false) },
                            onSave = viewModel::saveCreature,
                            onRestore = viewModel::restoreCreature,
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun RuleSection(
    state: LibraryUiState,
    viewModel: LibraryViewModel,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val selectedSource = navigator.currentDestination?.content
    LaunchedEffect(selectedSource) { viewModel.selectRuleSource(selectedSource) }
    BackHandler(navigator.canNavigateBack(BackBehavior)) { navigator.navigateBack(BackBehavior) }
    BackHandler(state.editing) { viewModel.setEditing(false) }

    Column(Modifier.fillMaxSize().safeHorizontalPadding()) {
        if (state.load !is LoadState.Ready) {
            RuffTopBar(title = "Regelwerk", showBack = true)
            LoadGate(state, viewModel::reload) {}
        } else {
            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                listPane = {
                    AnimatedPane {
                        RuleListPane(
                            state = state,
                            selectedSource = selectedSource,
                            onQueryChange = viewModel::setQuery,
                            onRuleClick = { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, it.source) },
                        )
                    }
                },
                detailPane = {
                    AnimatedPane {
                        RuleDetailPane(
                            rule = selectedSource?.let { source -> state.rules.firstOrNull { it.source == source } },
                            state = state,
                            showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden,
                            onEdit = { viewModel.setEditing(true) },
                            onCancelEdit = { viewModel.setEditing(false) },
                            onSave = viewModel::saveRule,
                            onRestore = viewModel::restoreRule,
                        )
                    }
                },
            )
        }
    }
}

/** Wesen-Detail als eigener Bildschirm (aus der Suche); Zurück führt zur Suche. */
@Composable
private fun CreatureStandalone(
    id: String,
    state: LibraryUiState,
    viewModel: LibraryViewModel,
) {
    LaunchedEffect(id) { viewModel.selectCreatureId(id) }
    BackHandler(state.editing) { viewModel.setEditing(false) }
    Column(Modifier.fillMaxSize().safeHorizontalPadding()) {
        if (state.load !is LoadState.Ready) {
            RuffTopBar(title = "Wesen", showBack = true)
            LoadGate(state, viewModel::reload) {}
        } else {
            CreatureDetailPane(
                creature = state.creatures.firstOrNull { it.id == id },
                state = state,
                showBack = true,
                onEdit = { viewModel.setEditing(true) },
                onCancelEdit = { viewModel.setEditing(false) },
                onSave = viewModel::saveCreature,
                onRestore = viewModel::restoreCreature,
            )
        }
    }
}

/** Regelseite als eigener Bildschirm (aus der Suche); Zurück führt zur Suche. */
@Composable
private fun RuleStandalone(
    source: String,
    state: LibraryUiState,
    viewModel: LibraryViewModel,
) {
    LaunchedEffect(source) { viewModel.selectRuleSource(source) }
    BackHandler(state.editing) { viewModel.setEditing(false) }
    Column(Modifier.fillMaxSize().safeHorizontalPadding()) {
        if (state.load !is LoadState.Ready) {
            RuffTopBar(title = "Regelwerk", showBack = true)
            LoadGate(state, viewModel::reload) {}
        } else {
            RuleDetailPane(
                rule = state.rules.firstOrNull { it.source == source },
                state = state,
                showBack = true,
                onEdit = { viewModel.setEditing(true) },
                onCancelEdit = { viewModel.setEditing(false) },
                onSave = viewModel::saveRule,
                onRestore = viewModel::restoreRule,
            )
        }
    }
}
