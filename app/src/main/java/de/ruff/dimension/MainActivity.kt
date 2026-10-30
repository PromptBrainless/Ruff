package de.ruff.dimension

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.repository.SearchHits
import de.ruff.dimension.domain.powerLevelOptions
import de.ruff.dimension.domain.searchCreatures
import de.ruff.dimension.domain.searchRules
import de.ruff.dimension.ui.LibraryTab
import de.ruff.dimension.ui.LibraryUiState
import de.ruff.dimension.ui.LibraryViewModel
import de.ruff.dimension.ui.LoadState

private val Soot = Color(0xFF16171A)
private val Iron = Color(0xFF25282D)
private val Ember = Color(0xFFB3201B)
private val Flame = Color(0xFFE0662A)
private val Parchment = Color(0xFFE4E6E3)
private val Storm = Color(0xFF9AA3AC)

private val RuffColors =
    darkColorScheme(
        primary = Ember,
        secondary = Flame,
        background = Soot,
        surface = Iron,
        onBackground = Parchment,
        onSurface = Parchment,
    )

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(22, 23, 26)
        window.navigationBarColor = android.graphics.Color.rgb(22, 23, 26)
        setContent {
            MaterialTheme(colorScheme = RuffColors) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                RuffApp(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun RuffApp(
    state: LibraryUiState,
    viewModel: LibraryViewModel,
) {
    val selectedCreature = state.selectedCreature
    val selectedRule = state.selectedRule

    Scaffold(
        topBar = {
            Header(
                title = selectedCreature?.name ?: selectedRule?.title ?: "Ruff",
                showBack = selectedCreature != null || selectedRule != null,
                onBack = viewModel::closeDetails,
            )
        },
        containerColor = Soot,
    ) { padding ->
        when {
            state.load is LoadState.Loading ->
                LoadingState(modifier = Modifier.padding(padding))
            state.load is LoadState.Error ->
                ErrorState(
                    message = state.load.message,
                    onRetry = viewModel::reload,
                    modifier = Modifier.padding(padding),
                )
            selectedCreature != null ->
                CreatureDetails(
                    creature = selectedCreature,
                    editing = state.editing,
                    canEdit = state.canEdit,
                    error = state.editorError,
                    onEdit = { viewModel.setEditing(true) },
                    onCancelEdit = { viewModel.setEditing(false) },
                    onSave = viewModel::saveCreature,
                    onRestore = { viewModel.restoreCreature(selectedCreature.id) },
                    modifier = Modifier.padding(padding),
                )
            selectedRule != null ->
                RuleDetails(
                    rule = selectedRule,
                    editing = state.editing,
                    canEdit = state.canEdit,
                    error = state.editorError,
                    onEdit = { viewModel.setEditing(true) },
                    onCancelEdit = { viewModel.setEditing(false) },
                    onSave = viewModel::saveRule,
                    onRestore = { viewModel.restoreRule(selectedRule.source) },
                    modifier = Modifier.padding(padding),
                )
            else ->
                Library(
                    creatures = state.creatures,
                    rules = state.rules,
                    tab = state.tab,
                    query = state.query,
                    powerFilter = state.powerFilter,
                    searchHits = state.searchHits,
                    onTabChange = viewModel::selectTab,
                    onQueryChange = viewModel::setQuery,
                    onPowerFilterChange = viewModel::setPowerFilter,
                    onCreatureClick = viewModel::openCreature,
                    onRuleClick = viewModel::openRule,
                    modifier = Modifier.padding(padding),
                )
        }
    }
}

@Composable
private fun Header(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
) {
    Surface(color = Soot, tonalElevation = 2.dp) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(68.dp)
                    .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Iron),
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    Text("Zurück")
                }
            } else {
                Image(
                    painter = painterResource(R.drawable.ruff_glyph),
                    contentDescription = "Ruff-Glyphe",
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Parchment,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!showBack) {
                    Text("DIMENSION · REGELWERK & WESEN", color = Storm, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun Library(
    creatures: List<Creature>,
    rules: List<RulePage>,
    tab: LibraryTab,
    query: String,
    powerFilter: Int?,
    searchHits: SearchHits?,
    onTabChange: (LibraryTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onPowerFilterChange: (Int?) -> Unit,
    onCreatureClick: (Creature) -> Unit,
    onRuleClick: (RulePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredCreatures =
        remember(creatures, query, powerFilter, searchHits) { searchCreatures(creatures, query, powerFilter, searchHits) }
    val filteredRules = remember(rules, query, searchHits) { searchRules(rules, query, searchHits) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        SectionBanner()
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LibraryTab.entries.forEach { option ->
                FilterChip(
                    selected = tab == option,
                    onClick = { onTabChange(option) },
                    label = { Text(option.label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (tab == LibraryTab.CREATURES) "Wesen suchen" else "Regeln suchen") },
            singleLine = true,
        )
        Spacer(Modifier.height(10.dp))
        if (tab == LibraryTab.CREATURES) {
            PowerLevelMenu(
                creatures = creatures,
                selected = powerFilter,
                onSelected = onPowerFilterChange,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${filteredCreatures.size} von ${creatures.size} Wesen",
                color = Storm,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            if (filteredCreatures.isEmpty()) {
                EmptyState("Keine Wesen gefunden. Suchbegriff oder Machtstufe ändern.")
            } else {
                filteredCreatures.forEach { creature ->
                    CreatureCard(creature = creature, onClick = { onCreatureClick(creature) })
                }
            }
        } else {
            Text(
                "${filteredRules.size} Regel-Dokumente",
                color = Storm,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            if (filteredRules.isEmpty()) {
                EmptyState("Keine Regel-Dokumente gefunden.")
            } else {
                filteredRules.forEach { rule ->
                    RuleCard(rule = rule, onClick = { onRuleClick(rule) })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionBanner() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(14.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.ruff_article_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Soot.copy(alpha = 0.3f))
                    .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ruff_default_npc),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text("DIE SCHWELLENLANDE", color = Flame, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ein Archiv für Ruff",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Regeln, Kräfte und Wesen aus den Exportdaten", color = Storm, fontSize = 12.sp)
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
    var expanded by remember { mutableStateOf(false) }
    val levels = remember(creatures) { powerLevelOptions(creatures) }

    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = Iron),
        ) {
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

@Composable
private fun CreatureCard(
    creature: Creature,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Iron),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconId = drawableId(creature.icon)
            if (iconId != 0) {
                Image(
                    painter = painterResource(iconId),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ruff_glyph),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(creature.name, fontWeight = FontWeight.SemiBold)
                if (creature.archetype.isNotBlank()) {
                    Text(creature.archetype, color = Storm, fontSize = 12.sp)
                }
                Text(
                    creature.description,
                    color = Parchment.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (creature.powerLevel > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "ST ${creature.powerLevel}",
                    color = Flame,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: RulePage,
    onClick: () -> Unit,
) {
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
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Iron),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(rule.title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                summary.ifBlank { rule.source },
                color = Storm,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CreatureDetails(
    creature: Creature,
    editing: Boolean,
    canEdit: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: (Creature) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (editing) {
        CreatureEditor(
            creature = creature,
            error = error,
            onCancel = onCancelEdit,
            onSave = onSave,
            onRestore = onRestore,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
    ) {
        CreatureImage(creature)
        Card(colors = CardDefaults.cardColors(containerColor = Iron)) {
            Column(Modifier.padding(18.dp)) {
                Text(creature.powerLabel.ifBlank { "Machtstufe nicht angegeben" }, color = Flame)
                Spacer(Modifier.height(8.dp))
                Text(creature.description, style = MaterialTheme.typography.bodyLarge)
                if (creature.archetype.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Archetyp · ${creature.archetype}", color = Storm)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        creature.fields.forEach { (label, value) ->
            if (value.isNotBlank() && label != "Machtstufe" && label != "Archetyp") {
                DetailField(label = label, value = value)
            }
        }
        error?.let { Text(it, color = Flame, modifier = Modifier.padding(vertical = 8.dp)) }
        if (canEdit) {
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = Ember),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Inhalt bearbeiten")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@SuppressLint("DiscouragedApi")
@Composable
private fun drawableId(resourceName: String): Int {
    val context = LocalContext.current
    return remember(resourceName) {
        if (resourceName.isBlank()) {
            0
        } else {
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        }
    }
}

@Composable
private fun CreatureImage(creature: Creature) {
    val imageId = drawableId(creature.cover.ifBlank { creature.portrait })
    Image(
        painter = painterResource(if (imageId != 0) imageId else R.drawable.ruff_platzhalter_kapuze),
        contentDescription = if (imageId != 0) "Bild: ${creature.name}" else "Platzhalter: noch kein Bild",
        contentScale = if (imageId != 0) ContentScale.Crop else ContentScale.Fit,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(if (creature.cover.isNotBlank()) 200.dp else 260.dp)
                .clip(RoundedCornerShape(14.dp)),
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun RuleDetails(
    rule: RulePage,
    editing: Boolean,
    canEdit: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: (RulePage) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (editing) {
        RuleEditor(
            rule = rule,
            error = error,
            onCancel = onCancelEdit,
            onSave = onSave,
            onRestore = onRestore,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
    ) {
        Text(rule.source, color = Storm, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(12.dp))
        rule.blocks.forEach { block ->
            when (block.type) {
                "h2", "h3", "h4" -> {
                    Text(
                        block.text,
                        color = Flame,
                        fontWeight = FontWeight.Bold,
                        style =
                            if (block.type == "h2") {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleSmall
                            },
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                    )
                }
                "li", "row" ->
                    Text(
                        "•  ${block.text}",
                        color = Parchment,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                else ->
                    Text(
                        block.text,
                        color = Parchment.copy(alpha = 0.9f),
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
            }
        }
        error?.let { Text(it, color = Flame, modifier = Modifier.padding(vertical = 8.dp)) }
        if (canEdit) {
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = Ember),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Inhalt bearbeiten")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CreatureEditor(
    creature: Creature,
    error: String?,
    onCancel: () -> Unit,
    onSave: (Creature) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(creature.id) { mutableStateOf(creature.name) }
    var description by remember(creature.id) { mutableStateOf(creature.description) }
    var archetype by remember(creature.id) { mutableStateOf(creature.archetype) }
    var powerLabel by remember(creature.id) { mutableStateOf(creature.powerLabel) }
    var powerLevel by remember(creature.id) { mutableStateOf(creature.powerLevel.toString()) }
    var fields by remember(creature.id) { mutableStateOf(creature.fields) }
    var restoreConfirmation by remember(creature.id) { mutableStateOf(false) }
    val parsedPowerLevel = powerLevel.toIntOrNull()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
    ) {
        Text("Wesen bearbeiten", style = MaterialTheme.typography.titleLarge, color = Flame)
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
        error?.let { Text(it, color = Flame, modifier = Modifier.padding(vertical = 8.dp)) }
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
                colors = ButtonDefaults.buttonColors(containerColor = Ember),
            ) {
                Text("Speichern")
            }
            TextButton(onClick = onCancel) { Text("Abbrechen") }
        }
        TextButton(onClick = { restoreConfirmation = true }) { Text("Seed-Original wiederherstellen") }
    }
    if (restoreConfirmation) {
        AlertDialog(
            onDismissRequest = { restoreConfirmation = false },
            title = { Text("Original wiederherstellen?") },
            text = { Text("Die lokale Bearbeitung wird verworfen. Die Seed-Daten bleiben unverändert.") },
            confirmButton = {
                TextButton(onClick = {
                    restoreConfirmation = false
                    onRestore()
                }) { Text("Wiederherstellen") }
            },
            dismissButton = {
                TextButton(onClick = { restoreConfirmation = false }) { Text("Abbrechen") }
            },
        )
    }
}

@Composable
private fun RuleEditor(
    rule: RulePage,
    error: String?,
    onCancel: () -> Unit,
    onSave: (RulePage) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember(rule.source) { mutableStateOf(rule.title) }
    var blocks by remember(rule.source) { mutableStateOf(rule.blocks) }
    var restoreConfirmation by remember(rule.source) { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Regelseite bearbeiten", style = MaterialTheme.typography.titleLarge, color = Flame)
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
            error?.let { Text(it, color = Flame, modifier = Modifier.padding(vertical = 8.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSave(rule.copy(title = title, blocks = blocks)) },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Ember),
                ) {
                    Text("Speichern")
                }
                TextButton(onClick = onCancel) { Text("Abbrechen") }
            }
            TextButton(onClick = { restoreConfirmation = true }) { Text("Seed-Original wiederherstellen") }
        }
    }
    if (restoreConfirmation) {
        AlertDialog(
            onDismissRequest = { restoreConfirmation = false },
            title = { Text("Original wiederherstellen?") },
            text = { Text("Die lokale Bearbeitung wird verworfen. Die Seed-Daten bleiben unverändert.") },
            confirmButton = {
                TextButton(onClick = {
                    restoreConfirmation = false
                    onRestore()
                }) { Text("Wiederherstellen") }
            },
            dismissButton = {
                TextButton(onClick = { restoreConfirmation = false }) { Text("Abbrechen") }
            },
        )
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = Iron),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label.uppercase(), color = Flame, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(value, color = Parchment)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        color = Iron,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(message, color = Storm, modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Flame)
        Spacer(Modifier.height(12.dp))
        Text("Archiv wird vorbereitet …", color = Storm)
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(message)
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Iron),
        ) {
            Text("Erneut versuchen")
        }
    }
}
