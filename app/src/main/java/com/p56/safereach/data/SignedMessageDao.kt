package com.p56.safereach.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SignedMessageDao {

    @Query("SELECT * FROM $TABLE_NAME WHERE id = $MESSAGE_ID AND dismissed = 0")
    fun observeLatest(): Flow<SignedMessageEntity?>

    @Query("SELECT * FROM $TABLE_NAME WHERE id = $MESSAGE_ID")
    suspend fun get(): SignedMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(signedMessage: SignedMessageEntity)

    @Transaction
    suspend fun upsertIfNewer(signedMessage: SignedMessageEntity) {
        val stored = get()
        if (stored == null || stored.timestamp < signedMessage.timestamp) {
            upsert(signedMessage)
        }
    }

    @Query("UPDATE $TABLE_NAME SET dismissed = 1 WHERE id = $MESSAGE_ID")
    suspend fun clear()

    companion object {
        const val MESSAGE_ID = 0
        const val TABLE_NAME = "signed_messages"
    }
}
