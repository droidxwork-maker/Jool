package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameSimulation
import com.example.model.FactionColor
import com.example.model.MatchResult
import com.example.model.UnitStance
import com.example.model.UnitType
import com.example.render.FogOverlayCache
import com.example.render.MapTerrainCache
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SteelDark
import com.example.ui.theme.SteelDarkest
import com.example.ui.theme.SteelHighlight
import com.example.ui.theme.SteelLight
import com.example.ui.theme.SteelMedium
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalGreenBright
import com.example.ui.theme.TacticalGreenDark
import com.example.ui.theme.TacticalRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.HudSnapshot

private data class CommandGridItem(
  val label: String,
  val cost: Int?,
  val iconType: UnitType?,
  val isSelected: Boolean = false,
  val enabled: Boolean = true,
  val testTag: String,
  val onClick: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InGameCommandSidebar(
  simulation: GameSimulation,
  terrainCache: MapTerrainCache,
  fogCache: FogOverlayCache,
  hud: HudSnapshot,
  cameraX: Float,
  cameraY: Float,
  viewportWorldW: Float,
  viewportWorldH: Float,
  pendingBuildType: UnitType?,
  renderTick: Int,
  onMinimapNavigate: (Float, Float) -> Unit,
  onUnselectAll: () -> Unit,
  onBuildBuilding: (UnitType) -> Unit,
  onProduceUnit: (UnitType) -> Unit,
  onStopUnits: () -> Unit,
  onSelectAllCombat: () -> Unit,
  onSelectIdleBuilder: () -> Unit,
  onSetStance: (UnitStance) -> Unit,
  onAssignGroup: (Int) -> Unit,
  onRecallGroup: (Int, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val humanColor = simulation.playersById[simulation.humanPlayerId]?.color ?: FactionColor.BLUE
  val primaryColor = Color(humanColor.primaryArgb)
  val darkColor = Color(humanColor.darkArgb)
  val lightColor = Color(humanColor.lightArgb)

  var assignGroupMode by remember { mutableStateOf(false) }
  var stanceMenuOpen by remember { mutableStateOf(false) }

  Column(
    modifier =
      modifier
        .fillMaxHeight()
        .background(SteelDark)
        .border(BorderStroke(1.5.dp, SteelBorder))
        .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // 1. Live Minimap
    MinimapCanvas(
      terrainBitmap = terrainCache.minimapBaseBitmap,
      mapWidthWorld = simulation.map.worldWidth,
      mapHeightWorld = simulation.map.worldHeight,
      simulation = simulation,
      fogCache = fogCache,
      cameraX = cameraX,
      cameraY = cameraY,
      viewportWorldW = viewportWorldW,
      viewportWorldH = viewportWorldH,
      renderTick = renderTick,
      onNavigateWorld = onMinimapNavigate,
      modifier = Modifier.fillMaxWidth().aspectRatio(1f).testTag("hud_minimap"),
    )

    // 2. Unselect All
    TacticalButton(
      text = "Unselect All",
      onClick = onUnselectAll,
      enabled = hud.selectedCount > 0 || pendingBuildType != null,
      testTag = "hud_unselect_all",
      modifier = Modifier.fillMaxWidth(),
    )

    // Selected Info Strip
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(SteelMedium, RoundedCornerShape(3.dp))
          .border(1.dp, SteelBorder, RoundedCornerShape(3.dp))
          .padding(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val pType = hud.primarySelectedType
      if (pType != null) {
        UnitTypeBadge(
          type = pType,
          factionColor = primaryColor,
          darkColor = darkColor,
          lightColor = lightColor,
          modifier = Modifier.size(34.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (hud.selectedCount > 1) "${pType.displayName} (+${hud.selectedCount - 1})" else pType.displayName,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(3.dp))
          val hpRatio = if (hud.primarySelectedMaxHp > 0f) (hud.primarySelectedHp / hud.primarySelectedMaxHp).coerceIn(0f, 1f) else 0f
          LinearProgressIndicator(
            progress = { hpRatio },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = if (hpRatio > 0.4f) TacticalGreenBright else TacticalRed,
            trackColor = SteelDarkest,
          )
          if (hud.primaryIsUnderConstruction) {
            Text(
              text = "Building ${(hud.primarySelectedBuildProgress * 100).toInt()}%",
              color = TacticalGreenBright,
              fontSize = 10.sp,
            )
          } else if (hud.primarySelectedQueueSize > 0) {
            Text(
              text = "Queue: ${hud.primarySelectedQueueSize}/5",
              color = TacticalAmber,
              fontSize = 10.sp,
            )
          }
        }
      } else {
        Text(
          text = pendingBuildType?.let { "Tap map to place ${it.shortName}" } ?: "No unit selected",
          color = if (pendingBuildType != null) TacticalGreenBright else TextSecondary,
          fontSize = 12.sp,
          modifier = Modifier.padding(vertical = 6.dp),
        )
      }
    }

    // 3. Command & Build Grid
    val humanPlayer = simulation.playersById[simulation.humanPlayerId]
    val currentTechLevel = humanPlayer?.techLevel ?: 1

    val commands = remember(hud, pendingBuildType, currentTechLevel) {
      buildList {
        if (hud.hasBuilderSelected) {
          listOf(
            UnitType.EXTRACTOR,
            UnitType.OIL_REFINERY,
            UnitType.DEFENSE_WALL,
            UnitType.STEEL_GATE,
            UnitType.TURRET,
            UnitType.ANTI_AIR,
            UnitType.LAND_FACTORY,
            UnitType.AIR_FACTORY,
            UnitType.TECH_CENTER,
            UnitType.MISSILE_SILO,
            UnitType.COMMAND_CENTER,
          ).forEach { bType ->
            add(
              CommandGridItem(
                label = bType.shortName,
                cost = bType.cost,
                iconType = bType,
                isSelected = pendingBuildType == bType,
                enabled = hud.credits >= bType.cost,
                testTag = "cmd_build_${bType.name.lowercase()}",
                onClick = { onBuildBuilding(bType) },
              )
            )
          }
        }

        val pType = hud.primarySelectedType
        if (pType != null && pType.canProduce && !hud.primaryIsUnderConstruction) {
          pType.producibleUnits(currentTechLevel).forEach { uType ->
            add(
              CommandGridItem(
                label = uType.shortName,
                cost = uType.cost,
                iconType = uType,
                enabled = hud.credits >= uType.cost && hud.unitCount < hud.unitCap,
                testTag = "cmd_produce_${uType.name.lowercase()}",
                onClick = { onProduceUnit(uType) },
              )
            )
          }
        }

        if (pType != null && !pType.isBuilding && hud.selectedCount > 0) {
          add(
            CommandGridItem(
              label = "Stop",
              cost = null,
              iconType = null,
              testTag = "cmd_stop",
              onClick = onStopUnits,
            )
          )
        }

        add(
          CommandGridItem(
            label = "Idle Builder",
            cost = null,
            iconType = UnitType.BUILDER,
            testTag = "cmd_idle_builder",
            onClick = onSelectIdleBuilder,
          )
        )

        add(
          CommandGridItem(
            label = "All Combat",
            cost = null,
            iconType = UnitType.LIGHT_TANK,
            testTag = "cmd_select_all_combat",
            onClick = onSelectAllCombat,
          )
        )
      }
    }

    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier = Modifier.weight(1f).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      contentPadding = PaddingValues(2.dp),
    ) {
      items(commands, key = { it.testTag }) { item ->
        val borderCol =
          when {
            item.isSelected -> TacticalGreenBright
            item.enabled -> SteelHighlight
            else -> SteelBorder.copy(alpha = 0.4f)
          }
        val bgCol =
          when {
            item.isSelected -> TacticalGreenDark
            item.enabled -> SteelLight
            else -> SteelMedium.copy(alpha = 0.7f)
          }

        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 54.dp)
              .background(bgCol, RoundedCornerShape(3.dp))
              .border(1.5.dp, borderCol, RoundedCornerShape(3.dp))
              .clickable(enabled = item.enabled) { item.onClick() }
              .padding(horizontal = 4.dp, vertical = 4.dp)
              .testTag(item.testTag),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          if (item.iconType != null) {
            UnitTypeBadge(
              type = item.iconType,
              factionColor = primaryColor,
              darkColor = darkColor,
              lightColor = lightColor,
              modifier = Modifier.size(26.dp),
            )
          }
          Text(
            text = item.label,
            color = if (item.enabled) TextPrimary else TextSecondary.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (item.cost != null) {
            Text(
              text = "$${item.cost}",
              color = if (item.enabled) TacticalGreenBright else TacticalRed,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // 4. Stance toggle + Control Groups (1, 2, 3)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(modifier = Modifier.weight(1.1f)) {
        TacticalButton(
          text = "Stance",
          onClick = { stanceMenuOpen = true },
          testTag = "cmd_stance_button",
          modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
          expanded = stanceMenuOpen,
          onDismissRequest = { stanceMenuOpen = false },
          containerColor = SteelMedium,
        ) {
          UnitStance.entries.forEach { st ->
            DropdownMenuItem(
              text = { Text(st.label, color = TextPrimary, fontSize = 13.sp) },
              onClick = {
                onSetStance(st)
                stanceMenuOpen = false
              },
            )
          }
        }
      }

      listOf(1, 2, 3).forEach { gNum ->
        val count = hud.groupCounts[gNum] ?: 0
        Box(
          modifier =
            Modifier
              .weight(0.9f)
              .heightIn(min = 48.dp)
              .background(
                if (count > 0) SteelLight else SteelMedium,
                RoundedCornerShape(3.dp),
              )
              .border(
                1.dp,
                if (count > 0) TacticalGreenBright else SteelBorder,
                RoundedCornerShape(3.dp),
              )
              .combinedClickable(
                onClick = { onRecallGroup(gNum, false) },
                onDoubleClick = { onRecallGroup(gNum, true) },
                onLongClick = { onAssignGroup(gNum) },
              )
              .testTag("control_group_$gNum"),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "($gNum)",
              color = TextPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
            )
            if (count > 0) {
              Text(
                text = "$count",
                color = TacticalGreenBright,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun InGamePauseDialog(
  defaultSaveName: String,
  onResume: () -> Unit,
  onSaveGame: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onQuitToMenu: () -> Unit,
) {
  var showSaveInput by remember { mutableStateOf(false) }
  var saveName by remember { mutableStateOf(defaultSaveName) }

  AlertDialog(
    onDismissRequest = onResume,
    containerColor = SteelDark,
    titleContentColor = TacticalGreenBright,
    title = {
      Text(
        text = if (showSaveInput) "Save Battle" else "Game Paused",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
      )
    },
    text = {
      if (showSaveInput) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Enter a name for this saved battle:", color = TextSecondary, fontSize = 13.sp)
          OutlinedTextField(
            value = saveName,
            onValueChange = { saveName = it },
            singleLine = true,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = TacticalGreenBright,
                unfocusedBorderColor = SteelBorder,
              ),
            modifier = Modifier.fillMaxWidth().testTag("save_name_input"),
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          TacticalButton(
            text = "Resume",
            onClick = onResume,
            isPrimary = true,
            testTag = "pause_resume_button",
            modifier = Modifier.fillMaxWidth(),
          )
          TacticalButton(
            text = "Save Game",
            onClick = { showSaveInput = true },
            testTag = "pause_save_button",
            modifier = Modifier.fillMaxWidth(),
          )
          TacticalButton(
            text = "Quick Settings",
            onClick = onOpenSettings,
            testTag = "pause_settings_button",
            modifier = Modifier.fillMaxWidth(),
          )
          TacticalButton(
            text = "Quit to Menu",
            onClick = onQuitToMenu,
            isDanger = true,
            testTag = "pause_quit_button",
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      if (showSaveInput) {
        TacticalButton(
          text = "Save",
          onClick = {
            onSaveGame(saveName)
            showSaveInput = false
          },
          isPrimary = true,
          testTag = "confirm_save_button",
        )
      }
    },
    dismissButton = {
      if (showSaveInput) {
        TacticalButton(
          text = "Back",
          onClick = { showSaveInput = false },
        )
      }
    },
  )
}

@Composable
fun MatchResultDialog(
  result: MatchResult,
  onBackToLobby: () -> Unit,
  onMainMenu: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = {},
    containerColor = SteelDark,
    titleContentColor = if (result.isVictory) TacticalGreenBright else TacticalRed,
    title = {
      Text(
        text = if (result.isVictory) "VICTORY!" else "DEFEAT",
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Winning Side: ${result.winningTeam.label}",
          color = TextPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
        )
        Text(
          text = "Battle Duration: ${formatDuration(result.elapsedSeconds)}",
          color = TextSecondary,
          fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        StatRow("Units Built", "${result.playerStats.unitsBuilt}")
        StatRow("Enemies Destroyed", "${result.playerStats.enemiesDestroyed}")
        StatRow("Buildings Constructed", "${result.playerStats.buildingsBuilt}")
        StatRow("Units Lost", "${result.playerStats.unitsLost}")
        StatRow("Total Credits Earned", "$${result.playerStats.totalCreditsEarned}")
      }
    },
    confirmButton = {
      TacticalButton(
        text = "Back to Lobby",
        onClick = onBackToLobby,
        isPrimary = true,
        testTag = "result_lobby_button",
      )
    },
    dismissButton = {
      TacticalButton(
        text = "Main Menu",
        onClick = onMainMenu,
        testTag = "result_menu_button",
      )
    },
  )
}

@Composable
private fun StatRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, color = TextSecondary, fontSize = 13.sp)
    Text(value, color = TacticalGreenBright, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
  }
}
