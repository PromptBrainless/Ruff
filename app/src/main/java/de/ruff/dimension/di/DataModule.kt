package de.ruff.dimension.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.ruff.dimension.data.local.AssetSeedSource
import de.ruff.dimension.data.local.CreatureDao
import de.ruff.dimension.data.local.RuffDatabase
import de.ruff.dimension.data.local.RuleDao
import de.ruff.dimension.data.local.SeedImporter
import de.ruff.dimension.data.local.SeedSource
import de.ruff.dimension.data.repository.CreatureRepository
import de.ruff.dimension.data.repository.RoomCreatureRepository
import de.ruff.dimension.data.repository.RoomRuleRepository
import de.ruff.dimension.data.repository.RoomSearchRepository
import de.ruff.dimension.data.repository.RuleRepository
import de.ruff.dimension.data.repository.SearchRepository
import de.ruff.dimension.domain.LibrarySource
import de.ruff.dimension.domain.SearchLibrary
import de.ruff.dimension.domain.SeededLibrarySource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/** Dispatcher für Datenbank- und Dateizugriffe. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class IoDispatcher

/** Stellt Datenbank, DAOs, Seed-Quelle und Repositories bereit. */
@Module
@InstallIn(SingletonComponent::class)
internal object DataModule {
    /** Ohne destruktiven Migrations-Fallback, siehe [RuffDatabase.create]. */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): RuffDatabase = RuffDatabase.create(context)

    @Provides
    fun provideCreatureDao(database: RuffDatabase): CreatureDao = database.creatureDao()

    @Provides
    fun provideRuleDao(database: RuffDatabase): RuleDao = database.ruleDao()

    @Provides
    @Singleton
    fun provideSeedSource(
        @ApplicationContext context: Context,
    ): SeedSource = AssetSeedSource(context.assets)

    @Provides
    @Singleton
    fun provideSeedImporter(
        database: RuffDatabase,
        source: SeedSource,
    ): SeedImporter = SeedImporter(database, source)

    @Provides
    @Singleton
    fun provideCreatureRepository(dao: CreatureDao): CreatureRepository = RoomCreatureRepository(dao)

    @Provides
    @Singleton
    fun provideRuleRepository(dao: RuleDao): RuleRepository = RoomRuleRepository(dao)

    @Provides
    @Singleton
    fun provideLibrarySource(
        seedImporter: SeedImporter,
        creatures: CreatureRepository,
        rules: RuleRepository,
        fallback: SeedSource,
    ): LibrarySource =
        SeededLibrarySource(
            seed = { seedImporter.seedIfNeeded() },
            creatures = creatures,
            rules = rules,
            fallback = fallback,
        )

    @Provides
    @Singleton
    fun provideSearchRepository(database: RuffDatabase): SearchRepository = RoomSearchRepository(database)

    @Provides
    fun provideSearchLibrary(repository: SearchRepository): SearchLibrary = SearchLibrary(repository)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
