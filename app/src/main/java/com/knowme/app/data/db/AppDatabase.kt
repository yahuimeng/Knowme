package com.knowme.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NotificationEntity::class, TodoEntity::class, DigestEntity::class,
        AskMessageEntity::class, TokenUsageEntity::class,
        ConversationEntity::class, ChatMessageEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun todoDao(): TodoDao
    abstract fun digestDao(): DigestDao
    abstract fun askDao(): AskDao
    abstract fun tokenUsageDao(): TokenUsageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

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

        /** v2→v3：新增 token_usage 表。 */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `token_usage` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`kind` TEXT NOT NULL, " +
                        "`model` TEXT NOT NULL, " +
                        "`inputTokens` INTEGER NOT NULL, " +
                        "`outputTokens` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_token_usage_createdAt` ON `token_usage` (`createdAt`)")
            }
        }

        /** v3→v4：新增 conversations / messages 表（多对话+多轮聊天）。 */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `conversations` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`mode` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_updatedAt` ON `conversations` (`updatedAt`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `messages` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`conversationId` INTEGER NOT NULL, " +
                        "`role` TEXT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`isError` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversationId` ON `messages` (`conversationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_createdAt` ON `messages` (`createdAt`)")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "knowme.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
