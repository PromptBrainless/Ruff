package de.ruff.dimension.ui.akte

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.ruff.dimension.domain.akte.AkteAbschnitt
import de.ruff.dimension.domain.akte.AkteFormat
import de.ruff.dimension.domain.akte.Angabe
import de.ruff.dimension.domain.akte.CreatureProfile
import de.ruff.dimension.domain.akte.GanzzahlSpanne
import de.ruff.dimension.domain.akte.Herkunft
import de.ruff.dimension.domain.akte.ProfilFeld
import de.ruff.dimension.ui.common.ChainDivider
import de.ruff.dimension.ui.common.RuffDurations
import de.ruff.dimension.ui.common.RuffHaptik
import de.ruff.dimension.ui.common.SectionHeader
import de.ruff.dimension.ui.common.currentMotion
import de.ruff.dimension.ui.common.emberButtonColors
import de.ruff.dimension.ui.common.rememberRuffHaptik
import de.ruff.dimension.ui.theme.Glut
import de.ruff.dimension.ui.theme.Pergament
import kotlinx.coroutines.delay

/*
 * Kreaturen-Akte in der Detailansicht: vier Abschnitte (Steckbrief, Körperliche Werte,
 * Verfassung & Allergien, Kognition). Jeder Wert trägt ein Herkunfts-Abzeichen; ein
 * Tipp auf die Zeile klappt die Begründung auf. Alle Bewegungen laufen über RuffMotion
 * und entfallen im reduzierten Modus.
 */

/** Obergrenze der IQ/EQ-Skala in der Anzeige (menschliches Mittel 100). */
private const val KOGNITION_SKALA = 200f

/** Versatz zwischen den Abschnitten beim Einblenden. */
private const val STAFFEL_MS = 70

