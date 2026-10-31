package de.ruff.dimension.ui.akte

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.ruff.dimension.domain.akte.AkteAbschnitt
import de.ruff.dimension.domain.akte.AkteEntwurf
import de.ruff.dimension.domain.akte.AllergieArt
import de.ruff.dimension.domain.akte.CreatureProfile
import de.ruff.dimension.domain.akte.ProfilFeld
import de.ruff.dimension.domain.akte.Schwere
import de.ruff.dimension.domain.akte.Verfassungsstufe
import de.ruff.dimension.ui.common.RuffHaptik
import de.ruff.dimension.ui.common.SectionHeader
import de.ruff.dimension.ui.common.emberButtonColors
import de.ruff.dimension.ui.common.rememberRuffHaptik

/*
 * Editor der Kreaturen-Akte. Der Entwurf ist eine flache Schlüssel/Wert-Liste
 * (AkteEntwurf) und übersteht Drehen und Prozessende. Beim Speichern prüft das
 * ViewModel den Entwurf; jedes geänderte Feld wird zu „manuell“.
 */

private val EntwurfSaver =
    listSaver<Map<String, String>, String>(
        save = { map -> map.flatMap { (k, v) -> listOf(k, v) } },
        restore = { list -> LinkedHashMap(list.chunked(2).associate { (k, v) -> k to v }) },
    )

