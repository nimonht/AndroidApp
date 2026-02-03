package com.example.androidapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.androidapp.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

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
     * Get a user by ID as a Flow for observing changes.
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUserById(userId: String): Flow<UserEntity?>

    /**
     * Get a user by email.
     */
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    /**
     * Get a user by username.
     */
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?

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
     * Soft delete a user by setting deletedAt timestamp.
     */
    @Query("UPDATE users SET deleted_at = :deletedAt WHERE id = :userId")
    suspend fun softDeleteUser(userId: String, deletedAt: Long = System.currentTimeMillis())
}
