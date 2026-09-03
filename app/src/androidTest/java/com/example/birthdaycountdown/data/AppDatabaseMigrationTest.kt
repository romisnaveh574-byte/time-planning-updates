package com.example.birthdaycountdown.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @Test
    fun roomUpgradeFrom16SeedsBuiltInsPreservesStatusesAndValidatesSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-16-17.db"
        context.deleteDatabase(databaseName)
        createVersion16Database(context, databaseName)

        val database = openMigratedDatabase(context, databaseName)

        try {
            val sqlite = database.openHelper.writableDatabase
            val migratedStatuses = sqlite.query(
                "SELECT id, name, systemType, sortOrder FROM watch_statuses ORDER BY sortOrder"
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            WatchStatusEntity(
                                id = cursor.getString(0),
                                name = cursor.getString(1),
                                systemType = cursor.getString(2),
                                sortOrder = cursor.getInt(3)
                            )
                        )
                    }
                }
            }
            val recordStatusIds = sqlite.query("SELECT status FROM watch_records ORDER BY id").use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            val statusColumns = sqlite.query("PRAGMA table_info(watch_statuses)").use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(1)) }
            }

            assertEquals(WatchStatusEntity.builtIns, migratedStatuses)
            assertEquals(WatchStatus.entries.map { it.name }, recordStatusIds)
            assertEquals(listOf("id", "name", "systemType", "sortOrder"), statusColumns)
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun roomUpgradeFrom9PreservesCountdownAndCreatesWatchAndAiSchemas() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-9-17.db"
        context.deleteDatabase(databaseName)
        createVersion9Database(context, databaseName)

        val database = openMigratedDatabase(context, databaseName)

        try {
            val sqlite = database.openHelper.writableDatabase
            val countdown = sqlite.query("SELECT name, isPinned FROM countdown_records WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                cursor.getString(0) to cursor.getInt(1)
            }
            val categories = sqlite.query("SELECT COUNT(*) FROM watch_categories").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            val aiMessageColumns = columnNames(sqlite, "ai_messages")

            assertEquals("旧生日", countdown.first)
            assertEquals(1, countdown.second)
            assertEquals(4, categories)
            assertEquals(
                listOf("id", "conversationId", "role", "text", "imagePath", "size", "quality", "status", "createdAt", "referenceImagePath", "actualSize", "warning", "errorMessage", "resultViewed"),
                aiMessageColumns
            )
            assertEquals(WatchStatusEntity.builtIns.size, rowCount(sqlite, "watch_statuses"))
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun roomUpgradeFrom14PreservesWatchAndAiDataAndAddsDefaults() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-14-17.db"
        context.deleteDatabase(databaseName)
        createVersion14Database(context, databaseName)

        val database = openMigratedDatabase(context, databaseName)

        try {
            val sqlite = database.openHelper.writableDatabase
            val watchRecord = sqlite.query("SELECT title, totalEpisodes, platform, status, lastWatchedAt FROM watch_records WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                listOf(cursor.getString(0), cursor.isNull(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4))
            }
            val aiMessage = sqlite.query("SELECT text, errorMessage, resultViewed FROM ai_messages WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                listOf(cursor.getString(0), cursor.getString(1), cursor.getInt(2))
            }

            assertEquals(listOf("旧追剧", true, "", WatchStatus.WATCHING.name, 0L), watchRecord)
            assertEquals(listOf("旧消息", "旧错误", 1), aiMessage)
            assertEquals(WatchStatusEntity.builtIns.size, rowCount(sqlite, "watch_statuses"))
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun openMigratedDatabase(context: Context, databaseName: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17
            )
            .build()

    private fun createVersion9Database(context: Context, databaseName: String) {
        createDatabase(context, databaseName, 9) { db ->
            db.execSQL(CREATE_COUNTDOWN_RECORDS)
            db.execSQL(
                "INSERT INTO countdown_records (id, type, name, dateTimeIso, calendarType, lunarYear, lunarMonth, lunarDay, lunarLeapMonth, reminderEnabled, reminderMinutesBefore, showYears, showMonths, showDays, showHours, showMinutes, showSeconds, showSolarDate, showLunarDate, cardBackgroundColor, cardTextColor, solarDisplayMask, lunarDisplayMask, countdownDisplayMask, cardGradientId, titleGradientId, solarGradientId, lunarGradientId, countdownGradientId, titleTextColor, solarTextColor, lunarTextColor, countdownTextColor, sortOrder, isPinned) VALUES (1, 'BIRTHDAY', '旧生日', '2020-01-01T00:00:00', 'SOLAR', NULL, NULL, NULL, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 63, 63, 63, 'solid', 'solid', 'solid', 'solid', 'solid', -1, -1, -1, -1, 1, 1)"
            )
        }
    }

    private fun createVersion14Database(context: Context, databaseName: String) {
        createDatabase(context, databaseName, 14) { db ->
            db.execSQL(CREATE_COUNTDOWN_RECORDS)
            db.execSQL(CREATE_WATCH_CATEGORIES)
            db.execSQL(CREATE_WATCH_RECORDS_14)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_records_categoryId` ON `watch_records` (`categoryId`)")
            db.execSQL(CREATE_AI_CONVERSATIONS)
            db.execSQL(CREATE_AI_MESSAGES_14)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_messages_conversationId` ON `ai_messages` (`conversationId`)")
            db.execSQL("INSERT INTO watch_categories (id, name, sortOrder) VALUES (1, '电视剧', 0)")
            db.execSQL("INSERT INTO watch_records (id, title, categoryId, currentEpisode, sortOrder) VALUES (1, '旧追剧', 1, 3, 0)")
            db.execSQL("INSERT INTO ai_conversations (id, mode, title, createdAt, updatedAt) VALUES (1, 'CHAT', '旧会话', 1, 1)")
            db.execSQL("INSERT INTO ai_messages (id, conversationId, role, text, imagePath, size, quality, status, createdAt, referenceImagePath, actualSize, warning, errorMessage) VALUES (1, 1, 'assistant', '旧消息', NULL, NULL, NULL, 'ERROR', 1, NULL, NULL, NULL, '旧错误')")
        }
    }

    private fun createDatabase(context: Context, databaseName: String, version: Int, create: (SupportSQLiteDatabase) -> Unit) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) = create(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase
        helper.close()
    }

    private fun columnNames(db: SupportSQLiteDatabase, table: String): List<String> =
        db.query("PRAGMA table_info($table)").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(1)) }
        }

    private fun rowCount(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun createVersion16Database(context: Context, databaseName: String) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(16) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(CREATE_COUNTDOWN_RECORDS)
                        db.execSQL(CREATE_WATCH_CATEGORIES)
                        db.execSQL(CREATE_WATCH_RECORDS)
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_records_categoryId` ON `watch_records` (`categoryId`)")
                        db.execSQL(CREATE_AI_CONVERSATIONS)
                        db.execSQL(CREATE_AI_MESSAGES)
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_messages_conversationId` ON `ai_messages` (`conversationId`)")

                        db.execSQL("INSERT INTO watch_categories (id, name, sortOrder) VALUES (1, '电视剧', 0)")
                        WatchStatus.entries.forEachIndexed { index, status ->
                            db.execSQL(
                                "INSERT INTO watch_records (title, categoryId, currentEpisode, totalEpisodes, platform, status, lastWatchedAt, sortOrder) VALUES (?, 1, 0, NULL, '', ?, 0, ?)",
                                arrayOf("记录$index", status.name, index)
                            )
                        }
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                })
                .build()
        )

        helper.writableDatabase
        helper.close()
    }

    private companion object {
        const val CREATE_COUNTDOWN_RECORDS = "CREATE TABLE countdown_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, name TEXT NOT NULL, dateTimeIso TEXT NOT NULL, calendarType TEXT NOT NULL, lunarYear INTEGER, lunarMonth INTEGER, lunarDay INTEGER, lunarLeapMonth INTEGER NOT NULL, reminderEnabled INTEGER NOT NULL, reminderMinutesBefore INTEGER NOT NULL, showYears INTEGER NOT NULL, showMonths INTEGER NOT NULL, showDays INTEGER NOT NULL, showHours INTEGER NOT NULL, showMinutes INTEGER NOT NULL, showSeconds INTEGER NOT NULL, showSolarDate INTEGER NOT NULL, showLunarDate INTEGER NOT NULL, cardBackgroundColor INTEGER NOT NULL, cardTextColor INTEGER NOT NULL, solarDisplayMask INTEGER NOT NULL, lunarDisplayMask INTEGER NOT NULL, countdownDisplayMask INTEGER NOT NULL, cardGradientId TEXT NOT NULL, titleGradientId TEXT NOT NULL, solarGradientId TEXT NOT NULL, lunarGradientId TEXT NOT NULL, countdownGradientId TEXT NOT NULL, titleTextColor INTEGER NOT NULL, solarTextColor INTEGER NOT NULL, lunarTextColor INTEGER NOT NULL, countdownTextColor INTEGER NOT NULL, sortOrder INTEGER NOT NULL, isPinned INTEGER NOT NULL)"
        const val CREATE_WATCH_CATEGORIES = "CREATE TABLE watch_categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL)"
        const val CREATE_WATCH_RECORDS_14 = "CREATE TABLE watch_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, categoryId INTEGER NOT NULL, currentEpisode INTEGER NOT NULL, sortOrder INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES watch_categories(id) ON UPDATE NO ACTION ON DELETE NO ACTION)"
        const val CREATE_WATCH_RECORDS = "CREATE TABLE watch_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, categoryId INTEGER NOT NULL, currentEpisode INTEGER NOT NULL, totalEpisodes INTEGER, platform TEXT NOT NULL, status TEXT NOT NULL, lastWatchedAt INTEGER NOT NULL, sortOrder INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES watch_categories(id) ON UPDATE NO ACTION ON DELETE NO ACTION)"
        const val CREATE_AI_CONVERSATIONS = "CREATE TABLE ai_conversations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mode TEXT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
        const val CREATE_AI_MESSAGES_14 = "CREATE TABLE ai_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, conversationId INTEGER NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, imagePath TEXT, size TEXT, quality TEXT, status TEXT NOT NULL, createdAt INTEGER NOT NULL, referenceImagePath TEXT, actualSize TEXT, warning TEXT, errorMessage TEXT, FOREIGN KEY(conversationId) REFERENCES ai_conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        const val CREATE_AI_MESSAGES = "CREATE TABLE ai_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, conversationId INTEGER NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, imagePath TEXT, referenceImagePath TEXT, size TEXT, quality TEXT, actualSize TEXT, warning TEXT, errorMessage TEXT, status TEXT NOT NULL, resultViewed INTEGER NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(conversationId) REFERENCES ai_conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
    }
}
