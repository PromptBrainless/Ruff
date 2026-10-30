package de.ruff.dimension.data

/**
 * Ein Wesen aus `assets/creatures.json`.
 *
 * Alle Felder haben Standardwerte, damit Gson fehlende Schlüssel toleriert.
 * `id`, `source`, `status` und `spielbar` stehen bereits im JSON und werden
 * hier nur zusätzlich gelesen; die App-Oberfläche nutzt sie noch nicht.
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
    val id: String = "",
    val source: String = "",
    val status: String = "",
    val spielbar: Boolean? = null,
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
