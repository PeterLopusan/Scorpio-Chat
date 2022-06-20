package com.example.scorpiochat

import android.content.Context

object SharedPreferencesManager {

    private const val sharedPrefName = ""

    fun getTopic(context: Context): String? {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        return sharedPref.getString(context.getString(R.string.shared_pref_topic), null)
    }

    fun setTopic(context: Context, topic: String) {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        sharedPref.edit().putString(context.getString(R.string.shared_pref_topic), topic).apply()
    }

    fun getDarkTheme(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(context.getString(R.string.shared_pref_dark_theme), false)
    }

    fun setDarkTheme(context: Context, darkTheme: Boolean) {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(context.getString(R.string.shared_pref_dark_theme), darkTheme).apply()
    }

    fun getIfUserIsMuted(context: Context, userId: String): Boolean {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(userId, false)
    }

    fun setIfUserIsMuted(context: Context, muteUser: Boolean, userId: String) {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(userId, muteUser).apply()
    }
}