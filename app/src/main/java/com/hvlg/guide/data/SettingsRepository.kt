package com.hvlg.guide.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ReaderSettings(
    val lastCardId: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val favoriteIds: Set<String> = emptySet(),
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hvlg_reader_settings",
)

/** 阅读位置与主题偏好的持久化（DataStore Preferences，纯本地）。 */
class SettingsRepository(private val context: Context) {

    val settings: Flow<ReaderSettings> = context.settingsDataStore.data.map { prefs ->
        ReaderSettings(
            lastCardId = prefs[KEY_LAST_CARD]?.takeIf { it.isNotBlank() },
            themeMode = prefs[KEY_THEME_MODE]?.let { raw ->
                runCatching { ThemeMode.valueOf(raw) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            favoriteIds = prefs[KEY_FAVORITES].orEmpty(),
        )
    }

    suspend fun setLastCardId(cardId: String) {
        context.settingsDataStore.edit { it[KEY_LAST_CARD] = cardId }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setFavorite(cardId: String, favorite: Boolean) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES].orEmpty()
            prefs[KEY_FAVORITES] = if (favorite) current + cardId else current - cardId
        }
    }

    private companion object {
        val KEY_LAST_CARD = stringPreferencesKey("last_card_id")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_FAVORITES = stringSetPreferencesKey("favorite_ids")
    }
}
