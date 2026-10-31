package de.ruff.dimension.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.ruff.dimension.data.AdminAppearance
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.ui.common.RuffTopBar

@Composable
internal fun AdminScreen(
    appearance: AdminAppearance,
    archivedCreatures: List<Creature>,
    archivedRules: List<RulePage>,
    error: String?,
    adminError: String?,
    onSaveAppearance: (AdminAppearance) -> Unit,
    onBackgroundSelected: (android.net.Uri) -> Unit,
    onFontSelected: (android.net.Uri) -> Unit,
    onClearBackground: () -> Unit,
    onClearFont: () -> Unit,
    onRestoreCreature: (Creature) -> Unit,
    onRestoreRule: (RulePage) -> Unit,
    onChangePin: (String, String, String) -> Unit,
    onLock: () -> Unit,
) {
    val backgroundPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(onBackgroundSelected)
        }
    val fontPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(onFontSelected)
        }
    var accentColor by rememberSaveable(appearance.accentColor) { mutableStateOf(appearance.accentColor) }
    var frameColor by rememberSaveable(appearance.frameColor) { mutableStateOf(appearance.frameColor) }
    var frameWidth by rememberSaveable(appearance.frameWidthDp) { mutableStateOf(appearance.frameWidthDp.toString()) }
    var cornerRadius by rememberSaveable(appearance.cornerRadiusDp) { mutableStateOf(appearance.cornerRadiusDp.toString()) }
    var showArchive by rememberSaveable { mutableStateOf(false) }
    var showChangePin by rememberSaveable { mutableStateOf(false) }
    var currentPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var pinConfirmation by rememberSaveable { mutableStateOf("") }
    val parsedFrameWidth = frameWidth.toFloatOrNull()?.takeIf { it in 0f..12f }
    val parsedRadius = cornerRadius.toFloatOrNull()?.takeIf { it in 0f..48f }
    val validColors =
        runCatching {
            android.graphics.Color.parseColor(accentColor)
            android.graphics.Color.parseColor(frameColor)
        }.isSuccess

    Column(Modifier.fillMaxSize()) {
        RuffTopBar(title = "Admin-Verwaltung", showBack = true)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(16.dp),
        ) {
            item {
                Text(
                    "Gestaltung",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    "Änderungen bleiben auf diesem Gerät. Seed-Originale werden nicht überschrieben.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedTextField(
                    value = accentColor,
                    onValueChange = { accentColor = it },
                    label = { Text("Akzentfarbe · #RRGGBB") },
                    isError = !validColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = frameColor,
                    onValueChange = { frameColor = it },
                    label = { Text("Rahmenfarbe · #RRGGBB") },
                    isError = !validColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = frameWidth,
                        onValueChange = { frameWidth = it },
                        label = { Text("Rahmenbreite dp") },
                        isError = parsedFrameWidth == null,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = cornerRadius,
                        onValueChange = { cornerRadius = it },
                        label = { Text("Eckenradius dp") },
                        isError = parsedRadius == null,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { backgroundPicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (appearance.backgroundPath.isBlank()) "Hintergrundbild auswählen" else "Hintergrundbild ändern")
                }
                if (appearance.backgroundPath.isNotBlank()) {
                    Text(appearance.backgroundPath.substringAfterLast('/'), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onClearBackground) { Text("Hintergrund zurücksetzen") }
                }
                Button(
                    onClick = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (appearance.fontPath.isBlank()) "Schriftdatei auswählen" else "Schriftdatei ändern")
                }
                if (appearance.fontPath.isNotBlank()) {
                    Text(appearance.fontPath.substringAfterLast('/'), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onClearFont) { Text("Schrift zurücksetzen") }
                }
                Button(
                    onClick = {
                        val width = parsedFrameWidth
                        val radius = parsedRadius
                        if (validColors && width != null && radius != null) {
                            onSaveAppearance(
                                appearance.copy(
                                    accentColor = accentColor,
                                    frameColor = frameColor,
                                    frameWidthDp = width,
                                    cornerRadiusDp = radius,
                                ),
                            )
                        }
                    },
                    enabled = validColors && parsedFrameWidth != null && parsedRadius != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Gestaltung speichern")
                }
            }
            item {
                TextButton(onClick = { showChangePin = true }) { Text("Admin-PIN ändern") }
                Button(onClick = { showArchive = !showArchive }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showArchive) "Archiv schließen" else "Archivierte Inhalte (${archivedCreatures.size + archivedRules.size})")
                }
            }
            if (showArchive) {
                items(archivedCreatures, key = { "creature:${it.id}" }) { creature ->
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(creature.name, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onRestoreCreature(creature) }) { Text("Wiederherstellen") }
                    }
                }
                items(archivedRules, key = { "rule:${it.source}" }) { rule ->
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(rule.title, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onRestoreRule(rule) }) { Text("Wiederherstellen") }
                    }
                }
                if (showChangePin) {
                    AlertDialog(
                        onDismissRequest = {
                            showChangePin = false
                            currentPin = ""
                            newPin = ""
                            pinConfirmation = ""
                        },
                        title = { Text("Admin-PIN ändern") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Der bisherige PIN wird benötigt. Mindestens vier Ziffern.")
                                PinField("Bisheriger PIN", currentPin) { currentPin = it }
                                PinField("Neuer PIN", newPin) { newPin = it }
                                PinField("Neuen PIN wiederholen", pinConfirmation) { pinConfirmation = it }
                                adminError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { onChangePin(currentPin, newPin, pinConfirmation) },
                                enabled =
                                    (
                                        currentPin.isNotBlank() &&
                                            newPin.length >= 4 &&
                                            newPin.all(Char::isDigit) &&
                                            newPin == pinConfirmation
                                    ),
                            ) { Text("PIN ändern") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showChangePin = false
                                currentPin = ""
                                newPin = ""
                                pinConfirmation = ""
                            }) { Text("Abbrechen") }
                        },
                    )
                }
            }

            @Composable
            private fun PinField(
                label: String,
                value: String,
                onValueChange: (String) -> Unit,
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(it.filter(Char::isDigit).take(12)) },
                    label = { Text(label) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Die Schriftdatei wird nur lokal verwendet. Verwende nur Schriften, für die du Nutzungsrechte hast.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onLock) { Text("Admin-Modus sperren") }
            }
        }
    }
}
