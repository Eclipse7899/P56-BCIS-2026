package com.p56.safereach.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignedMessageDao {

    @Query("SELECT * FROM signed_messages WHERE id = 0")
    fun observeLatest(): Flow<SignedMessageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: SignedMessageEntity)

    @Query("DELETE FROM signed_messages WHERE id = 0")
    suspend fun clear()
}
