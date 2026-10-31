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
    val presentation: ContentPresentation = ContentPresentation(),
)

/** Eine Regel-Wiki-Seite aus `assets/rules.json`. */
internal data class RulePage(
    val title: String = "",
    val source: String = "",
    val fields: Map<String, String> = emptyMap(),
    val blocks: List<RuleBlock> = emptyList(),
    val presentation: ContentPresentation = ContentPresentation(),
)

/** Ein Inhaltsblock einer Regelseite, etwa `p`, `li`, `h2` oder `row`. */
internal data class RuleBlock(
    val type: String = "",
    val text: String = "",
)

/** Medien und Gestaltung eines Inhalts; bleibt von den unveränderten Seed-Assets getrennt. */
internal data class ContentPresentation(
    val media: List<MediaAttachment> = emptyList(),
    val backgroundPath: String = "",
    val frameColor: String = "#B3201B",
    val frameWidthDp: Float = 1f,
    val cornerRadiusDp: Float = 14f,
    val fontPath: String = "",
)

internal data class MediaAttachment(
    val path: String,
    val mimeType: String,
    val displayName: String,
)

/** Globale Darstellung der App, lokal und unabhängig von Seed-Inhalten gespeichert. */
internal data class AdminAppearance(
    val backgroundPath: String = "",
    val accentColor: String = "#B3201B",
    val frameColor: String = "#8B5A3C",
    val frameWidthDp: Float = 1f,
    val cornerRadiusDp: Float = 14f,
    val fontPath: String = "",
)
