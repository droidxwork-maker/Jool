package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GameSettings(
  val unitCap: Int = 500,
  val autosaveEnabled: Boolean = true,
  val gameVolume: Float = 0.8f,
  val interfaceVolume: Float = 0.85f,
  val musicVolume: Float = 0.45f,
  val batterySavingMode: Boolean = false,
  val highRefreshRate: Boolean = true,
  val showUnitWaypoints: Boolean = true,
  val alwaysShowUnitHealth: Boolean = false,
  val showZoomButtons: Boolean = true,
  val renderClouds: Boolean = true,
  val showFps: Boolean = false,
  val cameraShakeEnabled: Boolean = true,
  val announcerVoiceEnabled: Boolean = true,
) {
  val targetFps: Int
    get() =
      when {
        batterySavingMode -> 30
        highRefreshRate -> 60
        else -> 45
      }
}
