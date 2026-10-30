package de.ruff.dimension.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Lokale Datenbank der App, künftig Single Source of Truth.
 *
 * Version 1. Es gibt bewusst keinen destruktiven Migrations-Fallback: jede
 * Schemaänderung braucht eine geprüfte Migration, damit spätere Nutzerdaten
 * (Favoriten, Notizen) nie verloren gehen. Die geplante FTS5-Volltextsuche
 * kommt als eigene Migration (Version 2), die die virtuelle Tabelle per
 * `execSQL` anlegt und aus `creatures`, `rule_pages` und `rule_blocks` füllt.
 */
@Database(
    entities = [
        CreatureEntity::class,
        RulePageEntity::class,
        RuleBlockEntity::class,
        SeedMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
internal abstract class RuffDatabase : RoomDatabase() {
    abstract fun creatureDao(): CreatureDao

    abstract fun ruleDao(): RuleDao

    abstract fun seedMetaDao(): SeedMetaDao

    companion object {
        const val NAME = "ruff.db"

        fun create(context: Context): RuffDatabase =
            Room
                .databaseBuilder(context.applicationContext, RuffDatabase::class.java, NAME)
                .build()
    }
}
