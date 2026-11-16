package com.battleground.royale3d.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.battleground.royale3d.model.AppLanguage
import com.battleground.royale3d.model.CameraPerspective
import com.battleground.royale3d.model.GameSettings
import com.battleground.royale3d.model.GraphicsQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "br_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SENSITIVITY = floatPreferencesKey("look_sensitivity")
        val PERSPECTIVE = stringPreferencesKey("default_perspective")
        val QUALITY = stringPreferencesKey("graphics_quality")
        val LANGUAGE = stringPreferencesKey("app_language")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val WS_URL = stringPreferencesKey("ws_server_url")
        val VAULT_UNLOCKED = stringSetPreferencesKey("vault_unlocked_ids")
        val EQUIPPED_WEAPON = stringPreferencesKey("equipped_weapon_id")
        val EQUIPPED_JERSEY = intPreferencesKey("equipped_jersey_variant")
    }

    val settingsFlow: Flow<GameSettings> = context.settingsDataStore.data.map { prefs ->
        decode(prefs)
    }

    suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        context.settingsDataStore.edit { prefs ->
            val updated = transform(decode(prefs))
            prefs[Keys.SENSITIVITY] = updated.lookSensitivity
            prefs[Keys.PERSPECTIVE] = updated.defaultPerspective.name
            prefs[Keys.QUALITY] = updated.graphicsQuality.name
            prefs[Keys.LANGUAGE] = updated.language.name
            prefs[Keys.HAPTICS] = updated.hapticsEnabled
            prefs[Keys.WS_URL] = updated.wsServerUrl
            prefs[Keys.VAULT_UNLOCKED] = updated.vaultUnlockedIds
            prefs[Keys.EQUIPPED_WEAPON] = updated.equippedWeaponId
            prefs[Keys.EQUIPPED_JERSEY] = updated.equippedJerseyVariant
        }
    }

    private fun decode(prefs: Preferences): GameSettings {
        return GameSettings(
            lookSensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f,
            defaultPerspective = prefs[Keys.PERSPECTIVE]?.let {
                runCatching { CameraPerspective.valueOf(it) }.getOrNull()
            } ?: CameraPerspective.THIRD_PERSON,
            graphicsQuality = prefs[Keys.QUALITY]?.let {
                runCatching { GraphicsQuality.valueOf(it) }.getOrNull()
            } ?: GraphicsQuality.MEDIUM,
            language = prefs[Keys.LANGUAGE]?.let {
                runCatching { AppLanguage.valueOf(it) }.getOrNull()
            } ?: AppLanguage.BENGALI,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            wsServerUrl = prefs[Keys.WS_URL] ?: "wss://echo.websocket.events",
            vaultUnlockedIds = prefs[Keys.VAULT_UNLOCKED] ?: setOf("jersey", "ff_gun"),
            equippedWeaponId = prefs[Keys.EQUIPPED_WEAPON] ?: "ff_gun",
            equippedJerseyVariant = prefs[Keys.EQUIPPED_JERSEY] ?: 0
        )
    }
}
