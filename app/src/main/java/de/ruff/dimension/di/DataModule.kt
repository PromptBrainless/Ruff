package de.ruff.dimension.di

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.ruff.dimension.data.local.AssetProfileSeedSource
import de.ruff.dimension.data.local.AssetSeedSource
import de.ruff.dimension.data.local.ContentEditStore
import de.ruff.dimension.data.local.CreatureDao
import de.ruff.dimension.data.local.CreatureProfileSeeder
import de.ruff.dimension.data.local.RoomContentEditStore
import de.ruff.dimension.data.local.RuffDatabase
import de.ruff.dimension.data.local.RuleDao
import de.ruff.dimension.data.local.SeedImporter
import de.ruff.dimension.data.local.SeedSource
import de.ruff.dimension.data.repository.AdminAccessRepository
import de.ruff.dimension.data.repository.ContentEditorRepository
import de.ruff.dimension.data.repository.CreatureProfileRepository
import de.ruff.dimension.data.repository.CreatureRepository
import de.ruff.dimension.data.repository.FavoritesRepository
import de.ruff.dimension.data.repository.MediaImporter
import de.ruff.dimension.data.repository.PrivateMediaImporter
import de.ruff.dimension.data.repository.RoomContentEditorRepository
import de.ruff.dimension.data.repository.RoomCreatureProfileRepository
import de.ruff.dimension.data.repository.RoomCreatureRepository
import de.ruff.dimension.data.repository.RoomFavoritesRepository
import de.ruff.dimension.data.repository.RoomRuleRepository
import de.ruff.dimension.data.repository.RoomSearchRepository
import de.ruff.dimension.data.repository.RuleRepository
import de.ruff.dimension.data.repository.SearchRepository
import de.ruff.dimension.data.repository.SharedPreferencesAdminAccess
import de.ruff.dimension.domain.LibrarySource
import de.ruff.dimension.domain.ObserveFavorites
import de.ruff.dimension.domain.SearchLibrary
import de.ruff.dimension.domain.SeededLibrarySource
import de.ruff.dimension.domain.ToggleFavorite
import kotlinx.coroutines.CancellationException
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
    fun provideContentEditStore(database: RuffDatabase): ContentEditStore = RoomContentEditStore(database)

    @Provides
    @Singleton
    fun provideAdminAccessRepository(
        @ApplicationContext context: Context,
    ): AdminAccessRepository = SharedPreferencesAdminAccess(context)

    @Provides
    @Singleton
    fun provideMediaImporter(
        @ApplicationContext context: Context,
    ): MediaImporter = PrivateMediaImporter(context)

    @Provides
    @Singleton
    fun provideContentEditorRepository(
        store: ContentEditStore,
        mediaImporter: MediaImporter,
    ): ContentEditorRepository = RoomContentEditorRepository(store, mediaImporter)

    @Provides
    @Singleton
    fun provideCreatureProfileSeeder(
        @ApplicationContext context: Context,
        database: RuffDatabase,
    ): CreatureProfileSeeder = CreatureProfileSeeder(database, AssetProfileSeedSource(context.assets))

    @Provides
    @Singleton
    fun provideCreatureProfileRepository(database: RuffDatabase): CreatureProfileRepository = RoomCreatureProfileRepository(database)

    /**
     * Nach dem Inhalts-Seed laufen die Muster-Akten. Ein Fehler dort darf die
     * Bibliothek nicht in den Asset-Notbetrieb zwingen: Er wird nur protokolliert,
     * und der nächste Start versucht es erneut (Seed-Version bleibt unverändert).
     */
    @Provides
    @Singleton
    fun provideLibrarySource(
        seedImporter: SeedImporter,
        profileSeeder: CreatureProfileSeeder,
        creatures: CreatureRepository,
        rules: RuleRepository,
        fallback: SeedSource,
    ): LibrarySource =
        SeededLibrarySource(
            seed = {
                seedImporter.seedIfNeeded()
                try {
                    profileSeeder.seedIfNeeded()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("RuffAkte", "Muster-Akten konnten nicht eingespielt werden", e)
                }
            },
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
    @Singleton
    fun provideFavoritesRepository(database: RuffDatabase): FavoritesRepository = RoomFavoritesRepository(database)

    @Provides
    fun provideObserveFavorites(repository: FavoritesRepository): ObserveFavorites = ObserveFavorites(repository)

    @Provides
    fun provideToggleFavorite(repository: FavoritesRepository): ToggleFavorite = ToggleFavorite(repository)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
