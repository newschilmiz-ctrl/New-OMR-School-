package com.example.util

import android.content.Context
import android.content.SharedPreferences

object SyncPreferences {
    private const val PREF_NAME = "omr_sync_preferences"
    private const val KEY_MYSQL_ENABLED = "mysql_sync_enabled"
    private const val KEY_MYSQL_SERVER_URL = "mysql_server_url"
    private const val KEY_MYSQL_API_KEY = "mysql_api_key"
    private const val KEY_FIREBASE_ENABLED = "firebase_sync_enabled"
    private const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isMySqlSyncEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MYSQL_ENABLED, false)
    }

    fun setMySqlSyncEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MYSQL_ENABLED, enabled).apply()
    }

    fun getMySqlServerUrl(context: Context): String {
        return getPrefs(context).getString(KEY_MYSQL_SERVER_URL, "") ?: ""
    }

    fun setMySqlServerUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_MYSQL_SERVER_URL, url.trim()).apply()
    }

    fun getMySqlApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_MYSQL_API_KEY, "") ?: ""
    }

    fun setMySqlApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_MYSQL_API_KEY, apiKey.trim()).apply()
    }

    fun isFirebaseSyncEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIREBASE_ENABLED, true)
    }

    fun setFirebaseSyncEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FIREBASE_ENABLED, enabled).apply()
    }

    fun getLastSyncTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun setLastSyncTime(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_SYNC_TIME, time).apply()
    }
}
