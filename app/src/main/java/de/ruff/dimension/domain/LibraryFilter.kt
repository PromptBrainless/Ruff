package de.ruff.dimension.domain

import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.repository.SearchHits

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

/**
 * Suche mit Volltext-Ranking: zuerst die FTS-Treffer nach Relevanz, danach alle
 * weiteren Treffer der bisherigen Teilstring-Suche in der gewohnten Reihenfolge.
 * Es wird also nie weniger gefunden als bisher. Ohne passende [hits] (leere
 * Suche, FTS nicht verfügbar oder Ergebnis noch nicht da) gilt [filterCreatures].
 */
internal fun searchCreatures(
    creatures: List<Creature>,
    query: String,
    powerFilter: Int?,
    hits: SearchHits?,
): List<Creature> {
    val substring = filterCreatures(creatures, query, powerFilter)
    if (hits == null || hits.query != query || normalizeQuery(query).isEmpty()) return substring
    val byId = creatures.associateBy { it.id }
    val ranked =
        hits.creatureIds
            .mapNotNull { byId[it] }
            .filter { powerFilter == null || it.powerLevel == powerFilter }
    return (ranked + substring).distinctBy { it.id }
}

/** Wie [searchCreatures] für Regelseiten (ID = `source`). */
internal fun searchRules(
    rules: List<RulePage>,
    query: String,
    hits: SearchHits?,
): List<RulePage> {
    val substring = filterRules(rules, query)
    if (hits == null || hits.query != query || normalizeQuery(query).isEmpty()) return substring
    val bySource = rules.associateBy { it.source }
    return (hits.ruleIds.mapNotNull { bySource[it] } + substring).distinctBy { it.source }
}
