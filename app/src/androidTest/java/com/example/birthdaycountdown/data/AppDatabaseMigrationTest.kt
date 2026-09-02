package com.example.birthdaycountdown.data

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
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase(DATABASE_NAME)
        createVersion16Database(context, DATABASE_NAME)

        val database = AppDatabase.create(context)

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
            context.deleteDatabase(DATABASE_NAME)
        }
    }

    private fun createVersion16Database(context: android.content.Context, databaseName: String) {
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
        const val DATABASE_NAME = "countdown.db"
        const val CREATE_COUNTDOWN_RECORDS = "CREATE TABLE countdown_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, name TEXT NOT NULL, dateTimeIso TEXT NOT NULL, calendarType TEXT NOT NULL, lunarYear INTEGER, lunarMonth INTEGER, lunarDay INTEGER, lunarLeapMonth INTEGER NOT NULL, reminderEnabled INTEGER NOT NULL, reminderMinutesBefore INTEGER NOT NULL, showYears INTEGER NOT NULL, showMonths INTEGER NOT NULL, showDays INTEGER NOT NULL, showHours INTEGER NOT NULL, showMinutes INTEGER NOT NULL, showSeconds INTEGER NOT NULL, showSolarDate INTEGER NOT NULL, showLunarDate INTEGER NOT NULL, cardBackgroundColor INTEGER NOT NULL, cardTextColor INTEGER NOT NULL, solarDisplayMask INTEGER NOT NULL, lunarDisplayMask INTEGER NOT NULL, countdownDisplayMask INTEGER NOT NULL, cardGradientId TEXT NOT NULL, titleGradientId TEXT NOT NULL, solarGradientId TEXT NOT NULL, lunarGradientId TEXT NOT NULL, countdownGradientId TEXT NOT NULL, titleTextColor INTEGER NOT NULL, solarTextColor INTEGER NOT NULL, lunarTextColor INTEGER NOT NULL, countdownTextColor INTEGER NOT NULL, sortOrder INTEGER NOT NULL, isPinned INTEGER NOT NULL)"
        const val CREATE_WATCH_CATEGORIES = "CREATE TABLE watch_categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sortOrder INTEGER NOT NULL)"
        const val CREATE_WATCH_RECORDS = "CREATE TABLE watch_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, categoryId INTEGER NOT NULL, currentEpisode INTEGER NOT NULL, totalEpisodes INTEGER, platform TEXT NOT NULL, status TEXT NOT NULL, lastWatchedAt INTEGER NOT NULL, sortOrder INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES watch_categories(id) ON UPDATE NO ACTION ON DELETE NO ACTION)"
        const val CREATE_AI_CONVERSATIONS = "CREATE TABLE ai_conversations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mode TEXT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
        const val CREATE_AI_MESSAGES = "CREATE TABLE ai_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, conversationId INTEGER NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, imagePath TEXT, referenceImagePath TEXT, size TEXT, quality TEXT, actualSize TEXT, warning TEXT, errorMessage TEXT, status TEXT NOT NULL, resultViewed INTEGER NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(conversationId) REFERENCES ai_conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
    }
}
