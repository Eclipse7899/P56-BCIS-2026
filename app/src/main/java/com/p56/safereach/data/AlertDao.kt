package com.p56.safereach.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts WHERE expiresAt > (strftime('%s', 'now') * 1000) - 86400000 AND invalidated != 0 ORDER BY severity DESC, timestamp DESC")
    fun observeActive(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun findById(id: String): AlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: AlertEntity)

    @Transaction
    suspend fun upsertIfNewer(alert: AlertEntity) {
        val stored = findById(alert.id)
        if (stored == null || stored.version < alert.version) {
            upsert(alert)
        }
    }
}