@Composable
internal fun CreatureProfileEditor(
    creatureId: String,
    creatureName: String,
    profile: CreatureProfile?,
    feldFehler: Map<String, String>,
    error: String?,
    busy: Boolean,
    onCancel: () -> Unit,
    onSave: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var entwurf by rememberSaveable(creatureId, stateSaver = EntwurfSaver) { mutableStateOf(AkteEntwurf.aus(profile)) }
    BackHandler(onBack = onCancel)

    fun wert(key: String) = entwurf[key].orEmpty()

    fun setze(
        key: String,
        value: String,
    ) {
        entwurf = entwurf + (key to value)
    }

    @Composable
    fun Feld(
        key: String,
        label: String,
        zahl: KeyboardType? = null,
        minLines: Int = 1,
        hilfe: String? = null,
        modifier: Modifier = Modifier.fillMaxWidth(),
    ) {
        val fehler = feldFehler[key]
        OutlinedTextField(
            value = wert(key),
            onValueChange = { setze(key, it) },
            label = { Text(label) },
            isError = fehler != null,
            supportingText = (fehler ?: hilfe)?.let { t -> { Text(t) } },
            keyboardOptions = zahl?.let { KeyboardOptions(keyboardType = it) } ?: KeyboardOptions.Default,
            minLines = minLines,
            singleLine = minLines == 1 && zahl != null,
            modifier = modifier.testTag("akte_edit_$key"),
        )
    }

    @Composable
    fun Begruendung(feld: ProfilFeld) {
        Feld(AkteEntwurf.begruendung(feld), "Begründung · ${feld.label}", minLines = 2)
    }

    @Composable
    fun Kopf(feld: ProfilFeld) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Text(
                feld.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            profile?.angabe(feld)?.let { HerkunftBadge(it.herkunft) }
        }
    }

    @Composable
    fun Spanne(
        feld: ProfilFeld,
        einheit: String,
        typ: KeyboardType,
        hilfe: String? = null,
    ) {
        Kopf(feld)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Feld(AkteEntwurf.key(feld, "min"), "von ($einheit)", zahl = typ, modifier = Modifier.weight(1f))
            Feld(AkteEntwurf.key(feld, "max"), "bis ($einheit)", zahl = typ, modifier = Modifier.weight(1f))
        }
        hilfe?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Begruendung(feld)
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
                .testTag("akte_editor"),
    ) {
        Text("Akte bearbeiten · $creatureName", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
        Text(
            "Geänderte Werte werden als „manuell“ markiert. Leere Felder gelten als unbekannt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp),
        )

        SectionHeader(AkteAbschnitt.STECKBRIEF.titel, Modifier.padding(top = 8.dp))
        listOf(ProfilFeld.ERSCHEINUNG, ProfilFeld.LEBENSRAUM, ProfilFeld.VERHALTEN).forEach { feld ->
            Kopf(feld)
            Feld(AkteEntwurf.key(feld, "wert"), feld.label, minLines = 3)
            Begruendung(feld)
        }

        SectionHeader(AkteAbschnitt.KOERPER.titel, Modifier.padding(top = 12.dp))
        Spanne(ProfilFeld.GROESSE, "m", KeyboardType.Decimal)
        Spanne(ProfilFeld.GEWICHT, "kg", KeyboardType.Decimal)
        Spanne(ProfilFeld.PULS, "bpm", KeyboardType.Number, hilfe = "0 bis 0 bedeutet: kein Herzschlag.")
        Kopf(ProfilFeld.BLUTDRUCK)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Feld(
                AkteEntwurf.key(ProfilFeld.BLUTDRUCK, "sys"),
                "systolisch (mmHg)",
                zahl = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            Feld(
                AkteEntwurf.key(ProfilFeld.BLUTDRUCK, "dia"),
                "diastolisch (mmHg)",
                zahl = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        Feld(AkteEntwurf.key(ProfilFeld.BLUTDRUCK, "hinweis"), "Hinweis (z. B. „kein Kreislauf“)")
        Begruendung(ProfilFeld.BLUTDRUCK)

        SectionHeader(AkteAbschnitt.VERFASSUNG.titel, Modifier.padding(top = 12.dp))
        Kopf(ProfilFeld.VERFASSUNG)
        val stufeKey = AkteEntwurf.key(ProfilFeld.VERFASSUNG, "stufe")
        Auswahl(
            label = "Einstufung",
            aktuell = Verfassungsstufe.entries.firstOrNull { it.name == wert(stufeKey) }?.label ?: "nicht eingestuft",
            optionen = listOf("" to "nicht eingestuft") + Verfassungsstufe.entries.map { it.name to it.label },
            fehler = feldFehler[stufeKey],
            onWahl = { setze(stufeKey, it) },
        )
        Feld(AkteEntwurf.key(ProfilFeld.VERFASSUNG, "text"), "Beschreibung der Verfassung", minLines = 2)
        Begruendung(ProfilFeld.VERFASSUNG)

        Kopf(ProfilFeld.ALLERGIEN)
        val anzahl = AkteEntwurf.anzahlAllergien(entwurf)
        if (anzahl == 0) {
            Text(
                "Keine Auslöser eingetragen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        for (i in 0 until anzahl) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Column(Modifier.padding(10.dp)) {
                    Feld(AkteEntwurf.allergieKey(i, "ausloeser"), "Auslöser")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val artKey = AkteEntwurf.allergieKey(i, "art")
                        val schwereKey = AkteEntwurf.allergieKey(i, "schwere")
                        Box(Modifier.weight(1f)) {
                            Auswahl(
                                label = "Art",
                                aktuell =
                                    AllergieArt.entries.firstOrNull { it.name == wert(artKey) }?.label
                                        ?: AllergieArt.UNVERTRAEGLICHKEIT.label,
                                optionen = AllergieArt.entries.map { it.name to it.label },
                                onWahl = { setze(artKey, it) },
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            Auswahl(
                                label = "Schwere",
                                aktuell = Schwere.entries.firstOrNull { it.name == wert(schwereKey) }?.label ?: Schwere.DEUTLICH.label,
                                optionen = Schwere.entries.map { it.name to it.label },
                                onWahl = { setze(schwereKey, it) },
                            )
                        }
                    }
                    Feld(AkteEntwurf.allergieKey(i, "reaktion"), "Reaktion", minLines = 2)
                    TextButton(onClick = { entwurf = AkteEntwurf.ohneAllergie(entwurf, i) }) { Text("Auslöser entfernen") }
                }
            }
        }
        if (anzahl < AkteEntwurf.MAX_ALLERGIEN) {
            OutlinedButton(onClick = { entwurf = AkteEntwurf.mitNeuerAllergie(entwurf) }) { Text("Auslöser hinzufügen") }
        }
        Begruendung(ProfilFeld.ALLERGIEN)

        SectionHeader(AkteAbschnitt.KOGNITION.titel, Modifier.padding(top = 12.dp))
        Spanne(ProfilFeld.IQ, "IQ", KeyboardType.Number, hilfe = "Menschlicher Maßstab, Mittel 100.")
        Spanne(ProfilFeld.EQ, "EQ", KeyboardType.Number)
        Kopf(ProfilFeld.WAHNHAFT)
        val wahnKey = AkteEntwurf.key(ProfilFeld.WAHNHAFT, "wert")
        val haptik = rememberRuffHaptik()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ja" to "ja", "nein" to "nein", "" to "unbekannt").forEach { (code, label) ->
                FilterChip(
                    selected = wert(wahnKey) == code,
                    onClick = {
                        haptik(RuffHaptik.AUSWAHL)
                        setze(wahnKey, code)
                    },
                    label = { Text(label) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                )
            }
        }
        Begruendung(ProfilFeld.WAHNHAFT)

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSave(entwurf) },
                enabled = !busy,
                colors = emberButtonColors(),
                modifier = Modifier.testTag("akte_speichern"),
            ) {
                Text("Speichern")
            }
            TextButton(onClick = onCancel) { Text("Abbrechen") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Einfache Auswahlliste (Schlüssel → Anzeigetext). */
@Composable
private fun Auswahl(
    label: String,
    aktuell: String,
    optionen: List<Pair<String, String>>,
    onWahl: (String) -> Unit,
    fehler: String? = null,
) {
    var offen by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { offen = true }, modifier = Modifier.fillMaxWidth()) {
                Text("$aktuell ▾", maxLines = 1)
            }
            DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
                optionen.forEach { (code, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            offen = false
                            onWahl(code)
                        },
                    )
                }
            }
        }
        fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
