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
 * Favorit eines Wesens – Nutzerdaten, strikt getrennt von den Seed-Tabellen.
 *
 * - [id] ist eine UUID, damit Einträge später geräteübergreifend abgeglichen
 *   werden können, ohne mit lokalen Zählern zu kollidieren.
 * - [creatureId] verweist auf `creatures.id`, bewusst **ohne** Fremdschlüssel:
 *   Der Seed darf Nutzerdaten nie über eine Kaskade löschen oder blockieren.
 *   Verwaiste Einträge (Wesen fehlt in neuen Seed-Daten) bleiben erhalten und
 *   werden in der Oberfläche einfach nicht angezeigt.
 * - Pro Wesen gibt es genau eine Zeile (eindeutiger Index). Entfernen ist ein
 *   Soft-Delete über [deletedAt]; erneutes Favorisieren belebt dieselbe Zeile.
 * - Jede Änderung setzt [updatedAt] und erhöht [revision] (für späteren Sync).
 */
@Entity(
    tableName = "favorites",
    indices = [Index(value = ["creatureId"], unique = true)],
)
internal data class FavoriteEntity(
    @PrimaryKey val id: String,
    val creatureId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
    /** `null` = aktiv; sonst Zeitpunkt des (weichen) Entfernens. */
    val deletedAt: Long? = null,
) {
    val active: Boolean get() = deletedAt == null
}

@Dao
internal interface FavoriteDao {
    /** Aktive Favoriten, ältester zuerst. */
    @Query("SELECT creatureId FROM favorites WHERE deletedAt IS NULL ORDER BY createdAt, id")
    fun observeActiveCreatureIds(): Flow<List<String>>

    @Query("SELECT * FROM favorites WHERE creatureId = :creatureId")
    suspend fun getByCreatureId(creatureId: String): FavoriteEntity?

    @Query("SELECT * FROM favorites ORDER BY createdAt, id")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT COUNT(*) FROM favorites WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(favorite: FavoriteEntity)

    @Update
    suspend fun update(favorite: FavoriteEntity)
}

/**
 * v2 → v3: legt nur die Favoriten-Tabelle samt eindeutigem Index an (additiv).
 * Inhalte, Suchindex und Bearbeitungen bleiben unberührt. Das SQL entspricht
 * exakt `schemas/…/3.json`; Room prüft das Ergebnis beim Öffnen.
 */
internal val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(FavoritesSchema.CREATE_TABLE)
            connection.execSQL(FavoritesSchema.CREATE_INDEX)
        }
    }

internal object FavoritesSchema {
    const val CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS `favorites` (`id` TEXT NOT NULL, `creatureId` TEXT NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `revision` INTEGER NOT NULL, " +
            "`deletedAt` INTEGER, PRIMARY KEY(`id`))"
    const val CREATE_INDEX =
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_creatureId` ON `favorites` (`creatureId`)"
}
