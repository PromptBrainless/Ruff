package de.ruff.dimension.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.domain.normalizeQuery
import de.ruff.dimension.domain.searchCreatures
import de.ruff.dimension.domain.searchRules
import de.ruff.dimension.ui.LibraryUiState
import de.ruff.dimension.ui.common.EmptyState
import de.ruff.dimension.ui.common.LoadGate
import de.ruff.dimension.ui.common.RuffTopBar
import de.ruff.dimension.ui.common.SearchField
import de.ruff.dimension.ui.common.SectionHeader
import de.ruff.dimension.ui.common.safeHorizontalPadding
import de.ruff.dimension.ui.creature.CreatureCard
import de.ruff.dimension.ui.rule.RuleCard

/**
 * Gemeinsame Suche über Wesen und Regeln. Nutzt die bestehende Suchlogik
 * (FTS-Treffer zuerst, danach Teilstring-Treffer); der Machtstufen-Filter der
 * Wesen-Liste gilt hier bewusst nicht.
 */
@Composable
internal fun SearchScreen(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onCreatureClick: (Creature) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRuleClick: (RulePage) -> Unit,
) {
    Column(Modifier.fillMaxSize().safeHorizontalPadding()) {
        RuffTopBar(title = "Suche", showBack = true)
        LoadGate(state, onRetry) {
            val creatures =
                remember(state.creatures, state.query, state.searchHits) {
                    searchCreatures(state.creatures, state.query, null, state.searchHits)
                }
            val rules = remember(state.rules, state.query, state.searchHits) { searchRules(state.rules, state.query, state.searchHits) }
            Column(Modifier.padding(horizontal = 16.dp)) {
                SearchField(query = state.query, onQueryChange = onQueryChange, label = "Wesen und Regeln durchsuchen")
            }
            if (normalizeQuery(state.query).isEmpty()) {
                EmptyState("Suchbegriff eingeben, um Wesen und Regelseiten gemeinsam zu durchsuchen.", Modifier.padding(16.dp))
            } else if (creatures.isEmpty() && rules.isEmpty()) {
                EmptyState("Nichts gefunden.", Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("search_results"),
                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        ),
                ) {
                    if (creatures.isNotEmpty()) {
                        item(key = "h_wesen") { SectionHeader("Wesen · ${creatures.size}") }
                        items(creatures, key = { "c_" + it.id.ifBlank { it.name } }) { creature ->
                            CreatureCard(
                                creature = creature,
                                onClick = { onCreatureClick(creature) },
                                favorite = if (state.favoritesAvailable) state.isFavorite(creature.id) else null,
                                onToggleFavorite = { onToggleFavorite(creature.id) },
                                favoritePlace = "suche",
                            )
                        }
                    }
                    if (rules.isNotEmpty()) {
                        item(key = "h_regeln") { SectionHeader("Regeln · ${rules.size}", Modifier.padding(top = 8.dp)) }
                        items(rules, key = { "r_" + it.source }) { rule ->
                            RuleCard(rule = rule, onClick = { onRuleClick(rule) })
                        }
                    }
                }
            }
        }
    }
}
