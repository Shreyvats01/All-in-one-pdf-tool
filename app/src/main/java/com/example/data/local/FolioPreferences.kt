package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettingsState(
    val onboardingCompleted: Boolean = true,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val dynamicColor: Boolean = false,
    val languageCode: String = "English",
    val outputNamingPattern: String = "{name}_{tool}",
    val customOutputFolderUri: String = "",
    val connectedFolderName: String = "",
    val defaultCompression: String = "MEDIUM",
    val defaultDpi: Int = 150,
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "1234",
    val useBiometric: Boolean = true,
    val autoLockTimeoutMinutes: Int = 5,
    val hideInRecents: Boolean = false,
    val keepScreenOnInReader: Boolean = true,
    val volumeKeyPaging: Boolean = false,
    val tapZonePaging: Boolean = true,
    val reduceMotion: Boolean = false,
    val recentToolIds: List<String> = listOf("MERGE_PDF", "SCAN_TO_PDF", "SIGN_PDF", "SPLIT_PDF"),
    val searchHistory: List<String> = emptyList()
)

class FolioPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("folio_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadState())
    val settings: StateFlow<UserSettingsState> = _settings.asStateFlow()

    private fun loadState(): UserSettingsState {
        val recentToolsRaw = prefs.getString("recent_tools", "MERGE_PDF,SCAN_TO_PDF,SIGN_PDF,SPLIT_PDF") ?: ""
        val historyRaw = prefs.getString("search_history", "") ?: ""
        val cleanHistory = historyRaw.split("|")
            .filter { it.isNotBlank() && it !in setOf("Architecture", "Financial", "Agreement", "Confidential") }
        return UserSettingsState(
            onboardingCompleted = prefs.getBoolean("onboarding_completed", true),
            themeMode = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM",
            dynamicColor = prefs.getBoolean("dynamic_color", false),
            languageCode = prefs.getString("language_code", "English") ?: "English",
            outputNamingPattern = prefs.getString("output_naming", "{name}_{tool}") ?: "{name}_{tool}",
            customOutputFolderUri = prefs.getString("custom_output_uri", "") ?: "",
            connectedFolderName = prefs.getString("connected_folder_name", "") ?: "",
            defaultCompression = prefs.getString("default_compression", "MEDIUM") ?: "MEDIUM",
            defaultDpi = prefs.getInt("default_dpi", 150),
            appLockEnabled = prefs.getBoolean("app_lock_enabled", false),
            appLockPin = prefs.getString("app_lock_pin", "1234") ?: "1234",
            useBiometric = prefs.getBoolean("use_biometric", true),
            autoLockTimeoutMinutes = prefs.getInt("auto_lock_timeout", 5),
            hideInRecents = prefs.getBoolean("hide_in_recents", false),
            keepScreenOnInReader = prefs.getBoolean("keep_screen_on", true),
            volumeKeyPaging = prefs.getBoolean("volume_key_paging", false),
            tapZonePaging = prefs.getBoolean("tap_zone_paging", true),
            reduceMotion = prefs.getBoolean("reduce_motion", false),
            recentToolIds = recentToolsRaw.split(",").filter { it.isNotBlank() },
            searchHistory = cleanHistory
        )
    }

    fun update(transform: (UserSettingsState) -> UserSettingsState) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.edit()
            .putBoolean("onboarding_completed", updated.onboardingCompleted)
            .putString("theme_mode", updated.themeMode)
            .putBoolean("dynamic_color", updated.dynamicColor)
            .putString("language_code", updated.languageCode)
            .putString("output_naming", updated.outputNamingPattern)
            .putString("custom_output_uri", updated.customOutputFolderUri)
            .putString("connected_folder_name", updated.connectedFolderName)
            .putString("default_compression", updated.defaultCompression)
            .putInt("default_dpi", updated.defaultDpi)
            .putBoolean("app_lock_enabled", updated.appLockEnabled)
            .putString("app_lock_pin", updated.appLockPin)
            .putBoolean("use_biometric", updated.useBiometric)
            .putInt("auto_lock_timeout", updated.autoLockTimeoutMinutes)
            .putBoolean("hide_in_recents", updated.hideInRecents)
            .putBoolean("keep_screen_on", updated.keepScreenOnInReader)
            .putBoolean("volume_key_paging", updated.volumeKeyPaging)
            .putBoolean("tap_zone_paging", updated.tapZonePaging)
            .putBoolean("reduce_motion", updated.reduceMotion)
            .putString("recent_tools", updated.recentToolIds.joinToString(","))
            .putString("search_history", updated.searchHistory.take(10).joinToString("|"))
            .apply()
    }

    fun recordToolUsed(toolIdName: String) {
        update { current ->
            val updatedList = (listOf(toolIdName) + current.recentToolIds.filter { it != toolIdName }).take(8)
            current.copy(recentToolIds = updatedList)
        }
    }

    fun recordSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        update { current ->
            val updatedList = (listOf(trimmed) + current.searchHistory.filter { !it.equals(trimmed, ignoreCase = true) }).take(8)
            current.copy(searchHistory = updatedList)
        }
    }
}
