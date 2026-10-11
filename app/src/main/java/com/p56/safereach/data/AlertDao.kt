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
    suspend fun findById(id: String): AlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: AlertEntity)

    @Transaction
    suspend fun upsertIfNewer(alert: AlertEntity): AlertUpsertResult {
        val stored = findById(alert.id)
        return when {
            stored == null -> {
                upsert(alert)
                AlertUpsertResult.INSERTED
            }

            stored.version < alert.version -> {
                upsert(alert)
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
