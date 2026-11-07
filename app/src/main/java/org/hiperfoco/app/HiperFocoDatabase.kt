package org.hiperfoco.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.Locale

internal fun nameKey(name: String): String = name.lowercase(Locale.ROOT)

@Entity(tableName = "universes", indices = [Index(value = ["nameKey"], unique = true)])
internal data class Universe(
    val name: String,
    val nameKey: String = nameKey(name),
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = Universe::class,
            parentColumns = ["id"],
            childColumns = ["universeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["universeId", "nameKey"], unique = true)],
)
internal data class Topic(
    val universeId: Long,
    val name: String,
    val nameKey: String = nameKey(name),
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

internal data class UniverseWithTopics(
    @Embedded val universe: Universe,
    @Relation(parentColumn = "id", entityColumn = "universeId") val topics: List<Topic>,
)

@Dao
internal interface UniverseDao {
    @Transaction
    @Query("SELECT * FROM universes ORDER BY id")
    fun observeUniverses(): Flow<List<UniverseWithTopics>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUniverse(universe: Universe): Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    suspend fun updateUniverse(universe: Universe): Int

    @Query("DELETE FROM universes WHERE id = :id")
    suspend fun deleteUniverse(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTopic(topic: Topic): Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    suspend fun updateTopic(topic: Topic): Int

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopic(id: Long)
}

@Database(entities = [Universe::class, Topic::class], version = 1)
internal abstract class HiperFocoDatabase : RoomDatabase() {
    abstract fun universeDao(): UniverseDao

    companion object {
        private var instance: HiperFocoDatabase? = null

        @Synchronized
        fun get(context: Context): HiperFocoDatabase {
            return instance ?: build(context.applicationContext, "hiperfoco.db").also {
                instance = it
            }
        }

        fun build(context: Context, name: String): HiperFocoDatabase {
            return Room.databaseBuilder(context, HiperFocoDatabase::class.java, name)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val examples = mapOf(
                            "Astronomia" to emptyList(),
                            "Programação" to emptyList(),
                            "História" to emptyList(),
                            "Linux" to listOf(
                                "Distribuições: Fedora, Ubuntu e Debian",
                                "Terminal e comandos básicos",
                                "Arquivos e permissões",
                                "Pacotes e atualizações",
                                "Serviços e logs",
                                "Scripts em Bash",
                            ),
                            "Dinossauros" to listOf(
                                "Períodos: Triássico, Jurássico e Cretáceo",
                                "Dinossauros famosos: T. rex, Triceratops e Stegosaurus",
                                "Alimentação e habitats",
                                "Fósseis e escavações",
                                "Extinção no fim do Cretáceo",
                                "Evolução e parentesco com as aves",
                            ),
                        )
                        for ((universe, topics) in examples) {
                            val universeKey = nameKey(universe)
                            db.execSQL(
                                "INSERT INTO universes (name, nameKey) VALUES (?, ?)",
                                arrayOf(universe, universeKey),
                            )
                            for (topic in topics) {
                                db.execSQL(
                                    """
                                    INSERT INTO topics (universeId, name, nameKey)
                                    SELECT id, ?, ? FROM universes WHERE nameKey = ?
                                    """.trimIndent(),
                                    arrayOf(topic, nameKey(topic), universeKey),
                                )
                            }
                        }
                    }
                })
                .build()
        }
    }
}
