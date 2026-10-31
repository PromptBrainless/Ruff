package de.ruff.dimension.ui.rule

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.ruff.dimension.data.RuleBlock
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.repository.MediaKind
import de.ruff.dimension.domain.searchRules
import de.ruff.dimension.ui.LibraryUiState
import de.ruff.dimension.ui.common.ChainDivider
import de.ruff.dimension.ui.common.ContentPresentationFrame
import de.ruff.dimension.ui.common.EmptyState
import de.ruff.dimension.ui.common.MediaGallery
import de.ruff.dimension.ui.common.RuffTopBar
import de.ruff.dimension.ui.common.SearchField
import de.ruff.dimension.ui.common.SectionHeader
import de.ruff.dimension.ui.common.emberButtonColors
import de.ruff.dimension.ui.editor.RuleEditor

/** Regelwerk-Übersicht: Suchfeld und Liste der Regelseiten (Logik unverändert). */
@Composable
internal fun RuleListPane(
    state: LibraryUiState,
    selectedSource: String?,
    onQueryChange: (String) -> Unit,
    onRuleClick: (RulePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = remember(state.rules, state.query, state.searchHits) { searchRules(state.rules, state.query, state.searchHits) }
    Column(modifier = modifier.fillMaxSize()) {
        RuffTopBar(title = "Regelwerk", showBack = true)
        Column(Modifier.padding(horizontal = 16.dp)) {
            SearchField(query = state.query, onQueryChange = onQueryChange, label = "Regeln suchen")
            Spacer(Modifier.height(8.dp))
            Text(
                "${filtered.size} Regel-Dokumente",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (filtered.isEmpty()) {
            EmptyState("Keine Regel-Dokumente gefunden.", Modifier.padding(horizontal = 16.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("rule_list"),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
            ) {
                items(filtered, key = { it.source }) { rule ->
                    RuleCard(rule = rule, selected = rule.source == selectedSource, onClick = { onRuleClick(rule) })
                }
            }
        }
    }
}

/** Karte einer Regelseite mit Titel und erstem Absatz als Zusammenfassung. */
@Composable
internal fun RuleCard(
    rule: RulePage,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val summary =
        rule.blocks
            .firstOrNull { it.type == "p" || it.type == "li" }
            ?.text
            .orEmpty()
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clip(CardDefaults.shape)
                .clickable(onClickLabel = "${rule.title} öffnen", onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) colors.secondaryContainer else colors.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(rule.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                summary.ifBlank { rule.source },
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Regelseite; das Regelwerk ist noch in Arbeit, daher bewusst schlichte Platzhalter-Darstellung. */
@Composable
internal fun RuleDetailPane(
    rule: RulePage?,
    state: LibraryUiState,
    showBack: Boolean,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: (RulePage) -> Unit,
    onRestore: (String) -> Unit,
    onArchive: (RulePage) -> Unit = {},
    onImportMedia: (RulePage, List<Uri>, MediaKind) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        RuffTopBar(title = rule?.title ?: "Regelwerk", showBack = showBack)
        when {
            rule == null -> EmptyState("Wähle links eine Regelseite aus.", Modifier.padding(16.dp))
            state.editing ->
                RuleEditor(
                    rule = rule,
                    error = state.editorError,
                    onCancel = onCancelEdit,
                    onSave = onSave,
                    onRestore = { onRestore(rule.source) },
                    onImportMedia = { uris, kind -> onImportMedia(rule, uris, kind) },
                )
            else -> RuleBlocks(rule, state.canEdit, state.editorError, onEdit, { onArchive(rule) })
        }
    }
}

@Composable
private fun RuleBlocks(
    rule: RulePage,
    canEdit: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onArchive: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    var showArchiveConfirmation by rememberSaveable(rule.source) { mutableStateOf(false) }
    ContentPresentationFrame(rule.presentation, Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("rule_detail"),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
        ) {
            item {
                Text(rule.source, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text(
                    "Regelwerk in Arbeit · vorläufige Darstellung",
                    color = colors.tertiary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            itemsIndexed(rule.blocks) { index, block ->
                RuleBlockView(block, zebra = index % 2 == 0)
            }
            item { MediaGallery(rule.presentation.media, Modifier.padding(vertical = 10.dp)) }
            item {
                error?.let { Text(it, color = colors.error, modifier = Modifier.padding(vertical = 8.dp)) }
                if (canEdit) {
                    ChainDivider(Modifier.padding(vertical = 8.dp), maxWidth = 220.dp)
                    Button(onClick = onEdit, colors = emberButtonColors(), modifier = Modifier.fillMaxWidth()) {
                        Text("Inhalt bearbeiten")
                    }
                    TextButton(onClick = { showArchiveConfirmation = true }) { Text("Regelseite archivieren") }
                }
            }
        }
    }
    if (showArchiveConfirmation) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirmation = false },
            title = { Text("Regelseite archivieren?") },
            text = { Text("Die Regelseite wird ausgeblendet und kann im Admin-Archiv wiederhergestellt werden.") },
            confirmButton = {
                TextButton(onClick = {
                    showArchiveConfirmation = false
                    onArchive()
                }) { Text("Archivieren") }
            },
            dismissButton = { TextButton(onClick = { showArchiveConfirmation = false }) { Text("Abbrechen") } },
        )
    }
}

/** Einfache Darstellung der Blocktypen h1/h2/h3(/h4), p, li und row (Tabellenzeile, Trenner „ · “). */
@Composable
internal fun RuleBlockView(
    block: RuleBlock,
    zebra: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    when (block.type) {
        "h1" -> SectionHeader(block.text, Modifier.padding(top = 16.dp, bottom = 4.dp))
        "h2" ->
            Text(
                block.text,
                color = colors.secondary,
                style = typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp).semantics { heading() },
            )
        "h3", "h4" ->
            Text(
                block.text,
                color = colors.secondary,
                style = typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).semantics { heading() },
            )
        "li" ->
            Row(Modifier.padding(vertical = 3.dp)) {
                Text("•", color = colors.primary, modifier = Modifier.padding(end = 8.dp))
                Text(block.text, color = colors.onSurface, style = typography.bodyLarge)
            }
        "row" -> {
            val cells = block.text.split(" · ")
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (zebra) colors.surfaceContainer else colors.surfaceContainerLow)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                cells.forEachIndexed { i, cell ->
                    Text(
                        cell,
                        color = if (i == 0) colors.onSurface else colors.onSurfaceVariant,
                        style = if (i == 0) typography.labelLarge else typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                    )
                }
            }
        }
        else ->
            Text(
                block.text,
                color = colors.onSurface,
                style = typography.bodyLarge,
                modifier = Modifier.padding(vertical = 6.dp),
            )
    }
}
