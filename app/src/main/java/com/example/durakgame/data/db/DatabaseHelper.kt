package com.example.durakgame.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, "durak_database", null, 1
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE user (
                id INTEGER PRIMARY KEY,
                nickname TEXT DEFAULT 'Игрок',
                avatar_id INTEGER DEFAULT 0,
                balance INTEGER DEFAULT 1000,
                games_played INTEGER DEFAULT 0,
                games_won INTEGER DEFAULT 0
            )
        """)
        db.execSQL("INSERT INTO user (id, nickname, balance) VALUES (1, 'Игрок', 1000)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS user")
        onCreate(db)
    }
}