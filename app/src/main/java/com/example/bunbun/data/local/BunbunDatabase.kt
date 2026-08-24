package com.example.bunbun.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CachedCurrentUserEntity::class, CachedChatEntity::class, CachedMessageEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class BunbunDatabase : RoomDatabase() {
    abstract fun dao(): BunbunDao

    companion object {
        fun create(context: Context): BunbunDatabase = Room.databaseBuilder(
            context.applicationContext,
            BunbunDatabase::class.java,
            "bunbun-local.db",
        ).addMigrations(MIGRATION_1_2).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cached_chats ADD COLUMN peerLastSeenAtMillis INTEGER")
            }
        }
    }
}
