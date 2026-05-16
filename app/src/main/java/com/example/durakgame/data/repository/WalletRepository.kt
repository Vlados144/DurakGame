package com.example.durakgame.data.repository

import com.example.durakgame.data.db.DatabaseHelper

class WalletRepository(private val dbHelper: DatabaseHelper) {

    fun getBalance(): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT balance FROM user WHERE id = 1", null)
        return if (cursor.moveToFirst()) {
            cursor.getLong(0)
        } else {
            1000
        }.also { cursor.close() }
    }

    fun addFunds(amount: Long) {
        val db = dbHelper.writableDatabase
        db.execSQL("UPDATE user SET balance = balance + ? WHERE id = 1", arrayOf(amount))
    }

    fun withdraw(amount: Long): Boolean {
        val current = getBalance()
        if (current < amount) return false
        addFunds(-amount)
        return true
    }
}