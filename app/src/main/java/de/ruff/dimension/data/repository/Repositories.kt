package de.ruff.dimension.data.repository

import de.ruff.dimension.data.Creature
import de.ruff.dimension.data.RuleBlock
import de.ruff.dimension.data.RulePage
import de.ruff.dimension.data.local.CreatureDao
import de.ruff.dimension.data.local.CreatureEntity
import de.ruff.dimension.data.local.RuleBlockEntity
import de.ruff.dimension.data.local.RuleDao
import de.ruff.dimension.data.local.RulePageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Zugriff auf Wesen; liefert die bestehenden App-Modelle. Noch nicht an die UI angebunden. */
internal interface CreatureRepository {
    fun observeCreatures(): Flow<List<Creature>>

    suspend fun getCreatures(): List<Creature>

    suspend fun getCreature(id: String): Creature?
}

/** Zugriff auf Regelseiten mit ihren Blöcken. Noch nicht an die UI angebunden. */
internal interface RuleRepository {
    suspend fun getRulePages(): List<RulePage>

    suspend fun getRulePage(id: String): RulePage?
}

internal class RoomCreatureRepository(
    private val dao: CreatureDao,
) : CreatureRepository {
    override fun observeCreatures(): Flow<List<Creature>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun getCreatures(): List<Creature> = dao.getAll().map { it.toModel() }

    override suspend fun getCreature(id: String): Creature? = dao.getById(id)?.toModel()
}

internal class RoomRuleRepository(
    private val dao: RuleDao,
) : RuleRepository {
    override suspend fun getRulePages(): List<RulePage> {
        val blocksByPage = dao.getAllBlocks().groupBy { it.pageId }
        return dao.getPages().map { page -> page.toModel(blocksByPage[page.id].orEmpty()) }
    }

    override suspend fun getRulePage(id: String): RulePage? = dao.getPageById(id)?.toModel(dao.getBlocks(id))
}

internal fun CreatureEntity.toModel() =
    Creature(
        name = name,
        powerLevel = powerLevel,
        powerLabel = powerLabel,
        description = description,
        archetype = archetype,
        fields = fields,
        portrait = portrait,
        icon = icon,
        cover = cover,
        id = id,
        source = source,
        status = status,
        spielbar = spielbar,
    )

internal fun RulePageEntity.toModel(blocks: List<RuleBlockEntity>) =
    RulePage(
        title = title,
        source = source,
        fields = fields,
        blocks = blocks.sortedBy { it.position }.map { RuleBlock(type = it.type, text = it.text) },
    )
