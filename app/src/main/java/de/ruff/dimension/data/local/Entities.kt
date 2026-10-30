package de.ruff.dimension.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// Inhaltstabellen, die ausschließlich aus den gebündelten Assets befüllt werden
// (siehe SeedImporter). Spätere Nutzertabellen wie Favoriten oder Notizen
// gehören in eigene Tabellen, die nur über CreatureEntity.id bzw.
// RulePageEntity.id verknüpft sind und vom Seed nie berührt werden.

/** Ein Wesen aus `assets/creatures.json`; [position] ist die Reihenfolge im JSON. */
@Entity(tableName = "creatures")
internal data class CreatureEntity(
    @PrimaryKey val id: String,
    val position: Int,
    val name: String,
    val source: String,
    val status: String,
    val powerLevel: Int,
    val powerLabel: String,
    val spielbar: Boolean?,
    val description: String,
    val archetype: String,
    val portrait: String,
    val icon: String,
    val cover: String,
    /** Freie Felder des Wesens als JSON, Schlüsselreihenfolge wie in der Quelle. */
    val fields: Map<String, String>,
)

/** Eine Regelseite aus `assets/rules.json`; [id] ist der stabile Quellpfad (`source`). */
@Entity(tableName = "rule_pages")
internal data class RulePageEntity(
    @PrimaryKey val id: String,
    val position: Int,
    val title: String,
    val source: String,
    val fields: Map<String, String>,
)

/** Ein Inhaltsblock einer Regelseite, sortiert über [position]. */
@Entity(
    tableName = "rule_blocks",
    primaryKeys = ["pageId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = RulePageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class RuleBlockEntity(
    val pageId: String,
    val position: Int,
    val type: String,
    val text: String,
)

/** Stand des Asset-Seeds: welche Seed-Version wann eingespielt wurde. */
@Entity(tableName = "seed_meta")
internal data class SeedMetaEntity(
    @PrimaryKey val key: String,
    val version: Int,
    val seededAtMillis: Long,
)
