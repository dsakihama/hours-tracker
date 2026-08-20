package com.dean.hourstracker.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [Project::class, Entry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hours_tracker.db",
                )
                    .addCallback(SeedCallback)
                    .build()
                    .also { instance = it }
            }

        private val SeedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                val presets = listOf(
                    "FC Portland",
                    "Tumwater",
                    "Findley",
                )
                presets.forEachIndexed { index, name ->
                    db.execSQL(
                        "INSERT INTO projects (name, isPreset, isArchived, createdAt) VALUES (?, 1, 0, ?)",
                        arrayOf(name, now + index),
                    )
                }
            }
        }
    }
}
