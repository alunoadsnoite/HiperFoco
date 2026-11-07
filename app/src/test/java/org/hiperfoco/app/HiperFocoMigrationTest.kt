package org.hiperfoco.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HiperFocoMigrationTest {
    @Test
    fun upgradesFromVersion1KeepingExistingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val filename = "hiperfoco-migration-${UUID.randomUUID()}.db"
        try {
            createVersion1Database(context, filename)

            val database = HiperFocoDatabase.build(context, filename)
            try {
                val universes = database.universeDao().observeUniverses().first()
                assertEquals(listOf("Antigo"), universes.map { it.universe.name })

                val entry = universes.single()
                assertEquals("Topico", entry.topics.single().name)
                assertEquals(0xFF6554A4L, entry.universe.color)
                assertNull(entry.universe.icon)
                assertNull(entry.topics.single().note)
                assertFalse(entry.topics.single().done)
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(filename)
        }
    }

    private fun createVersion1Database(context: Context, filename: String) {
        val file = context.getDatabasePath(filename)
        file.parentFile?.mkdirs()
        val database = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            database.execSQL(
                "CREATE TABLE universes (" +
                    "name TEXT NOT NULL, nameKey TEXT NOT NULL, " +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)",
            )
            database.execSQL(
                "CREATE UNIQUE INDEX index_universes_nameKey ON universes (nameKey)",
            )
            database.execSQL(
                "CREATE TABLE topics (" +
                    "universeId INTEGER NOT NULL, name TEXT NOT NULL, nameKey TEXT NOT NULL, " +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "FOREIGN KEY(universeId) REFERENCES universes(id) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE)",
            )
            database.execSQL(
                "CREATE UNIQUE INDEX index_topics_universeId_nameKey ON topics (universeId, nameKey)",
            )
            database.execSQL("INSERT INTO universes (name, nameKey) VALUES ('Antigo', 'antigo')")
            database.execSQL(
                "INSERT INTO topics (universeId, name, nameKey) VALUES (1, 'Topico', 'topico')",
            )
            database.version = 1
        } finally {
            database.close()
        }
    }
}
