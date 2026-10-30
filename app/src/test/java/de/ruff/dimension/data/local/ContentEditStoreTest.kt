package de.ruff.dimension.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.ruff.dimension.data.local.RuffDatabase.Companion.configure
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentEditStoreTest {
    private lateinit var database: RuffDatabase
    private lateinit var store: RoomContentEditStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, RuffDatabase::class.java)
                .configure()
                .allowMainThreadQueries()
                .build()
        store = RoomContentEditStore(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertReadAndDeleteLocalEdit() =
        runBlocking {
            val original = ContentEdit("creature", "creature-1", """{"name":"First"}""")
            store.upsert(original)
            store.upsert(original.copy(editedJson = """{"name":"Second"}"""))

            assertEquals(listOf(original.copy(editedJson = """{"name":"Second"}""")), store.getAll())

            store.delete("creature", "creature-1")

            assertEquals(emptyList<ContentEdit>(), store.getAll())
        }
}
