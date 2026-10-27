package de.ruff.dimension

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val Soot = Color(0xFF16171A)
private val Iron = Color(0xFF25282D)
private val Ember = Color(0xFFB3201B)
private val Flame = Color(0xFFE0662A)
private val Parchment = Color(0xFFE4E6E3)
private val Storm = Color(0xFF9AA3AC)

private val RuffColors = darkColorScheme(
    primary = Ember,
    secondary = Flame,
    background = Soot,
    surface = Iron,
    onBackground = Parchment,
    onSurface = Parchment,
)

private data class Creature(
    val name: String = "",
    val powerLevel: Int = 0,
    val powerLabel: String = "",
    val description: String = "",
    val archetype: String = "",
    val fields: Map<String, String> = emptyMap(),
)

private data class RulePage(
    val title: String = "",
    val source: String = "",
    val fields: Map<String, String> = emptyMap(),
    val blocks: List<RuleBlock> = emptyList(),
)

private data class RuleBlock(
    val type: String = "",
    val text: String = "",
)

private enum class LibraryTab(val label: String) {
    CREATURES("Wesen"),
    RULES("Regeln"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gson = Gson()
        val creaturesType = object : TypeToken<List<Creature>>() {}.type
        val rulesType = object : TypeToken<List<RulePage>>() {}.type
        val creatures = assets.open("creatures.json").bufferedReader().use {
            gson.fromJson<List<Creature>>(it, creaturesType)
        }
        val rules = assets.open("rules.json").bufferedReader().use {
            gson.fromJson<List<RulePage>>(it, rulesType)
        }

        window.statusBarColor = android.graphics.Color.rgb(22, 23, 26)
        window.navigationBarColor = android.graphics.Color.rgb(22, 23, 26)
        setContent {
            MaterialTheme(colorScheme = RuffColors) {
                RuffApp(creatures = creatures, rules = rules)
            }
        }
    }
}

@Composable
private fun RuffApp(creatures: List<Creature>, rules: List<RulePage>) {
    var tab by remember { mutableStateOf(LibraryTab.CREATURES) }
    var query by remember { mutableStateOf("") }
    var powerFilter by remember { mutableStateOf<Int?>(null) }
    var selectedCreature by remember { mutableStateOf<Creature?>(null) }
    var selectedRule by remember { mutableStateOf<RulePage?>(null) }

    Scaffold(
        topBar = {
            Header(
                title = selectedCreature?.name ?: selectedRule?.title ?: "Ruff",
                showBack = selectedCreature != null || selectedRule != null,
                onBack = {
                    selectedCreature = null
                    selectedRule = null
                },
            )
        },
        containerColor = Soot,
    ) { padding ->
        when {
            selectedCreature != null -> CreatureDetails(
                creature = selectedCreature!!,
                modifier = Modifier.padding(padding),
            )
            selectedRule != null -> RuleDetails(
                rule = selectedRule!!,
                modifier = Modifier.padding(padding),
            )
            else -> Library(
                creatures = creatures,
                rules = rules,
                tab = tab,
                query = query,
                powerFilter = powerFilter,
                onTabChange = {
                    tab = it
                    powerFilter = null
                },
                onQueryChange = { query = it },
                onPowerFilterChange = { powerFilter = it },
                onCreatureClick = { selectedCreature = it },
                onRuleClick = { selectedRule = it },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun Header(title: String, showBack: Boolean, onBack: () -> Unit) {
    Surface(color = Soot, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
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
    onTabChange: (LibraryTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onPowerFilterChange: (Int?) -> Unit,
    onCreatureClick: (Creature) -> Unit,
    onRuleClick: (RulePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedQuery = query.trim().lowercase()
    val filteredCreatures = remember(creatures, normalizedQuery, powerFilter) {
        creatures.filter { creature ->
            (powerFilter == null || creature.powerLevel == powerFilter) &&
                (normalizedQuery.isEmpty() ||
                    creature.name.lowercase().contains(normalizedQuery) ||
                    creature.archetype.lowercase().contains(normalizedQuery) ||
                    creature.fields.values.any { it.lowercase().contains(normalizedQuery) })
        }
    }
    val filteredRules = remember(rules, normalizedQuery) {
        rules.filter { rule ->
            normalizedQuery.isEmpty() ||
                rule.title.lowercase().contains(normalizedQuery) ||
                rule.blocks.any { it.text.lowercase().contains(normalizedQuery) }
        }
    }

    Column(
        modifier = modifier
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
        modifier = Modifier
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
            modifier = Modifier
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
    val levels = creatures
        .filter { it.powerLevel > 0 }
        .distinctBy { it.powerLevel }
        .sortedBy { it.powerLevel }

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
private fun CreatureCard(creature: Creature, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Iron),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ruff_glyph),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
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
private fun RuleCard(rule: RulePage, onClick: () -> Unit) {
    val summary = rule.blocks.firstOrNull { it.type == "p" || it.type == "li" }?.text.orEmpty()
    Card(
        modifier = Modifier
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
private fun CreatureDetails(creature: Creature, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
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
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RuleDetails(rule: RulePage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
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
                        style = if (block.type == "h2") {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                    )
                }
                "li", "row" -> Text(
                    "•  ${block.text}",
                    color = Parchment,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                else -> Text(
                    block.text,
                    color = Parchment.copy(alpha = 0.9f),
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Card(
        modifier = Modifier
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        color = Iron,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(message, color = Storm, modifier = Modifier.padding(18.dp))
    }
}
