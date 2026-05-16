package com.example.durakgame.data.repository

import android.content.ContentValues
import com.example.durakgame.data.db.DatabaseHelper

class UserRepository(private val dbHelper: DatabaseHelper) {

    fun getUser(): User {
        val db = dbHelper.readableDatabase
        val cursor = db.query("user", null, "id = ?", arrayOf("1"), null, null, null)

        return if (cursor.moveToFirst()) {
            User(
                nickname = cursor.getString(cursor.getColumnIndexOrThrow("nickname")),
                avatarId = cursor.getInt(cursor.getColumnIndexOrThrow("avatar_id")),
                balance = cursor.getLong(cursor.getColumnIndexOrThrow("balance")),
                gamesPlayed = cursor.getInt(cursor.getColumnIndexOrThrow("games_played")),
                gamesWon = cursor.getInt(cursor.getColumnIndexOrThrow("games_won"))
            )
        } else {
            // Если записи нет — создаём
            val values = ContentValues().apply {
                put("id", 1)
                put("nickname", "Игрок")
                put("balance", 1000)
            }
            db.insert("user", null, values)
            User()
        }.also { cursor.close() }
    }

    fun updateNickname(nickname: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("nickname", nickname) }
        db.update("user", values, "id = ?", arrayOf("1"))
    }

    fun updateAvatarId(avatarId: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("avatar_id", avatarId) }
        db.update("user", values, "id = ?", arrayOf("1"))
    }

    fun addToBalance(delta: Long) {
        val db = dbHelper.writableDatabase
        db.execSQL("UPDATE user SET balance = balance + ? WHERE id = 1", arrayOf(delta))
    }

    fun incrementGames(won: Boolean) {
        val db = dbHelper.writableDatabase
        val wonIncrement = if (won) 1 else 0
        db.execSQL(
            "UPDATE user SET games_played = games_played + 1, games_won = games_won + ? WHERE id = 1",
            arrayOf(wonIncrement)
        )
    }
}

data class User(
    val nickname: String = "Игрок",
    val avatarId: Int = 0,
    val balance: Long = 1000,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0
)