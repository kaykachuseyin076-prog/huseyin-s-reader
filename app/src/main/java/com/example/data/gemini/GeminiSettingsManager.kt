package com.example.data.gemini

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

object GeminiSettingsManager {
    private const val PREFS_NAME = "gemini_ai_prefs"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Gets the effective API key.
     * Prioritizes custom key entered by the user in Settings.
     * Falls back to BuildConfig.GEMINI_API_KEY if available and not a placeholder.
     */
    fun getEffectiveApiKey(context: Context): String? {
        val customKey = getCustomApiKey(context)
        if (!customKey.isNullOrBlank()) {
            return customKey.trim()
        }

        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            null
        }

        if (!buildKey.isNullOrBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey.trim()
        }

        return null
    }

    fun hasApiKey(context: Context): Boolean {
        return !getEffectiveApiKey(context).isNullOrBlank()
    }

    fun getCustomApiKey(context: Context): String? {
        return getPrefs(context).getString(KEY_CUSTOM_API_KEY, null)
    }

    fun isCustomKeySaved(context: Context): Boolean {
        return !getCustomApiKey(context).isNullOrBlank()
    }

    fun saveApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_API_KEY, apiKey.trim()).apply()
    }

    fun deleteApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_CUSTOM_API_KEY).apply()
    }

    /**
     * Formats API key for safe masked display (never displays the full key in UI).
     */
    fun getMaskedApiKey(context: Context): String? {
        val key = getEffectiveApiKey(context) ?: return null
        if (key.length <= 8) {
            return "••••••••"
        }
        val prefix = key.take(4)
        val suffix = key.takeLast(4)
        return "$prefix••••••••$suffix"
    }
}
