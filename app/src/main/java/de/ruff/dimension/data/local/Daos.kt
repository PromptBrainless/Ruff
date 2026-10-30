package de.ruff.dimension.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CreatureDao {
    @Query("SELECT * FROM creatures ORDER BY position")
    fun observeAll(): Flow<List<CreatureEntity>>

    @Query("SELECT * FROM creatures ORDER BY position")
    suspend fun getAll(): List<CreatureEntity>

    @Query("SELECT * FROM creatures WHERE id = :id")
    fun observeById(id: String): Flow<CreatureEntity?>

    @Query("SELECT * FROM creatures WHERE id = :id")
    suspend fun getById(id: String): CreatureEntity?

    @Query("SELECT COUNT(*) FROM creatures")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(creatures: List<CreatureEntity>)
}

@Dao
internal interface RuleDao {
    @Query("SELECT * FROM rule_pages ORDER BY position")
    fun observePages(): Flow<List<RulePageEntity>>

    @Query("SELECT * FROM rule_pages ORDER BY position")
    suspend fun getPages(): List<RulePageEntity>

    @Query("SELECT * FROM rule_pages WHERE id = :id")
    suspend fun getPageById(id: String): RulePageEntity?

    @Query("SELECT * FROM rule_blocks WHERE pageId = :pageId ORDER BY position")
    fun observeBlocks(pageId: String): Flow<List<RuleBlockEntity>>

    @Query("SELECT * FROM rule_blocks WHERE pageId = :pageId ORDER BY position")
    suspend fun getBlocks(pageId: String): List<RuleBlockEntity>

    @Query("SELECT * FROM rule_blocks ORDER BY pageId, position")
    suspend fun getAllBlocks(): List<RuleBlockEntity>

    @Query("SELECT COUNT(*) FROM rule_pages")
    suspend fun countPages(): Int

    @Query("SELECT COUNT(*) FROM rule_blocks")
    suspend fun countBlocks(): Int

    @Upsert
    suspend fun upsertPages(pages: List<RulePageEntity>)

    @Upsert
    suspend fun upsertBlocks(blocks: List<RuleBlockEntity>)

    /** Entfernt überzählige Blöcke, wenn eine Seite in neuen Seed-Daten kürzer wird. */
    @Query("DELETE FROM rule_blocks WHERE pageId = :pageId AND position >= :fromPosition")
    suspend fun deleteBlocksFrom(
        pageId: String,
        fromPosition: Int,
    )
}

@Dao
internal interface SeedMetaDao {
    @Query("SELECT * FROM seed_meta WHERE `key` = :key")
    suspend fun get(key: String): SeedMetaEntity?

    @Upsert
    suspend fun upsert(meta: SeedMetaEntity)
}
