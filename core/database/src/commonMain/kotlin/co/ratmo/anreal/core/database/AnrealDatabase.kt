package co.ratmo.anreal.core.database

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [SessionEntity::class, MessageEntity::class, QueuedItemEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
@ConstructedBy(AnrealDatabaseConstructor::class)
abstract class AnrealDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun queuedItemDao(): QueuedItemDao
}

@Suppress("KotlinNoActualForExpect")
expect object AnrealDatabaseConstructor : RoomDatabaseConstructor<AnrealDatabase> {
    override fun initialize(): AnrealDatabase
}

fun getRoomDatabase(builder: RoomDatabase.Builder<AnrealDatabase>): AnrealDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
