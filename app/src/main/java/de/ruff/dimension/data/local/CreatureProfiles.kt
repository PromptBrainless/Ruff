package de.ruff.dimension.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow

/**
 * Kreaturen-Akte eines Wesens (Steckbrief ohne Regelwerksbezug) – Nutzerdaten wie
 * [FavoriteEntity], strikt getrennt von den Seed-Tabellen.
 *
 * - [id] ist eine UUID, [creatureId] verweist ohne Fremdschlüssel auf `creatures.id`
 *   (eindeutig, eine Akte je Wesen). Der Inhalts-Seed fasst diese Tabelle nie an.
 * - Jeder Wert hat eine Spalte `…Begruendung` und `…Herkunft`
 *   (`ABGELEITET` / `VON_FYNN_BESTAETIGT` / `MANUELL`). Ist die Herkunft `NULL`,
 *   gibt es den Wert nicht. Herkunft gesetzt, Wert leer = bewusst geleert.
 * - Der Akten-Seed ([CreatureProfileSeeder]) ersetzt nur Werte mit Herkunft
 *   `NULL` oder `ABGELEITET`; bestätigte und manuelle Werte bleiben unangetastet.
 * - Entfernen ist ein Soft-Delete über [deletedAt]; jede Änderung setzt
 *   [updatedAt] und erhöht [revision] (für späteren Sync).
 * - Listen (Quellen, Allergien) liegen als JSON-Text vor.
 */
@Entity(
    tableName = "creature_profiles",
    indices = [Index(value = ["creatureId"], unique = true)],
)
internal data class CreatureProfileEntity(
    @PrimaryKey val id: String,
    val creatureId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
    val deletedAt: Long? = null,
    /** Version des Akten-Seeds, die zuletzt Werte geliefert hat; 0 = nie. */
    val seedVersion: Int = 0,
    /** JSON-Liste der Quellen der Ableitung. */
    val quellen: String = "[]",
    val erscheinung: String? = null,
    val erscheinungBegruendung: String? = null,
    val erscheinungHerkunft: String? = null,
    val lebensraum: String? = null,
    val lebensraumBegruendung: String? = null,
    val lebensraumHerkunft: String? = null,
    val verhalten: String? = null,
    val verhaltenBegruendung: String? = null,
    val verhaltenHerkunft: String? = null,
    val groesseMinM: Double? = null,
    val groesseMaxM: Double? = null,
    val groesseBegruendung: String? = null,
    val groesseHerkunft: String? = null,
    val gewichtMinKg: Double? = null,
    val gewichtMaxKg: Double? = null,
    val gewichtBegruendung: String? = null,
    val gewichtHerkunft: String? = null,
    val pulsMinBpm: Int? = null,
    val pulsMaxBpm: Int? = null,
    val pulsBegruendung: String? = null,
    val pulsHerkunft: String? = null,
    val blutdruckSystolisch: Int? = null,
    val blutdruckDiastolisch: Int? = null,
    val blutdruckHinweis: String? = null,
    val blutdruckBegruendung: String? = null,
    val blutdruckHerkunft: String? = null,
    /** JSON-Liste `[{ausloeser, art, schwere, reaktion}]`. */
    val allergien: String? = null,
    val allergienBegruendung: String? = null,
    val allergienHerkunft: String? = null,
    val verfassungStufe: String? = null,
    val verfassungText: String? = null,
    val verfassungBegruendung: String? = null,
    val verfassungHerkunft: String? = null,
    val iqMin: Int? = null,
    val iqMax: Int? = null,
    val iqBegruendung: String? = null,
    val iqHerkunft: String? = null,
    val eqMin: Int? = null,
    val eqMax: Int? = null,
    val eqBegruendung: String? = null,
    val eqHerkunft: String? = null,
    val wahnhaft: Boolean? = null,
    val wahnhaftBegruendung: String? = null,
    val wahnhaftHerkunft: String? = null,
) {
    val active: Boolean get() = deletedAt == null
}

@Dao
internal interface CreatureProfileDao {
    @Query("SELECT * FROM creature_profiles WHERE deletedAt IS NULL ORDER BY creatureId")
    fun observeActive(): Flow<List<CreatureProfileEntity>>

    @Query("SELECT * FROM creature_profiles WHERE creatureId = :creatureId")
    suspend fun getByCreatureId(creatureId: String): CreatureProfileEntity?

    @Query("SELECT * FROM creature_profiles ORDER BY creatureId")
    suspend fun getAll(): List<CreatureProfileEntity>

    @Query("SELECT COUNT(*) FROM creature_profiles WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: CreatureProfileEntity)

    @Update
    suspend fun update(profile: CreatureProfileEntity)
}

/**
 * v3 → v4: legt nur `creature_profiles` samt eindeutigem Index und die Such-Trigger
 * für Akten an (additiv). Wesen, Regeln, Favoriten, Suchindex und Bearbeitungen
 * bleiben unberührt. Das SQL entspricht exakt `schemas/…/4.json`.
 */
internal val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(CreatureProfilesSchema.CREATE_TABLE)
            connection.execSQL(CreatureProfilesSchema.CREATE_INDEX)
            ProfileSearch.create(connection)
            ProfileSearch.rebuild(connection)
        }
    }

