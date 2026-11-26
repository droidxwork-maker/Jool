package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameSettings
import com.example.ui.theme.SteelDark
import com.example.ui.theme.SteelMedium
import com.example.ui.theme.TacticalGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
  currentSettings: GameSettings,
  onSaveAndDone: (GameSettings) -> Unit,
  onCancel: () -> Unit,
  onPlayClick: () -> Unit,
) {
  var draft by remember(currentSettings) { mutableStateOf(currentSettings) }
  var unitCapMenuOpen by remember { mutableStateOf(false) }
  var showCreditsDialog by remember { mutableStateOf(false) }

  BackHandler { onCancel() }

  RivetedMetalBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(16.dp)
    ) {
      MetalPanel(modifier = Modifier.weight(1f).fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
          // 1. General & Storage
          GreenSectionHeader(title = "General & Storage")
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Singleplayer Unit Cap",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Maximum mobile units allowed per team",
                color = TextSecondary,
                fontSize = 11.sp,
              )
            }
            Box {
              TacticalButton(
                text = "${draft.unitCap} Units",
                onClick = {
                  onPlayClick()
                  unitCapMenuOpen = true
                },
                testTag = "settings_unit_cap_button",
              )
              DropdownMenu(
                expanded = unitCapMenuOpen,
                onDismissRequest = { unitCapMenuOpen = false },
                containerColor = SteelMedium,
              ) {
                listOf(250, 500, 1000, 2000).forEach { cap ->
                  DropdownMenuItem(
                    text = { Text("$cap Units", color = TextPrimary) },
                    onClick = {
                      onPlayClick()
                      draft = draft.copy(unitCap = cap)
                      unitCapMenuOpen = false
                    },
                  )
                }
              }
            }
          }
          SettingCheckboxRow(
            label = "Autosave Enabled",
            subtitle = "Automatically save active match progress every 45 seconds",
            checked = draft.autosaveEnabled,
            onCheckedChange = {
              onPlayClick()
              draft = draft.copy(autosaveEnabled = it)
            },
            testTag = "settings_autosave_checkbox",
          )

          // 2. Audio
          GreenSectionHeader(title = "Audio")
          SettingSliderRow(
            label = "Game Volume",
            value = draft.gameVolume,
            onValueChange = { draft = draft.copy(gameVolume = it) },
            testTag = "settings_game_volume_slider",
          )
          SettingSliderRow(
            label = "Interface Volume",
            value = draft.interfaceVolume,
            onValueChange = { draft = draft.copy(interfaceVolume = it) },
            testTag = "settings_interface_volume_slider",
          )
          SettingSliderRow(
            label = "Music Volume",
            value = draft.musicVolume,
            onValueChange = { draft = draft.copy(musicVolume = it) },
            testTag = "settings_music_volume_slider",
          )

          // 3. Graphics
          GreenSectionHeader(title = "Graphics")
          SettingCheckboxRow(
            label = "Battery Saving Mode",
            subtitle = "Limits simulation & rendering to 30 FPS",
            checked = draft.batterySavingMode,
            onCheckedChange = {
              onPlayClick()
              draft = draft.copy(batterySavingMode = it, highRefreshRate = if (it) false else draft.highRefreshRate)
            },
            testTag = "settings_battery_checkbox",
          )
          SettingCheckboxRow(
            label = "High Refresh Rate",
            subtitle = "Targets 60 FPS smooth scrolling and animations",
            checked = draft.highRefreshRate,
            onCheckedChange = {
              onPlayClick()
              draft = draft.copy(highRefreshRate = it, batterySavingMode = if (it) false else draft.batterySavingMode)
            },
            testTag = "settings_high_refresh_checkbox",
          )
          SettingCheckboxRow(
            label = "Show Unit Waypoints",
            subtitle = "Draw tactical movement path lines for selected units",
            checked = draft.showUnitWaypoints,
            onCheckedChange = {
              onPlayClick()
              draft = draft.copy(showUnitWaypoints = it)
            },
            testTag = "settings_waypoints_checkbox",
          )
          SettingCheckboxRow(
            label = "Always Show Unit Health",
            subtitle = "Display health bars above all units even at full HP",
            checked = draft.alwaysShowUnitHealth,
            onCheckedChange = {
              onPlayClick()
              draft = draft.copy(alwaysShowUnitHealth = it)
            },
            testTag = "settings_always_health_checkbox",
          )
          SettingCheckboxRow(
            label = "Show Zoom Buttons",
            subtitle = "Display + and - zoom controls on the battlefield",
            checked = draft.showZoomButtons,
            onCheckedChange = {
              onPlayClick()
              draft = draft.copy(showZoomButtons = it)
            },
            testTag = "settings_zoom_buttons_checkbox",
          )
          SettingCheckboxRow(
            label = "Render Clouds",
            subtitle = "Render drifting cloud shadows across the terrain",
            checked = draft.renderClouds,
            onCheckedChange = {
              onPlayClick()
              draft = draft.copy(renderClouds = it)
            },
            testTag = "settings_clouds_checkbox",
          )

          // 4. Developer / Debug
          GreenSectionHeader(title = "Developer / Debug")
          SettingCheckboxRow(
            label = "Show FPS",
            subtitle = "Display live frames-per-second counter in the top HUD",
            checked = draft.showFps,
            onCheckedChange = {
              onPlayClick()
              draft = draft.copy(showFps = it)
            },
            testTag = "settings_fps_checkbox",
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TacticalButton(
          text = "Credits",
          onClick = {
            onPlayClick()
            showCreditsDialog = true
          },
          testTag = "settings_credits_button",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          TacticalButton(
            text = "Cancel",
            onClick = onCancel,
            testTag = "settings_cancel_button",
          )
          TacticalButton(
            text = "Done",
            onClick = { onSaveAndDone(draft) },
            isPrimary = true,
            testTag = "settings_done_button",
          )
        }
      }
    }

    if (showCreditsDialog) {
      AlertDialog(
        onDismissRequest = { showCreditsDialog = false },
        containerColor = SteelDark,
        titleContentColor = TacticalGreenBright,
        textContentColor = TextPrimary,
        title = { Text("Iron Warfare — Credits", fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("2D Real-Time Strategy Engine for Android", fontWeight = FontWeight.SemiBold)
            Text(
              "Procedural terrain & sprite rendering, A* tactical pathfinding, multi-faction AI, and synthesized PCM game audio.",
              color = TextSecondary,
              fontSize = 13.sp,
            )
            Text("Version 1.0 — Built with Kotlin & Jetpack Compose", color = TextSecondary, fontSize = 12.sp)
          }
        },
        confirmButton = {
          TacticalButton(
            text = "Close",
            onClick = { showCreditsDialog = false },
            isPrimary = true,
          )
        },
      )
    }
  }
}
