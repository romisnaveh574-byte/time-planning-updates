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
    fun migrationFrom16SeedsBuiltInsAndPreservesLegacyRecordStatusIds() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val databaseName = "watch-status-migration-${System.nanoTime()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(16) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE watch_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, status TEXT NOT NULL)")
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                })
                .build()
        )

        try {
            val database = helper.writableDatabase
            WatchStatus.entries.forEach { status ->
                database.execSQL(
                    "INSERT INTO watch_records (status) VALUES (?)",
                    arrayOf(status.name)
                )
            }

            AppDatabase.MIGRATION_16_17.migrate(database)

            val migratedStatuses = mutableListOf<WatchStatusEntity>()
            database.query("SELECT id, name, systemType, sortOrder FROM watch_statuses ORDER BY sortOrder").use { cursor ->
                while (cursor.moveToNext()) {
                    migratedStatuses += WatchStatusEntity(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        systemType = cursor.getString(2),
                        sortOrder = cursor.getInt(3)
                    )
                }
            }
            val recordStatusIds = mutableListOf<String>()
            database.query("SELECT status FROM watch_records ORDER BY id").use { cursor ->
                while (cursor.moveToNext()) recordStatusIds += cursor.getString(0)
            }

            assertEquals(WatchStatusEntity.builtIns, migratedStatuses)
            assertEquals(WatchStatus.entries.map { it.name }, recordStatusIds)
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }
    }
}
