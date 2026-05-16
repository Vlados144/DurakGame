package com.example.durakgame.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey
    val id: Int = 1,
    val nickname: String = "Игрок",
    val avatarId: Int = 0,
    val balance: Long = 1000,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0
)