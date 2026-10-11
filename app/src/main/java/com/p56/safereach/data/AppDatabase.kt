package com.p56.safereach.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AlertEntity::class, SignedMessageEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    abstract fun signedMessageDao(): SignedMessageDao
}
