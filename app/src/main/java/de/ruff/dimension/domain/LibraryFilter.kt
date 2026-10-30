package de.ruff.dimension.domain

import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage

/*
 * Such- und Filterlogik der Bibliothek, unverändert aus MainActivity
 * übernommen: Teilstring-Suche ohne Groß-/Kleinschreibung über dieselben Felder.
 */

internal fun normalizeQuery(query: String): String = query.trim().lowercase()

/** Wesen nach Machtstufe ([powerFilter] `null` = alle) und Suchbegriff in Name, Archetyp und Feldwerten. */
internal fun filterCreatures(
    creatures: List<Creature>,
    query: String,
    powerFilter: Int?,
): List<Creature> {
    val normalizedQuery = normalizeQuery(query)
    return creatures.filter { creature ->
        (powerFilter == null || creature.powerLevel == powerFilter) &&
            (
                normalizedQuery.isEmpty() ||
                    creature.name.lowercase().contains(normalizedQuery) ||
                    creature.archetype.lowercase().contains(normalizedQuery) ||
                    creature.fields.values.any { it.lowercase().contains(normalizedQuery) }
            )
    }
}

/** Regelseiten nach Suchbegriff in Titel und Blocktexten. */
internal fun filterRules(
    rules: List<RulePage>,
    query: String,
): List<RulePage> {
    val normalizedQuery = normalizeQuery(query)
    return rules.filter { rule ->
        normalizedQuery.isEmpty() ||
            rule.title.lowercase().contains(normalizedQuery) ||
            rule.blocks.any { it.text.lowercase().contains(normalizedQuery) }
    }
}

/** Ein Vertreter je Machtstufe > 0, aufsteigend; liefert Stufe und Beschriftung fürs Menü. */
internal fun powerLevelOptions(creatures: List<Creature>): List<Creature> =
    creatures
        .filter { it.powerLevel > 0 }
        .distinctBy { it.powerLevel }
        .sortedBy { it.powerLevel }
