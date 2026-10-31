package de.ruff.dimension.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.ruff.dimension.data.ContentPresentation
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.MediaAttachment
import de.ruff.dimension.data.RuleBlock
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.repository.MediaKind
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
    onImportMedia: (List<Uri>, MediaKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(creature.id) { mutableStateOf(creature.name) }
    var description by rememberSaveable(creature.id) { mutableStateOf(creature.description) }
    var archetype by rememberSaveable(creature.id) { mutableStateOf(creature.archetype) }
    var powerLabel by rememberSaveable(creature.id) { mutableStateOf(creature.powerLabel) }
    var powerLevel by rememberSaveable(creature.id) { mutableStateOf(creature.powerLevel.toString()) }
    var fields by rememberSaveable(creature.id, stateSaver = FieldsSaver) { mutableStateOf(creature.fields) }
    var fieldLabels by rememberSaveable(creature.id, stateSaver = FieldsSaver) {
        mutableStateOf(creature.fields.keys.associateWith { it })
    }
    var presentation by remember(creature.id, creature.presentation) { mutableStateOf(creature.presentation) }
    var frameWidth by rememberSaveable(creature.id) { mutableStateOf(creature.presentation.frameWidthDp.toString()) }
    var cornerRadius by rememberSaveable(creature.id) { mutableStateOf(creature.presentation.cornerRadiusDp.toString()) }
    var frameColor by rememberSaveable(creature.id) { mutableStateOf(creature.presentation.frameColor) }
    var media by remember(creature.id, creature.presentation.media) { mutableStateOf(creature.presentation.media) }
    var restoreConfirmation by rememberSaveable(creature.id) { mutableStateOf(false) }
    val parsedPowerLevel = powerLevel.toIntOrNull()
    val parsedFrameWidth = frameWidth.toFloatOrNull()?.takeIf { it in 0f..12f }
    val parsedCornerRadius = cornerRadius.toFloatOrNull()?.takeIf { it in 0f..48f }

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fieldLabels[label].orEmpty(),
                    onValueChange = { fieldLabels = fieldLabels + (label to it) },
                    label = { Text("Bereich") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                TextButton(onClick = {
                    fields = fields - label
                    fieldLabels = fieldLabels - label
                }) { Text("Entfernen") }
            }
            OutlinedTextField(
                value = value,
                onValueChange = { fields = fields + (label to it) },
                label = { Text("Inhalt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
        }
        TextButton(onClick = {
            var index = fields.size + 1
            while ("Bereich $index" in fields) index++
            val newLabel = "Bereich $index"
            fields = fields + (newLabel to "")
            fieldLabels = fieldLabels + (newLabel to newLabel)
        }) { Text("Bereich hinzufügen") }
        PresentationEditor(
            presentation = presentation,
            frameColor = frameColor,
            frameWidth = frameWidth,
            cornerRadius = cornerRadius,
            media = media,
            onFrameColorChange = { frameColor = it },
            onFrameWidthChange = { frameWidth = it },
            onCornerRadiusChange = { cornerRadius = it },
            onRemoveMedia = { index -> media = media.toMutableList().also { it.removeAt(index) } },
            onImportMedia = onImportMedia,
        )
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
                                fields = fields.mapKeys { (key, _) -> fieldLabels[key].orEmpty().trim() },
                                presentation =
                                    presentation.copy(
                                        media = media,
                                        frameColor = frameColor,
                                        frameWidthDp = parsedFrameWidth ?: presentation.frameWidthDp,
                                        cornerRadiusDp = parsedCornerRadius ?: presentation.cornerRadiusDp,
                                    ),
                            ),
                        )
                    }
                },
                enabled =
                    parsedPowerLevel != null &&
                        name.isNotBlank() &&
                        parsedFrameWidth != null &&
                        parsedCornerRadius != null &&
                        fieldLabels.values.all(String::isNotBlank) &&
                        fieldLabels.values
                            .map(String::trim)
                            .toSet()
                            .size == fieldLabels.size &&
                        runCatching { android.graphics.Color.parseColor(frameColor) }.isSuccess,
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
    onImportMedia: (List<Uri>, MediaKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable(rule.source) { mutableStateOf(rule.title) }
    var blocks by rememberSaveable(rule.source, stateSaver = BlocksSaver) { mutableStateOf(rule.blocks) }
    var presentation by remember(rule.source, rule.presentation) { mutableStateOf(rule.presentation) }
    var frameWidth by rememberSaveable(rule.source) { mutableStateOf(rule.presentation.frameWidthDp.toString()) }
    var cornerRadius by rememberSaveable(rule.source) { mutableStateOf(rule.presentation.cornerRadiusDp.toString()) }
    var frameColor by rememberSaveable(rule.source) { mutableStateOf(rule.presentation.frameColor) }
    var media by remember(rule.source, rule.presentation.media) { mutableStateOf(rule.presentation.media) }
    var restoreConfirmation by rememberSaveable(rule.source) { mutableStateOf(false) }
    val parsedFrameWidth = frameWidth.toFloatOrNull()?.takeIf { it in 0f..12f }
    val parsedCornerRadius = cornerRadius.toFloatOrNull()?.takeIf { it in 0f..48f }

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
            Column {
                OutlinedTextField(
                    value = block.text,
                    onValueChange = { text -> blocks = blocks.toMutableList().also { it[index] = block.copy(text = text) } },
                    label = { Text("Block ${index + 1} · ${block.type}") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (block.type in setOf("h2", "h3", "h4")) 1 else 2,
                )
                TextButton(onClick = { blocks = blocks.toMutableList().also { it.removeAt(index) } }) {
                    Text("Block entfernen")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { blocks = blocks + RuleBlock(type = "p") }) { Text("Absatz hinzufügen") }
                TextButton(onClick = { blocks = blocks + RuleBlock(type = "h2") }) { Text("Überschrift hinzufügen") }
            }
            PresentationEditor(
                presentation = presentation,
                frameColor = frameColor,
                frameWidth = frameWidth,
                cornerRadius = cornerRadius,
                media = media,
                onFrameColorChange = { frameColor = it },
                onFrameWidthChange = { frameWidth = it },
                onCornerRadiusChange = { cornerRadius = it },
                onRemoveMedia = { index -> media = media.toMutableList().also { it.removeAt(index) } },
                onImportMedia = onImportMedia,
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSave(
                            rule.copy(
                                title = title,
                                blocks = blocks,
                                presentation =
                                    presentation.copy(
                                        media = media,
                                        frameColor = frameColor,
                                        frameWidthDp = parsedFrameWidth ?: presentation.frameWidthDp,
                                        cornerRadiusDp = parsedCornerRadius ?: presentation.cornerRadiusDp,
                                    ),
                            ),
                        )
                    },
                    enabled =
                        title.isNotBlank() &&
                            parsedFrameWidth != null &&
                            parsedCornerRadius != null &&
                            runCatching { android.graphics.Color.parseColor(frameColor) }.isSuccess,
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
private fun PresentationEditor(
    presentation: ContentPresentation,
    frameColor: String,
    frameWidth: String,
    cornerRadius: String,
    media: List<MediaAttachment>,
    onFrameColorChange: (String) -> Unit,
    onFrameWidthChange: (String) -> Unit,
    onCornerRadiusChange: (String) -> Unit,
    onRemoveMedia: (Int) -> Unit,
    onImportMedia: (List<Uri>, MediaKind) -> Unit,
) {
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) onImportMedia(uris, MediaKind.IMAGE)
        }
    val videoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onImportMedia(listOf(it), MediaKind.VIDEO) }
        }
    val backgroundPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onImportMedia(listOf(it), MediaKind.BACKGROUND) }
        }
    val fontPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onImportMedia(listOf(it), MediaKind.FONT) }
        }

    Text("Darstellung und Medien", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    OutlinedTextField(
        value = frameColor,
        onValueChange = onFrameColorChange,
        label = { Text("Rahmenfarbe · #RRGGBB") },
        isError = runCatching { android.graphics.Color.parseColor(frameColor) }.isFailure,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = frameWidth,
            onValueChange = onFrameWidthChange,
            label = { Text("Rahmen dp") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = cornerRadius,
            onValueChange = onCornerRadiusChange,
            label = { Text("Ecken dp") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) { Text("Bilder hinzufügen") }
        TextButton(onClick = { videoPicker.launch(arrayOf("video/*")) }) { Text("Video hinzufügen") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { backgroundPicker.launch(arrayOf("image/*")) }) {
            Text(if (presentation.backgroundPath.isBlank()) "Hintergrund setzen" else "Hintergrund ändern")
        }
        TextButton(onClick = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) }) {
            Text(if (presentation.fontPath.isBlank()) "Schrift setzen" else "Schrift ändern")
        }
    }
    media.forEachIndexed { index, attachment ->
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(attachment.displayName, modifier = Modifier.weight(1f))
            TextButton(onClick = { onRemoveMedia(index) }) { Text("Entfernen") }
        }
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
