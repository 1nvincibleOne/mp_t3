package com.example.frontdesk.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StayDao {
    // Emits again every time the table changes
    @Query("SELECT * FROM stays ORDER BY id")
    fun observeAll(): Flow<List<Stay>>

    @Query("SELECT * FROM stays")
    suspend fun getAll(): List<Stay>

    @Query("SELECT * FROM stays WHERE id = :id")
    suspend fun getById(id: Long): Stay?

    @Insert
    suspend fun insert(stay: Stay): Long

    @Update
    suspend fun update(stay: Stay)

    @Delete
    suspend fun delete(stay: Stay)
}
