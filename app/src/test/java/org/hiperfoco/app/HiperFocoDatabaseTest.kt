package org.hiperfoco.app

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HiperFocoDatabaseTest {
    @Test
    fun persistsChangesAndDeletesOnlyTopicsOfTheRemovedUniverse() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val filename = "hiperfoco-test-${UUID.randomUUID()}.db"
        try {
            val (universeId, otherId) = withDatabase(context, filename) { db ->
                val dao = db.universeDao()
                val examples = dao.observeUniverses().first()
                assertEquals(
                    listOf("Astronomia", "Programação", "História", "Linux", "Dinossauros"),
                    examples.map { it.universe.name },
                )
                val linux = examples.single { it.universe.name == "Linux" }
                assertEquals(
                    setOf(
                        "Distribuições: Fedora, Ubuntu e Debian",
                        "Terminal e comandos básicos",
                        "Arquivos e permissões",
                        "Pacotes e atualizações",
                        "Serviços e logs",
                        "Scripts em Bash",
                    ),
                    linux.topics.map { it.name }.toSet(),
                )
                val dinosaurs = examples.single { it.universe.name == "Dinossauros" }
                assertEquals(
                    setOf(
                        "Períodos: Triássico, Jurássico e Cretáceo",
                        "Dinossauros famosos: T. rex, Triceratops e Stegosaurus",
                        "Alimentação e habitats",
                        "Fósseis e escavações",
                        "Extinção no fim do Cretáceo",
                        "Evolução e parentesco com as aves",
                    ),
                    dinosaurs.topics.map { it.name }.toSet(),
                )
                examples.forEach { dao.deleteUniverse(it.universe.id) }

                val id = dao.insertUniverse(Universe("Órbita"))
                val second = dao.insertUniverse(Universe("Física"))
                assertTrue(id > 0 && second > 0)
                assertEquals(-1L, dao.insertUniverse(Universe("ÓRBITA")))
                assertEquals(0, dao.updateUniverse(Universe("FÍSICA", id = id)))
                assertEquals(1, dao.updateUniverse(Universe("Astronáutica", id = id)))

                val topicId = dao.insertTopic(Topic(id, "Lua"))
                val toDelete = dao.insertTopic(Topic(id, "Marte"))
                assertTrue(topicId > 0 && toDelete > 0)
                assertEquals(-1L, dao.insertTopic(Topic(id, "LUA")))
                assertTrue(dao.insertTopic(Topic(second, "Lua")) > 0)
                assertEquals(0, dao.updateTopic(Topic(id, "MARTE", id = topicId)))
                assertEquals(1, dao.updateTopic(Topic(id, "Satélites", id = topicId)))
                dao.deleteTopic(toDelete)
                id to second
            }

            withDatabase(context, filename) { db ->
                val dao = db.universeDao()
                val persisted = dao.observeUniverses().first()
                assertEquals(listOf("Astronáutica", "Física"), persisted.map { it.universe.name })
                assertEquals(listOf("Satélites"), persisted.first().topics.map { it.name })

                dao.deleteUniverse(universeId)
                val remaining = dao.observeUniverses().first().single()
                assertEquals(otherId, remaining.universe.id)
                assertEquals("Lua", remaining.topics.single().name)
                db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM topics").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(1, cursor.getInt(0))
                }

                val invalidInsert = runCatching { dao.insertTopic(Topic(universeId, "Órfão")) }
                assertTrue(invalidInsert.exceptionOrNull() is SQLiteConstraintException)
            }
        } finally {
            context.deleteDatabase(filename)
        }
    }

    private suspend fun <T> withDatabase(
        context: Context,
        filename: String,
        test: suspend (HiperFocoDatabase) -> T,
    ): T {
        val database = HiperFocoDatabase.build(context, filename)
        try {
            return test(database)
        } finally {
            database.close()
        }
    }
}
