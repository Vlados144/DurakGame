package com.example.durakgame.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("durak_prefs", Context.MODE_PRIVATE)

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var defaultTimer: Int
        get() = prefs.getInt("default_timer", 0)
        set(value) = prefs.edit().putInt("default_timer", value).apply()

    var lastNickname: String
        get() = prefs.getString("last_nickname", "Игрок") ?: "Игрок"
        set(value) = prefs.edit().putString("last_nickname", value).apply()
}