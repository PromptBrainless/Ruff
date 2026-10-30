package de.ruff.dimension.data

/**
 * Ein Wesen aus `assets/creatures.json`.
 *
 * Alle Felder haben Standardwerte, damit Gson fehlende Schlüssel toleriert.
 */
internal data class Creature(
    val name: String = "",
    val powerLevel: Int = 0,
    val powerLabel: String = "",
    val description: String = "",
    val archetype: String = "",
    val fields: Map<String, String> = emptyMap(),
    val portrait: String = "",
    val icon: String = "",
    val cover: String = "",
)

/** Eine Regel-Wiki-Seite aus `assets/rules.json`. */
internal data class RulePage(
    val title: String = "",
    val source: String = "",
    val fields: Map<String, String> = emptyMap(),
    val blocks: List<RuleBlock> = emptyList(),
)

/** Ein Inhaltsblock einer Regelseite, etwa `p`, `li`, `h2` oder `row`. */
internal data class RuleBlock(
    val type: String = "",
    val text: String = "",
)