internal object CreatureProfilesSchema {
    const val CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS `creature_profiles` (`id` TEXT NOT NULL, `creatureId` TEXT NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `revision` INTEGER NOT NULL, " +
            "`deletedAt` INTEGER, `seedVersion` INTEGER NOT NULL, `quellen` TEXT NOT NULL, " +
            "`erscheinung` TEXT, `erscheinungBegruendung` TEXT, `erscheinungHerkunft` TEXT, " +
            "`lebensraum` TEXT, `lebensraumBegruendung` TEXT, `lebensraumHerkunft` TEXT, " +
            "`verhalten` TEXT, `verhaltenBegruendung` TEXT, `verhaltenHerkunft` TEXT, " +
            "`groesseMinM` REAL, `groesseMaxM` REAL, `groesseBegruendung` TEXT, `groesseHerkunft` TEXT, " +
            "`gewichtMinKg` REAL, `gewichtMaxKg` REAL, `gewichtBegruendung` TEXT, `gewichtHerkunft` TEXT, " +
            "`pulsMinBpm` INTEGER, `pulsMaxBpm` INTEGER, `pulsBegruendung` TEXT, `pulsHerkunft` TEXT, " +
            "`blutdruckSystolisch` INTEGER, `blutdruckDiastolisch` INTEGER, `blutdruckHinweis` TEXT, " +
            "`blutdruckBegruendung` TEXT, `blutdruckHerkunft` TEXT, " +
            "`allergien` TEXT, `allergienBegruendung` TEXT, `allergienHerkunft` TEXT, " +
            "`verfassungStufe` TEXT, `verfassungText` TEXT, `verfassungBegruendung` TEXT, `verfassungHerkunft` TEXT, " +
            "`iqMin` INTEGER, `iqMax` INTEGER, `iqBegruendung` TEXT, `iqHerkunft` TEXT, " +
            "`eqMin` INTEGER, `eqMax` INTEGER, `eqBegruendung` TEXT, `eqHerkunft` TEXT, " +
            "`wahnhaft` INTEGER, `wahnhaftBegruendung` TEXT, `wahnhaftHerkunft` TEXT, PRIMARY KEY(`id`))"
    const val CREATE_INDEX =
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_creature_profiles_creatureId` ON `creature_profiles` (`creatureId`)"
}

/**
 * Akten im FTS5-Index [SearchIndex]: je aktiver Akte eine Zeile vom Typ
 * [TYPE] mit `ref_id` = Wesen-ID. Durchsucht werden Erscheinung, Lebensraum,
 * Verhalten, Verfassungstext und Allergie-Auslöser/-Reaktionen (Spalte `body`).
 * Die Suche ordnet Treffer dieses Typs dem Wesen zu ([SearchIndex.TYPE_CREATURE]).
 */
internal object ProfileSearch {
    const val TYPE = "creature_profile"

    private fun body(row: String) =
        "coalesce($row.erscheinung, '') || ' ' || coalesce($row.lebensraum, '') || ' ' || " +
            "coalesce($row.verhalten, '') || ' ' || coalesce($row.verfassungText, '') || ' ' || " +
            "coalesce((SELECT group_concat(coalesce(json_extract(a.value, '$.ausloeser'), '') || ' ' || " +
            "coalesce(json_extract(a.value, '$.reaktion'), ''), ' ') " +
            "FROM json_each(coalesce($row.allergien, '[]')) a), '')"

    private fun insert(row: String) =
        "INSERT INTO ${SearchIndex.TABLE}(type, ref_id, title, subtitle, body) " +
            "SELECT '$TYPE', $row.creatureId, '', '', ${body(row)} WHERE $row.deletedAt IS NULL;"

    private fun delete(row: String) = "DELETE FROM ${SearchIndex.TABLE} WHERE type = '$TYPE' AND ref_id = $row.creatureId;"

    val CREATE: List<String> =
        listOf(
            "CREATE TRIGGER IF NOT EXISTS search_creature_profiles_ai AFTER INSERT ON creature_profiles BEGIN " +
                insert("NEW") + " END",
            "CREATE TRIGGER IF NOT EXISTS search_creature_profiles_au AFTER UPDATE ON creature_profiles BEGIN " +
                delete("OLD") + " " + insert("NEW") + " END",
            "CREATE TRIGGER IF NOT EXISTS search_creature_profiles_ad AFTER DELETE ON creature_profiles BEGIN " +
                delete("OLD") + " END",
        )

    val REBUILD: List<String> =
        listOf(
            "DELETE FROM ${SearchIndex.TABLE} WHERE type = '$TYPE'",
            "INSERT INTO ${SearchIndex.TABLE}(type, ref_id, title, subtitle, body) " +
                "SELECT '$TYPE', p.creatureId, '', '', ${body("p")} FROM creature_profiles p WHERE p.deletedAt IS NULL",
        )

    fun create(connection: SQLiteConnection) = CREATE.forEach { connection.execSQL(it) }

    fun rebuild(connection: SQLiteConnection) = REBUILD.forEach { connection.execSQL(it) }
}
