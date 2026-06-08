package com.knowme.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NotificationEntity::class, TodoEntity::class, DigestEntity::class, AskMessageEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun todoDao(): TodoDao
    abstract fun digestDao(): DigestDao
    abstract fun askDao(): AskDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v1→v2：新增 asks 表，保留已有通知/待办/早报。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `asks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`question` TEXT NOT NULL, " +
                        "`answer` TEXT NOT NULL, " +
                        "`isError` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_asks_createdAt` ON `asks` (`createdAt`)")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "knowme.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