@Composable
internal fun CreatureAkteSections(
    creatureId: String,
    creatureName: String,
    canEdit: Boolean,
    binding: AkteBinding,
    modifier: Modifier = Modifier,
) {
    val state = binding.state
    if (!state.available) {
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = modifier.padding(vertical = 8.dp)) }
        return
    }
    val profile = state.profile(creatureId)
    Column(modifier = modifier.fillMaxWidth().testTag("akte_$creatureId")) {
        ChainDivider(Modifier.padding(vertical = 4.dp), maxWidth = 280.dp)
        if (profile == null || profile.istLeer) {
            SectionHeader(AkteAbschnitt.STECKBRIEF.titel)
            Text(
                "Für $creatureName gibt es noch keine Akte.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            )
            if (canEdit) {
                OutlinedButton(onClick = { binding.onEdit(creatureId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Akte anlegen")
                }
            }
            return@Column
        }
        if (profile.offeneAbleitungen > 0) {
            Text(
                "Abgeleitete Werte sind Einschätzungen aus Bild und Lore – keine Fakten.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        AkteAbschnitt.entries.forEachIndexed { index, abschnitt ->
            val felder = profile.vorhandeneFelder.filter { it.abschnitt == abschnitt }
            if (felder.isNotEmpty()) {
                StaffelEinblendung(index) {
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        SectionHeader(abschnitt.titel)
                        felder.forEach { feld -> AkteZeile(feld, profile) }
                    }
                }
            }
        }
        if (profile.quellen.isNotEmpty()) {
            Text(
                "Grundlage: " + profile.quellen.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
        state.hinweis?.let {
            Text(it, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(vertical = 4.dp))
            LaunchedEffect(it) {
                delay(HINWEIS_MS)
                binding.onDismissHinweis()
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp)) }
        if (canEdit) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                if (profile.offeneAbleitungen > 0) {
                    OutlinedButton(
                        onClick = { binding.onConfirm(creatureId) },
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Werte bestätigen (${profile.offeneAbleitungen})")
                    }
                }
                Button(
                    onClick = { binding.onEdit(creatureId) },
                    enabled = !state.busy,
                    colors = emberButtonColors(),
                    modifier = Modifier.weight(1f).testTag("akte_bearbeiten"),
                ) {
                    Text("Akte bearbeiten")
                }
            }
        }
    }
}

private const val HINWEIS_MS = 2_500L

/** Blendet einen Abschnitt gestaffelt ein; im reduzierten Modus sofort sichtbar. */
@Composable
private fun StaffelEinblendung(
    index: Int,
    content: @Composable () -> Unit,
) {
    val motion = currentMotion
    val sichtbar = remember { MutableTransitionState(motion.reduced) }
    LaunchedEffect(Unit) { sichtbar.targetState = true }
    val enter =
        if (motion.reduced) {
            EnterTransition.None
        } else {
            val dauer = motion.duration(RuffDurations.MITTEL)
            val versatz = index * STAFFEL_MS
            fadeIn(tween(dauer, delayMillis = versatz, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(dauer, delayMillis = versatz, easing = FastOutSlowInEasing)) { it / 10 }
        }
    AnimatedVisibility(visibleState = sichtbar, enter = enter) { content() }
}

/** Eine Zeile: Feldname, Wert, Herkunft; Tipp klappt die Begründung auf. */
@Composable
private fun AkteZeile(
    feld: ProfilFeld,
    profile: CreatureProfile,
) {
    val angabe = profile.angabe(feld) ?: return
    val motion = currentMotion
    val haptik = rememberRuffHaptik()
    var offen by rememberSaveable(profile.creatureId, feld.name) { mutableStateOf(false) }
    val begruendung = angabe.begruendung.takeIf { it.isNotBlank() }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = begruendung != null,
                    role = Role.Button,
                    onClickLabel = if (offen) "Begründung ausblenden" else "Begründung zeigen",
                ) {
                    haptik(RuffHaptik.AUSWAHL)
                    offen = !offen
                }.padding(vertical = 8.dp, horizontal = 4.dp)
                .semantics { stateDescription = if (offen) "Begründung offen" else "Begründung zu" }
                .testTag("akte_feld_${feld.name}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                feld.label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.secondary,
                modifier = Modifier.weight(1f),
            )
            HerkunftBadge(angabe.herkunft)
        }
        Spacer(Modifier.height(2.dp))
        FeldWert(feld, angabe, profile)
        AnimatedVisibility(
            visible = offen && begruendung != null,
            enter = if (motion.reduced) EnterTransition.None else fadeIn(motion.spec(RuffDurations.KURZ)) + expandVertically(motion.spec()),
            exit =
                if (motion.reduced) {
                    ExitTransition.None
                } else {
                    fadeOut(motion.spec(RuffDurations.KURZ)) + shrinkVertically(motion.spec())
                },
        ) {
            Text(
                "Begründung: ${begruendung.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun FeldWert(
    feld: ProfilFeld,
    angabe: Angabe<*>,
    profile: CreatureProfile,
) {
    val colors = MaterialTheme.colorScheme
    val body = MaterialTheme.typography.bodyLarge
    when (feld) {
        ProfilFeld.ALLERGIEN -> {
            val liste = profile.allergien?.wert.orEmpty()
            if (liste.isEmpty()) {
                Text("keine bekannt", style = body, color = colors.onSurface)
            } else {
                liste.forEach { a ->
                    Text("• ${AkteFormat.allergie(a)}", style = body, color = colors.onSurface)
                    if (a.reaktion.isNotBlank()) {
                        Text(
                            a.reaktion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(start = 14.dp, bottom = 2.dp),
                        )
                    }
                }
            }
        }
        ProfilFeld.VERFASSUNG -> {
            val v = profile.verfassung?.wert ?: return
            Text(v.stufe.label, style = body, color = Pergament)
            if (v.text.isNotBlank()) Text(v.text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
        ProfilFeld.IQ, ProfilFeld.EQ -> {
            val spanne = (if (feld == ProfilFeld.IQ) profile.iq else profile.eq)?.wert ?: return
            val text =
                if (feld == ProfilFeld.IQ) {
                    "${AkteFormat.ganzzahl(spanne)} · ${AkteFormat.iqBand(spanne)}"
                } else {
                    AkteFormat.ganzzahl(spanne)
                }
            Text(text, style = body, color = colors.onSurface)
            KognitionsBalken(spanne, "${feld.label}: $text")
        }
        ProfilFeld.WAHNHAFT -> {
            val wahn = profile.wahnhaft?.wert ?: return
            Text(
                AkteFormat.wahnhaft(wahn),
                style = body,
                // Höllenrot selbst ist auf Schwarz zu dunkel für Text (WCAG); daher das helle Rot.
                color = if (wahn) colors.primary else colors.onSurface,
                fontWeight = if (wahn) FontWeight.Bold else null,
            )
        }
        else -> Text(AkteFormat.feld(feld, profile) ?: AkteFormat.kurz(angabe), style = body, color = colors.onSurface)
    }
}

/** Spanne auf einer Skala 0–200 mit Markierung bei 100 (menschliches Mittel). */
@Composable
private fun KognitionsBalken(
    spanne: GanzzahlSpanne,
    beschreibung: String,
) {
    val motion = currentMotion
    var gestartet by remember { mutableStateOf(motion.reduced) }
    LaunchedEffect(Unit) { gestartet = true }
    val anteil by animateFloatAsState(
        targetValue = if (gestartet) 1f else 0f,
        animationSpec = motion.spec(RuffDurations.LANG),
        label = "kognition",
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val mitte = MaterialTheme.colorScheme.outline
    val lo = (spanne.min / KOGNITION_SKALA).coerceIn(0f, 1f)
    val hi = (spanne.max / KOGNITION_SKALA).coerceIn(0f, 1f)
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(8.dp)
                .semantics { contentDescription = beschreibung },
    ) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        val start = size.width * lo * anteil
        val breite = (size.width * (hi - lo) * anteil).coerceAtLeast(size.height)
        drawRoundRect(
            Glut,
            topLeft = Offset(start, 0f),
            size = Size(breite.coerceAtMost(size.width - start), size.height),
            cornerRadius = r,
        )
        val x = size.width * (100f / KOGNITION_SKALA)
        drawLine(mitte, Offset(x, -2f), Offset(x, size.height + 2f), strokeWidth = 2f)
    }
}

/** Kleines Abzeichen „abgeleitet“ / „bestätigt“ / „manuell“. */
@Composable
internal fun HerkunftBadge(
    herkunft: Herkunft,
    modifier: Modifier = Modifier,
) {
    val farbe: Color =
        when (herkunft) {
            Herkunft.ABGELEITET -> Glut
            Herkunft.VON_FYNN_BESTAETIGT -> MaterialTheme.colorScheme.tertiary
            Herkunft.MANUELL -> Pergament
        }
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, farbe.copy(alpha = 0.8f)),
        modifier = modifier.semantics { contentDescription = "Herkunft: ${herkunft.label}" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(herkunft.label, style = MaterialTheme.typography.labelSmall, color = farbe)
        }
    }
}
