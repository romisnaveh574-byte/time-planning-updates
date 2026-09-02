package com.example.birthdaycountdown.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase

class RecordTypeConverters {
    @TypeConverter fun fromType(value: RecordType): String = value.name
    @TypeConverter fun toType(value: String): RecordType = RecordType.valueOf(value)
    @TypeConverter fun fromCalendar(value: CalendarType): String = value.name
    @TypeConverter fun toCalendar(value: String): CalendarType = CalendarType.valueOf(value)
    @TypeConverter fun fromWatchStatus(value: WatchStatus): String = value.name
    @TypeConverter fun toWatchStatus(value: String): WatchStatus = WatchStatus.valueOf(value)
}

@Database(entities = [CountdownEntity::class, WatchCategoryEntity::class, WatchRecordEntity::class, WatchStatusEntity::class, AiConversationEntity::class, AiMessageEntity::class], version = 17, exportSchema = false)
@TypeConverters(RecordTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun countdownDao(): CountdownDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun aiHistoryDao(): AiHistoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun create(context: android.content.Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "countdown.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
                .build()
                .also { instance = it }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN calendarType TEXT NOT NULL DEFAULT 'SOLAR'")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN lunarYear INTEGER")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN lunarMonth INTEGER")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN lunarDay INTEGER")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN lunarLeapMonth INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN showYears INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN showMonths INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN showDays INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN showHours INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN showMinutes INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN showSeconds INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN showSolarDate INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN showLunarDate INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN cardBackgroundColor INTEGER NOT NULL DEFAULT -1448980")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN cardTextColor INTEGER NOT NULL DEFAULT -14081235")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN solarDisplayMask INTEGER NOT NULL DEFAULT 63")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN lunarDisplayMask INTEGER NOT NULL DEFAULT 63")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN countdownDisplayMask INTEGER NOT NULL DEFAULT 63")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN cardGradientId TEXT NOT NULL DEFAULT 'solid'")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN titleGradientId TEXT NOT NULL DEFAULT 'solid'")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN solarGradientId TEXT NOT NULL DEFAULT 'solid'")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN lunarGradientId TEXT NOT NULL DEFAULT 'solid'")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN countdownGradientId TEXT NOT NULL DEFAULT 'solid'")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN titleTextColor INTEGER NOT NULL DEFAULT -14081235")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN solarTextColor INTEGER NOT NULL DEFAULT -14081235")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN lunarTextColor INTEGER NOT NULL DEFAULT -14081235")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN countdownTextColor INTEGER NOT NULL DEFAULT -14081235")
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 2147483647")
                db.execSQL("UPDATE countdown_records SET titleTextColor = cardTextColor, solarTextColor = cardTextColor, lunarTextColor = cardTextColor, countdownTextColor = cardTextColor")
                db.execSQL("UPDATE countdown_records SET sortOrder = id")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE countdown_records ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `watch_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `watch_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, `currentEpisode` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `watch_categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_records_categoryId` ON `watch_records` (`categoryId`)")
                db.execSQL("INSERT INTO `watch_categories` (`name`, `sortOrder`) VALUES ('电视剧', 0), ('电影', 1), ('动漫', 2), ('短剧', 3)")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS ai_conversations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mode TEXT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS ai_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, conversationId INTEGER NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, imagePath TEXT, size TEXT, quality TEXT, status TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(conversationId) REFERENCES ai_conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_messages_conversationId ON ai_messages (conversationId)")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ai_messages ADD COLUMN referenceImagePath TEXT")
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ai_messages ADD COLUMN actualSize TEXT")
                db.execSQL("ALTER TABLE ai_messages ADD COLUMN warning TEXT")
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ai_messages ADD COLUMN errorMessage TEXT")
            }
        }
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watch_records ADD COLUMN totalEpisodes INTEGER")
                db.execSQL("ALTER TABLE watch_records ADD COLUMN platform TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE watch_records ADD COLUMN status TEXT NOT NULL DEFAULT 'WATCHING'")
                db.execSQL("ALTER TABLE watch_records ADD COLUMN lastWatchedAt INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ai_messages ADD COLUMN resultViewed INTEGER NOT NULL DEFAULT 1")
            }
        }
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS watch_statuses (id TEXT NOT NULL, name TEXT NOT NULL, systemType TEXT, sortOrder INTEGER NOT NULL, PRIMARY KEY(id))")
                WatchStatusEntity.builtIns.forEach { status ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO watch_statuses (id, name, systemType, sortOrder) VALUES (?, ?, ?, ?)",
                        arrayOf(status.id, status.name, status.systemType, status.sortOrder)
                    )
                }
            }
        }
    }
}
