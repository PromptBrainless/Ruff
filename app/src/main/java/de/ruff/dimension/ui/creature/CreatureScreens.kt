package de.ruff.dimension.ui.creature

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.ruff.dimension.data.Creature
import de.ruff.dimension.domain.powerLevelOptions
import de.ruff.dimension.domain.searchCreatures
import de.ruff.dimension.ui.LibraryUiState
import de.ruff.dimension.ui.common.AvatarFrame
import de.ruff.dimension.ui.common.ChainDivider
import de.ruff.dimension.ui.common.EmptyState
import de.ruff.dimension.ui.common.PortraitFrame
import de.ruff.dimension.ui.common.RuffDurations
import de.ruff.dimension.ui.common.RuffTopBar
import de.ruff.dimension.ui.common.SearchField
import de.ruff.dimension.ui.common.SectionHeader
import de.ruff.dimension.ui.common.creatureImageKey
import de.ruff.dimension.ui.common.creaturePortraitId
import de.ruff.dimension.ui.common.currentMotion
import de.ruff.dimension.ui.common.drawableId
import de.ruff.dimension.ui.common.emberButtonColors
import de.ruff.dimension.ui.common.ruffSharedBounds
import de.ruff.dimension.ui.editor.CreatureEditor
import de.ruff.dimension.ui.theme.Glut
import de.ruff.dimension.ui.theme.Hoellenrot
import de.ruff.dimension.ui.theme.Pergament

