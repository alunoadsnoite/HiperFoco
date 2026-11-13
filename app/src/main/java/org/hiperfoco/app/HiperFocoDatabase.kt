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
    val icon: String? = null,
    val color: Long = 0xFF6554A4,
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
    val note: String? = null,
    val done: Boolean = false,
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

    @Query("UPDATE topics SET done = :done WHERE id = :id")
    suspend fun toggleTopicDone(id: Long, done: Boolean)
}

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE universes ADD COLUMN icon TEXT")
        db.execSQL("ALTER TABLE universes ADD COLUMN color INTEGER NOT NULL DEFAULT 0xFF6554A4")
        db.execSQL("ALTER TABLE topics ADD COLUMN note TEXT")
        db.execSQL("ALTER TABLE topics ADD COLUMN done INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [Universe::class, Topic::class], version = 2)
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
                        val resources = AppLanguage.context(context).resources
                        val examples = mapOf(
                            resources.getString(R.string.seed_astronomy) to emptyList(),
                            resources.getString(R.string.seed_programming) to emptyList(),
                            resources.getString(R.string.seed_history) to emptyList(),
                            resources.getString(R.string.seed_linux) to listOf(
                                resources.getString(R.string.seed_distros),
                                resources.getString(R.string.seed_terminal),
                                resources.getString(R.string.seed_files),
                                resources.getString(R.string.seed_packages),
                                resources.getString(R.string.seed_services),
                                resources.getString(R.string.seed_bash),
                            ),
                            resources.getString(R.string.seed_dinosaurs) to listOf(
                                resources.getString(R.string.seed_periods),
                                resources.getString(R.string.seed_famous),
                                resources.getString(R.string.seed_habitats),
                                resources.getString(R.string.seed_fossils),
                                resources.getString(R.string.seed_extinction),
                                resources.getString(R.string.seed_evolution),
                            ),
                        )
                        for ((universe, topics) in examples) {
                            val universeKey = nameKey(universe)
                            db.execSQL(
                                "INSERT INTO universes (name, nameKey, icon, color) VALUES (?, ?, ?, ?)",
                                arrayOf<Any?>(universe, universeKey, null, 0xFF6554A4L),
                            )
                            for (topic in topics) {
                                db.execSQL(
                                    """
                                    INSERT INTO topics (universeId, name, nameKey, note, done)
                                    SELECT id, ?, ?, NULL, 0 FROM universes WHERE nameKey = ?
                                    """.trimIndent(),
                                    arrayOf(topic, nameKey(topic), universeKey),
                                )
                            }
                        }
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
