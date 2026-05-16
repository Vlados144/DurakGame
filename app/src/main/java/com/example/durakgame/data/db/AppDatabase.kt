package com.example.durakgame.data.db

import android.content.Context

class AppDatabase private constructor(context: Context) {

    val databaseHelper = DatabaseHelper(context)

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context).also { INSTANCE = it }
            }
        }
    }
}