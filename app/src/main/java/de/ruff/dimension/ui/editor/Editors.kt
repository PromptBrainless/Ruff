package de.ruff.dimension.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RuleBlock
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.ui.common.emberButtonColors

/*
 * Editor-MVP (content_edits), Verhalten unverändert aus MainActivity übernommen.
 * Neu: Eingaben liegen in rememberSaveable und überstehen Drehen und Prozessende.
 */

/** Speichert Feldwerte (Reihenfolge bleibt erhalten) als flache Liste. */
private val FieldsSaver =
    listSaver<Map<String, String>, String>(
        save = { map -> map.flatMap { (k, v) -> listOf(k, v) } },
        restore = { list -> list.chunked(2).associate { (k, v) -> k to v } },
    )

/** Speichert Regelblöcke (Typ, Text) als flache Liste. */
private val BlocksSaver =
    listSaver<List<RuleBlock>, String>(
        save = { blocks -> blocks.flatMap { listOf(it.type, it.text) } },
        restore = { list -> list.chunked(2).map { (type, text) -> RuleBlock(type, text) } },
    )

@Composable
internal fun CreatureEditor(
    creature: Creature,
    error: String?,
    onCancel: () -> Unit,
    onSave: (Creature) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(creature.id) { mutableStateOf(creature.name) }
    var description by rememberSaveable(creature.id) { mutableStateOf(creature.description) }
    var archetype by rememberSaveable(creature.id) { mutableStateOf(creature.archetype) }
    var powerLabel by rememberSaveable(creature.id) { mutableStateOf(creature.powerLabel) }
    var powerLevel by rememberSaveable(creature.id) { mutableStateOf(creature.powerLevel.toString()) }
    var fields by rememberSaveable(creature.id, stateSaver = FieldsSaver) { mutableStateOf(creature.fields) }
    var restoreConfirmation by rememberSaveable(creature.id) { mutableStateOf(false) }
    val parsedPowerLevel = powerLevel.toIntOrNull()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
    ) {
        Text("Wesen bearbeiten", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            description,
            { description = it },
            label = { Text("Beschreibung") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        OutlinedTextField(archetype, { archetype = it }, label = { Text("Archetyp") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(powerLabel, { powerLabel = it }, label = { Text("Machtstufe") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            powerLevel,
            { powerLevel = it },
            label = { Text("Machtstufen-Wert") },
            modifier = Modifier.fillMaxWidth(),
            isError = parsedPowerLevel == null,
        )
        fields.forEach { (label, value) ->
            OutlinedTextField(
                value = value,
                onValueChange = { fields = fields + (label to it) },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    parsedPowerLevel?.let {
                        onSave(
                            creature.copy(
                                name = name,
                                description = description,
                                archetype = archetype,
                                powerLabel = powerLabel,
                                powerLevel = it,
                                fields = fields,
                            ),
                        )
                    }
                },
                enabled = parsedPowerLevel != null && name.isNotBlank(),
                colors = emberButtonColors(),
            ) {
                Text("Speichern")
            }
            TextButton(onClick = onCancel) { Text("Abbrechen") }
        }
        TextButton(onClick = { restoreConfirmation = true }) { Text("Seed-Original wiederherstellen") }
    }
    if (restoreConfirmation) {
        RestoreDialog(onDismiss = { restoreConfirmation = false }, onConfirm = {
            restoreConfirmation = false
            onRestore()
        })
    }
}

@Composable
internal fun RuleEditor(
    rule: RulePage,
    error: String?,
    onCancel: () -> Unit,
    onSave: (RulePage) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable(rule.source) { mutableStateOf(rule.title) }
    var blocks by rememberSaveable(rule.source, stateSaver = BlocksSaver) { mutableStateOf(rule.blocks) }
    var restoreConfirmation by rememberSaveable(rule.source) { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Regelseite bearbeiten", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            OutlinedTextField(
                title,
                { title = it },
                label = { Text("Titel") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        itemsIndexed(blocks, key = { index, block -> "$index:${block.type}" }) { index, block ->
            OutlinedTextField(
                value = block.text,
                onValueChange = { text -> blocks = blocks.toMutableList().also { it[index] = block.copy(text = text) } },
                label = { Text("Block ${index + 1} · ${block.type}") },
                modifier = Modifier.fillMaxWidth(),
                minLines = if (block.type in setOf("h2", "h3", "h4")) 1 else 2,
            )
        }
        item {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSave(rule.copy(title = title, blocks = blocks)) },
                    enabled = title.isNotBlank(),
                    colors = emberButtonColors(),
                ) {
                    Text("Speichern")
                }
                TextButton(onClick = onCancel) { Text("Abbrechen") }
            }
            TextButton(onClick = { restoreConfirmation = true }) { Text("Seed-Original wiederherstellen") }
        }
    }
    if (restoreConfirmation) {
        RestoreDialog(onDismiss = { restoreConfirmation = false }, onConfirm = {
            restoreConfirmation = false
            onRestore()
        })
    }
}

@Composable
private fun RestoreDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Original wiederherstellen?") },
        text = { Text("Die lokale Bearbeitung wird verworfen. Die Seed-Daten bleiben unverändert.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Wiederherstellen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