/** Wesen-Liste mit Suchfeld und Machtstufen-Filter (Logik unverändert aus der Bibliothek). */
@Composable
internal fun CreatureListPane(
    state: LibraryUiState,
    selectedId: String?,
    onQueryChange: (String) -> Unit,
    onPowerFilterChange: (Int?) -> Unit,
    onCreatureClick: (Creature) -> Unit,
    modifier: Modifier = Modifier,
) {
    val creatures = state.creatures
    val filtered =
        remember(creatures, state.query, state.powerFilter, state.searchHits) {
            searchCreatures(creatures, state.query, state.powerFilter, state.searchHits)
        }
    Column(modifier = modifier.fillMaxSize()) {
        RuffTopBar(title = "Wesen", showBack = true)
        Column(Modifier.padding(horizontal = 16.dp)) {
            SearchField(query = state.query, onQueryChange = onQueryChange, label = "Wesen suchen")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PowerLevelMenu(creatures = creatures, selected = state.powerFilter, onSelected = onPowerFilterChange)
                Spacer(Modifier.weight(1f))
                Text(
                    "${filtered.size} von ${creatures.size} Wesen",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (filtered.isEmpty()) {
            EmptyState("Keine Wesen gefunden. Suchbegriff oder Machtstufe ändern.", Modifier.padding(horizontal = 16.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("creature_list"),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
            ) {
                items(filtered, key = { it.id.ifBlank { it.name } }) { creature ->
                    CreatureCard(creature = creature, selected = creature.id == selectedId, onClick = { onCreatureClick(creature) })
                }
            }
        }
    }
}

@Composable
private fun PowerLevelMenu(
    creatures: List<Creature>,
    selected: Int?,
    onSelected: (Int?) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val levels = remember(creatures) { powerLevelOptions(creatures) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected?.let { "Machtstufe $it" } ?: "Alle Machtstufen")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Alle Machtstufen") },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            levels.forEach { creature ->
                DropdownMenuItem(
                    text = { Text(creature.powerLabel.substringBefore("(").trim()) },
                    onClick = {
                        onSelected(creature.powerLevel)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Karte eines Wesens: Icon im Eisenring (sonst Platzhalter), Name, Archetyp, Kurztext, Stufe.
 *
 * Bewegung: Beim Drücken sinkt die Karte leicht ein (Skalierung nur im Grafik-Layer,
 * kein neues Layout) und der Rand glüht in Glut auf; die ausgewählte Karte trägt
 * einen ruhigen Höllenrot-Rand. Im reduzierten Modus wechseln die Werte sofort.
 */
@Composable
internal fun CreatureCard(
    creature: Creature,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val motion = currentMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !motion.reduced) PRESSED_SCALE else 1f,
        animationSpec = motion.spec(RuffDurations.KURZ),
        label = "karte-druck",
    )
    val borderColor by animateColorAsState(
        targetValue =
            when {
                pressed -> Glut
                selected -> Hoellenrot
                else -> colors.outlineVariant.copy(alpha = 0.35f)
            },
        animationSpec = motion.spec(RuffDurations.MITTEL),
        label = "karte-glut-rand",
    )
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clip(CardDefaults.shape)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClickLabel = "${creature.name} öffnen",
                    onClick = onClick,
                ),
        colors =
            CardDefaults.cardColors(
                containerColor = if (selected) colors.secondaryContainer else colors.surfaceContainerHigh,
            ),
        border = BorderStroke(if (pressed || selected) 1.5.dp else 1.dp, borderColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarFrame(
                iconRes = drawableId(creature.icon).takeIf { it != 0 },
                modifier = Modifier.ruffSharedBounds(creatureImageKey(creature.id)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(creature.name, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                if (creature.archetype.isNotBlank()) {
                    Text(creature.archetype, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    creature.description,
                    color = colors.onSurface.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (creature.powerLevel > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "ST ${creature.powerLevel}",
                    color = colors.secondary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * Detailseite eines Wesens: Cover (falls vorhanden), Porträt im Runenrahmen, Abschnitte
 * auf der Titelleiste, Ketten-Trenner; Editor wie bisher über „Inhalt bearbeiten“.
 */
@Composable
internal fun CreatureDetailPane(
    creature: Creature?,
    state: LibraryUiState,
    showBack: Boolean,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: (Creature) -> Unit,
    onRestore: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        RuffTopBar(title = creature?.name ?: "Wesen", showBack = showBack)
        when {
            creature == null ->
                EmptyState("Wähle links ein Wesen aus.", Modifier.padding(16.dp))
            state.editing ->
                CreatureEditor(
                    creature = creature,
                    error = state.editorError,
                    onCancel = onCancelEdit,
                    onSave = onSave,
                    onRestore = { onRestore(creature.id) },
                )
            else -> CreatureDetails(creature, state.canEdit, state.editorError, onEdit)
        }
    }
}

@Composable
private fun CreatureDetails(
    creature: Creature,
    canEdit: Boolean,
    error: String?,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .testTag("creature_detail"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val coverId = drawableId(creature.cover)
        if (coverId != 0) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp)),
            ) {
                Image(
                    painter = painterResource(coverId),
                    contentDescription = "Szene: ${creature.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background))),
                )
            }
        }
        val portraitId = creaturePortraitId(creature)
        PortraitFrame(
            imageRes = portraitId,
            contentDescription = if (portraitId != null) "Bild: ${creature.name}" else "Platzhalter: noch kein Bild",
            maxWidth = 260.dp,
            modifier = Modifier.ruffSharedBounds(creatureImageKey(creature.id)),
        )
        Text(
            creature.name,
            style = MaterialTheme.typography.headlineSmall,
            color = Pergament,
            textAlign = TextAlign.Center,
        )
        Text(
            creature.powerLabel.ifBlank { "Machtstufe nicht angegeben" },
            color = colors.secondary,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        if (creature.archetype.isNotBlank()) {
            Text("Archetyp · ${creature.archetype}", color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        ChainDivider(Modifier.padding(vertical = 4.dp), maxWidth = 280.dp)
        SectionHeader("Beschreibung")
        Text(
            creature.description,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
        )
        creature.fields.forEach { (label, value) ->
            if (value.isNotBlank() && label != "Machtstufe" && label != "Archetyp") {
                SectionHeader(label, Modifier.padding(top = 6.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
                )
            }
        }
        error?.let { Text(it, color = colors.error, modifier = Modifier.padding(vertical = 8.dp)) }
        if (canEdit) {
            ChainDivider(Modifier.padding(vertical = 4.dp), maxWidth = 220.dp)
            Button(onClick = onEdit, colors = emberButtonColors(), modifier = Modifier.fillMaxWidth()) {
                Text("Inhalt bearbeiten")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Leichtes Einsinken beim Drücken; klein genug, dass Nachbarkarten nicht überdeckt werden. */
private const val PRESSED_SCALE = 0.975f
