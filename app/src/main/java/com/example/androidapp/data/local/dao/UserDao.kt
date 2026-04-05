package com.example.androidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.androidapp.data.local.entity.UserEntity

/**
 * Data Access Object for User entities.
 * Provides methods to query, insert, update, and delete users in the local database.
 */
@Dao
interface UserDao {

    /**
     * Get a user by ID.
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    /**
     * Insert a user, replacing if it already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Update an existing user.
     */
    @Update
    suspend fun updateUser(user: UserEntity)

    /**
     * Delete a user.
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)

    /**
     * Permanently delete a user by ID.
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)
}
