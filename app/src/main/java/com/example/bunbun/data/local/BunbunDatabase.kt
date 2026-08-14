package com.example.bunbun.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CachedCurrentUserEntity::class, CachedChatEntity::class, CachedMessageEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class BunbunDatabase : RoomDatabase() {
    abstract fun dao(): BunbunDao

    companion object {
        fun create(context: Context): BunbunDatabase = Room.databaseBuilder(
            context.applicationContext,
            BunbunDatabase::class.java,
            "bunbun-local.db",
        ).build()
    }
}
