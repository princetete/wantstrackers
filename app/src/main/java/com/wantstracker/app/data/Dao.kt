package com.wantstracker.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert
    suspend fun insert(c: CategoryEntity): Long

    @Insert
    suspend fun insertAll(list: List<CategoryEntity>)

    @Update
    suspend fun update(c: CategoryEntity)

    @Delete
    suspend fun delete(c: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface WantDao {
    @Query("SELECT * FROM wants ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WantEntity>>

    @Query("SELECT * FROM wants WHERE id = :id")
    fun observe(id: Long): Flow<WantEntity?>

    @Query("SELECT * FROM wants WHERE id = :id")
    suspend fun get(id: Long): WantEntity?

    @Query("SELECT * FROM wants")
    suspend fun getAll(): List<WantEntity>

    @Insert
    suspend fun insert(w: WantEntity): Long

    @Insert
    suspend fun insertAll(list: List<WantEntity>)

    @Update
    suspend fun update(w: WantEntity)

    @Query("DELETE FROM wants WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM wants")
    suspend fun deleteAll()
}

@Dao
interface LogDao {
    @Query("SELECT * FROM status_logs WHERE wantId = :wantId ORDER BY at ASC, id ASC")
    fun observeFor(wantId: Long): Flow<List<StatusLogEntity>>

    @Query("SELECT * FROM status_logs")
    suspend fun getAll(): List<StatusLogEntity>

    @Insert
    suspend fun insert(l: StatusLogEntity): Long

    @Insert
    suspend fun insertAll(list: List<StatusLogEntity>)

    @Query("DELETE FROM status_logs")
    suspend fun deleteAll()
}
