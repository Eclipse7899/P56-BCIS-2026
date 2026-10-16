package com.p56.safereach.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun findById(id: Int): AlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: AlertEntity)

    @Transaction
    suspend fun upsertIfNewer(newAlert: AlertEntity): AlertUpsertResult {
        val stored = findById(newAlert.id)
        return when {
            stored == null -> {
                upsert(newAlert)
                AlertUpsertResult.INSERTED
            }

            stored.timestamp < newAlert.timestamp -> {
                upsert(newAlert)
                AlertUpsertResult.UPDATED
            }

            else -> AlertUpsertResult.IGNORED
        }
    }
}

enum class AlertUpsertResult {
    INSERTED,
    UPDATED,
    IGNORED,
}
