package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.GameSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "iron_warfare_settings")

class SettingsRepository(private val context: Context) {
  private object Keys {
    val UNIT_CAP = intPreferencesKey("unit_cap")
    val AUTOSAVE_ENABLED = booleanPreferencesKey("autosave_enabled")
    val GAME_VOLUME = floatPreferencesKey("game_volume")
    val INTERFACE_VOLUME = floatPreferencesKey("interface_volume")
    val MUSIC_VOLUME = floatPreferencesKey("music_volume")
    val BATTERY_SAVING = booleanPreferencesKey("battery_saving")
    val HIGH_REFRESH_RATE = booleanPreferencesKey("high_refresh_rate")
    val SHOW_WAYPOINTS = booleanPreferencesKey("show_waypoints")
    val ALWAYS_SHOW_HEALTH = booleanPreferencesKey("always_show_health")
    val SHOW_ZOOM_BUTTONS = booleanPreferencesKey("show_zoom_buttons")
    val RENDER_CLOUDS = booleanPreferencesKey("render_clouds")
    val SHOW_FPS = booleanPreferencesKey("show_fps")
  }

  val settingsFlow: Flow<GameSettings> =
    context.settingsDataStore.data.map { prefs ->
      GameSettings(
        unitCap = prefs[Keys.UNIT_CAP] ?: 500,
        autosaveEnabled = prefs[Keys.AUTOSAVE_ENABLED] ?: true,
        gameVolume = prefs[Keys.GAME_VOLUME] ?: 0.8f,
        interfaceVolume = prefs[Keys.INTERFACE_VOLUME] ?: 0.85f,
        musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.45f,
        batterySavingMode = prefs[Keys.BATTERY_SAVING] ?: false,
        highRefreshRate = prefs[Keys.HIGH_REFRESH_RATE] ?: true,
        showUnitWaypoints = prefs[Keys.SHOW_WAYPOINTS] ?: true,
        alwaysShowUnitHealth = prefs[Keys.ALWAYS_SHOW_HEALTH] ?: false,
        showZoomButtons = prefs[Keys.SHOW_ZOOM_BUTTONS] ?: true,
        renderClouds = prefs[Keys.RENDER_CLOUDS] ?: true,
        showFps = prefs[Keys.SHOW_FPS] ?: false,
      )
    }

  suspend fun saveSettings(settings: GameSettings) {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.UNIT_CAP] = settings.unitCap
      prefs[Keys.AUTOSAVE_ENABLED] = settings.autosaveEnabled
      prefs[Keys.GAME_VOLUME] = settings.gameVolume
      prefs[Keys.INTERFACE_VOLUME] = settings.interfaceVolume
      prefs[Keys.MUSIC_VOLUME] = settings.musicVolume
      prefs[Keys.BATTERY_SAVING] = settings.batterySavingMode
      prefs[Keys.HIGH_REFRESH_RATE] = settings.highRefreshRate
      prefs[Keys.SHOW_WAYPOINTS] = settings.showUnitWaypoints
      prefs[Keys.ALWAYS_SHOW_HEALTH] = settings.alwaysShowUnitHealth
      prefs[Keys.SHOW_ZOOM_BUTTONS] = settings.showZoomButtons
      prefs[Keys.RENDER_CLOUDS] = settings.renderClouds
      prefs[Keys.SHOW_FPS] = settings.showFps
    }
  }
}
