package de.ruff.dimension.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

/**
 * Lokale Datenbank der App, Single Source of Truth.
 *
 * Es gibt bewusst keinen destruktiven Migrations-Fallback: jede
 * Schemaänderung braucht eine geprüfte Migration, damit spätere Nutzerdaten
 * (Favoriten, Notizen) nie verloren gehen.
 *
 * - Version 1: Inhaltstabellen und Seed-Metadaten.
 * - Version 2: FTS5-Volltextindex [SearchIndex] per [MIGRATION_1_2]. Die
 *   Entities sind unverändert; die virtuelle Tabelle steht daher nicht im
 *   exportierten Room-Schema, sondern in [SearchIndex].
 *
 * Als SQLite wird das mit der App gebündelte SQLite (`sqlite-bundled`)
 * verwendet, weil das Android-System-SQLite FTS5 nicht garantiert.
 */
@Database(
    entities = [
        CreatureEntity::class,
        RulePageEntity::class,
        RuleBlockEntity::class,
        SeedMetaEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
internal abstract class RuffDatabase : RoomDatabase() {
    abstract fun creatureDao(): CreatureDao

    abstract fun ruleDao(): RuleDao

    abstract fun seedMetaDao(): SeedMetaDao

    companion object {
        const val NAME = "ruff.db"

        /** Legt bei Neuinstallation den Suchindex an (bestehende Installationen: [MIGRATION_1_2]). */
        val CALLBACK =
            object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    SearchIndex.create(connection)
                    connection.execSQL(RoomContentEditStore.CREATE_TABLE)
                }

                override fun onOpen(connection: SQLiteConnection) {
                    connection.execSQL(RoomContentEditStore.CREATE_TABLE)
                }
            }

        fun create(context: Context): RuffDatabase =
            Room
                .databaseBuilder(context.applicationContext, RuffDatabase::class.java, NAME)
                .configure()
                .build()

        /** Gemeinsame Konfiguration für App und Tests: gebündeltes SQLite, Migrationen, Callback. */
        fun RoomDatabase.Builder<RuffDatabase>.configure(): RoomDatabase.Builder<RuffDatabase> =
            setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_1_2)
                .addCallback(CALLBACK)
    }
}
