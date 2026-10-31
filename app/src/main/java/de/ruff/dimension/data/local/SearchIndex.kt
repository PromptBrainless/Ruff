package de.ruff.dimension.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Gemeinsamer FTS5-Volltextindex für Wesen und Regelseiten.
 *
 * Room kennt die virtuelle Tabelle nicht (Room 2.7 hat nur FTS3/FTS4-Entities),
 * daher wird sie per `execSQL` angelegt: in [MIGRATION_1_2] für bestehende
 * Installationen und in [RuffDatabase.CALLBACK] für Neuinstallationen.
 *
 * Spalten: `type` (`creature`/`rule`) und `ref_id` (Wesen-`id` bzw. Seiten-`id`)
 * sind nicht indiziert; durchsucht werden `title`, `subtitle` und `body`.
 * - Wesen: Name, Archetyp, alle Feldwerte (dieselben Felder wie die bisherige Suche).
 * - Regelseiten: Titel, leer, alle Blocktexte.
 *
 * Akten (`creature_profiles`) schreiben eigene Zeilen vom Typ
 * [ProfileSearch.TYPE] mit eigenen Triggern; [REBUILD] lässt diese stehen.
 *
 * Trigger halten den Index bei Insert, Update und Delete synchron (auch bei
 * dem Abgleich im Seed). Nach dem Seed und in der Migration wird er zusätzlich
 * vollständig neu aufgebaut ([REBUILD]).
 */
internal object SearchIndex {
    const val TABLE = "search_index"
    const val TYPE_CREATURE = "creature"
    const val TYPE_RULE = "rule"

    /** bm25-Gewichte je Spalte: type, ref_id, title, subtitle, body. */
    const val BM25_WEIGHTS = "0.0, 0.0, 10.0, 5.0, 1.0"

    private const val CREATURE_BODY = "(SELECT group_concat(value, ' ') FROM json_each(%s.fields))"
    private const val RULE_BODY = "(SELECT group_concat(b.text, ' ') FROM rule_blocks b WHERE b.pageId = %s)"

    private fun insertCreature(row: String) =
        "INSERT INTO $TABLE(type, ref_id, title, subtitle, body) " +
            "VALUES('$TYPE_CREATURE', $row.id, $row.name, $row.archetype, ${CREATURE_BODY.format(row)});"

    private fun deleteRef(
        type: String,
        refId: String,
    ) = "DELETE FROM $TABLE WHERE type = '$type' AND ref_id = $refId;"

    private fun insertRule(pageId: String) =
        "INSERT INTO $TABLE(type, ref_id, title, subtitle, body) " +
            "SELECT '$TYPE_RULE', p.id, p.title, '', ${RULE_BODY.format("p.id")} FROM rule_pages p WHERE p.id = $pageId;"

    private fun refreshRule(pageId: String) = deleteRef(TYPE_RULE, pageId) + " " + insertRule(pageId)

    val CREATE: List<String> =
        listOf(
            "CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE USING fts5(" +
                "type UNINDEXED, ref_id UNINDEXED, title, subtitle, body, " +
                "tokenize = 'unicode61 remove_diacritics 2')",
            "CREATE TRIGGER IF NOT EXISTS search_creatures_ai AFTER INSERT ON creatures BEGIN " +
                insertCreature("NEW") + " END",
            "CREATE TRIGGER IF NOT EXISTS search_creatures_au AFTER UPDATE ON creatures BEGIN " +
                deleteRef(TYPE_CREATURE, "OLD.id") + " " + insertCreature("NEW") + " END",
            "CREATE TRIGGER IF NOT EXISTS search_creatures_ad AFTER DELETE ON creatures BEGIN " +
                deleteRef(TYPE_CREATURE, "OLD.id") + " END",
            "CREATE TRIGGER IF NOT EXISTS search_rule_pages_ai AFTER INSERT ON rule_pages BEGIN " +
                refreshRule("NEW.id") + " END",
            "CREATE TRIGGER IF NOT EXISTS search_rule_pages_au AFTER UPDATE ON rule_pages BEGIN " +
                deleteRef(TYPE_RULE, "OLD.id") + " " + refreshRule("NEW.id") + " END",
            "CREATE TRIGGER IF NOT EXISTS search_rule_pages_ad AFTER DELETE ON rule_pages BEGIN " +
                deleteRef(TYPE_RULE, "OLD.id") + " END",
            "CREATE TRIGGER IF NOT EXISTS search_rule_blocks_ai AFTER INSERT ON rule_blocks BEGIN " +
                refreshRule("NEW.pageId") + " END",
            "CREATE TRIGGER IF NOT EXISTS search_rule_blocks_au AFTER UPDATE ON rule_blocks BEGIN " +
                refreshRule("OLD.pageId") + " " + refreshRule("NEW.pageId") + " END",
            "CREATE TRIGGER IF NOT EXISTS search_rule_blocks_ad AFTER DELETE ON rule_blocks BEGIN " +
                refreshRule("OLD.pageId") + " END",
        )

    /** Baut den Index vollständig aus den Inhaltstabellen neu auf. */
    val REBUILD: List<String> =
        listOf(
            // Nur eigene Typen löschen: Akten-Zeilen (ProfileSearch) bleiben beim Seed erhalten.
            "DELETE FROM $TABLE WHERE type IN ('$TYPE_CREATURE', '$TYPE_RULE')",
            "INSERT INTO $TABLE(type, ref_id, title, subtitle, body) " +
                "SELECT '$TYPE_CREATURE', c.id, c.name, c.archetype, ${CREATURE_BODY.format("c")} FROM creatures c",
            "INSERT INTO $TABLE(type, ref_id, title, subtitle, body) " +
                "SELECT '$TYPE_RULE', p.id, p.title, '', ${RULE_BODY.format("p.id")} FROM rule_pages p",
        )

    /** Volltextabfrage, sortiert nach bm25 (kleiner = besser). Parameter: FTS5-Ausdruck. */
    const val SEARCH_SQL =
        "SELECT type, ref_id FROM $TABLE WHERE $TABLE MATCH ? ORDER BY bm25($TABLE, $BM25_WEIGHTS), rowid"

    fun create(connection: SQLiteConnection) = CREATE.forEach { connection.execSQL(it) }

    fun rebuild(connection: SQLiteConnection) = REBUILD.forEach { connection.execSQL(it) }
}

/**
 * v1 → v2: legt den FTS5-Index samt Triggern an und füllt ihn aus den
 * vorhandenen Daten. Ändert und löscht keine Zeile der bestehenden Tabellen.
 */
internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            SearchIndex.create(connection)
            SearchIndex.rebuild(connection)
        }
    }
