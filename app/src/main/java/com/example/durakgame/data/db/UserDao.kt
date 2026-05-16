package com.example.durakgame.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Query("SELECT * FROM user WHERE id = 1")
    suspend fun getUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    @Query("UPDATE user SET nickname = :nickname WHERE id = 1")
    suspend fun updateNickname(nickname: String)

    @Query("UPDATE user SET avatarId = :avatarId WHERE id = 1")
    suspend fun updateAvatarId(avatarId: Int)

    @Query("UPDATE user SET balance = balance + :delta WHERE id = 1")
    suspend fun addToBalance(delta: Long)

    @Query("UPDATE user SET gamesPlayed = gamesPlayed + 1, gamesWon = gamesWon + :wonIncrement WHERE id = 1")
    suspend fun incrementGames(wonIncrement: Int)
}