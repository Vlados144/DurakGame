package com.example.durakgame

import android.app.Application
import com.example.durakgame.data.PreferencesManager
import com.example.durakgame.data.db.AppDatabase
import com.example.durakgame.data.repository.UserRepository
import com.example.durakgame.data.repository.WalletRepository

class DurakApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var walletRepository: WalletRepository
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        userRepository = UserRepository(database.databaseHelper)
        walletRepository = WalletRepository(database.databaseHelper)
        preferencesManager = PreferencesManager(this)
    }
}