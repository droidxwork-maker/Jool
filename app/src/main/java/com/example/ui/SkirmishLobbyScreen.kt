package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameMap
import com.example.model.AiDifficulty
import com.example.model.FactionColor
import com.example.model.GameOptions
import com.example.model.LobbyGameType
import com.example.model.LobbyPlayer
import com.example.model.MapId
import com.example.model.SavedGameSummary
import com.example.model.TeamId
import com.example.render.MapTerrainCache
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SteelDark
import com.example.ui.theme.SteelLight
import com.example.ui.theme.SteelMedium
import com.example.ui.theme.TacticalGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SkirmishLobbyScreen(
  gameType: LobbyGameType,
  onGameTypeChange: (LobbyGameType) -> Unit,
  selectedMapId: MapId,
  onMapSelected: (MapId) -> Unit,
  savedGames: List<SavedGameSummary>,
  selectedSaveId: String?,
  onSelectSaveId: (String) -> Unit,
  mapPreview: Pair<GameMap, MapTerrainCache>,
  gameOptions: GameOptions,
  onUpdateGameOptions: (GameOptions) -> Unit,
  players: List<LobbyPlayer>,
  onAddAi: () -> Unit,
  onUpdatePlayer: (LobbyPlayer) -> Unit,
  onKickPlayer: (Int) -> Unit,
  onStartGame: () -> Unit,
  onBack: () -> Unit,
) {
  var showGameOptions by remember { mutableStateOf(false) }
  var editingPlayer by remember { mutableStateOf<LobbyPlayer?>(null) }
  var gameTypeMenuOpen by remember { mutableStateOf(false) }
  var mapMenuOpen by remember { mutableStateOf(false) }
  var saveMenuOpen by remember { mutableStateOf(false) }

  BackHandler { onBack() }

  RivetedMetalBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // LEFT COLUMN: Game Configuration & Minimap Preview
        MetalPanel(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
          GreenSectionHeader(title = "Game", modifier = Modifier.padding(top = 0.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(modifier = Modifier.weight(1f)) {
              TacticalButton(
                text = gameType.label,
                onClick = { gameTypeMenuOpen = true },
                testTag = "lobby_game_type_button",
                modifier = Modifier.fillMaxWidth(),
              )
              DropdownMenu(
                expanded = gameTypeMenuOpen,
                onDismissRequest = { gameTypeMenuOpen = false },
                containerColor = SteelMedium,
              ) {
                LobbyGameType.entries.forEach { t ->
                  DropdownMenuItem(
                    text = { Text(t.label, color = TextPrimary) },
                    onClick = {
                      onGameTypeChange(t)
                      gameTypeMenuOpen = false
                    },
                  )
                }
              }
            }

            if (gameType == LobbyGameType.SKIRMISH_MAP) {
              Box(modifier = Modifier.weight(1f)) {
                TacticalButton(
                  text = selectedMapId.displayName,
                  onClick = { mapMenuOpen = true },
                  testTag = "lobby_map_select_button",
                  modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                  expanded = mapMenuOpen,
                  onDismissRequest = { mapMenuOpen = false },
                  containerColor = SteelMedium,
                ) {
                  MapId.entries.forEach { m ->
                    DropdownMenuItem(
                      text = { Text(m.displayName, color = TextPrimary) },
                      onClick = {
                        onMapSelected(m)
                        mapMenuOpen = false
                      },
                    )
                  }
                }
              }
            } else {
              val selectedSave = savedGames.firstOrNull { it.saveId == selectedSaveId }
              Box(modifier = Modifier.weight(1f)) {
                TacticalButton(
                  text = selectedSave?.saveName ?: "No Saves",
                  onClick = { if (savedGames.isNotEmpty()) saveMenuOpen = true },
                  enabled = savedGames.isNotEmpty(),
                  testTag = "lobby_save_select_button",
                  modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                  expanded = saveMenuOpen,
                  onDismissRequest = { saveMenuOpen = false },
                  containerColor = SteelMedium,
                ) {
                  savedGames.forEach { s ->
                    DropdownMenuItem(
                      text = { Text("${s.saveName} (${s.mapId.displayName})", color = TextPrimary) },
                      onClick = {
                        onSelectSaveId(s.saveId)
                        saveMenuOpen = false
                      },
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = selectedMapId.subtitle,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(6.dp))

          // Minimap Preview
          Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
          ) {
            MinimapCanvas(
              terrainBitmap = mapPreview.second.minimapBaseBitmap,
              mapWidthWorld = mapPreview.first.worldWidth,
              mapHeightWorld = mapPreview.first.worldHeight,
              lobbyPlayers = players,
              spawnPoints = mapPreview.first.spawnPoints,
              modifier = Modifier.fillMaxHeight().aspectRatio(1f),
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            TacticalButton(
              text = "Back",
              onClick = onBack,
              testTag = "lobby_back_button",
            )
            TacticalButton(
              text = "Game Options",
              onClick = { showGameOptions = true },
              testTag = "lobby_game_options_button",
              modifier = Modifier.weight(1f),
            )
            TacticalButton(
              text = "Start Game",
              onClick = onStartGame,
              isPrimary = true,
              enabled = gameType == LobbyGameType.SKIRMISH_MAP || (selectedSaveId != null),
              testTag = "lobby_start_game_button",
              modifier = Modifier.weight(1f),
            )
          }
        }

        // RIGHT COLUMN: Player List Table
        MetalPanel(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Player List",
              color = TacticalGreenBright,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
            )
            TacticalButton(
              text = "Add AI",
              onClick = onAddAi,
              enabled = players.size < 4,
              testTag = "lobby_add_ai_button",
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Table Header
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(SteelLight, RoundedCornerShape(3.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Name", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
            Text("Spawn Point", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
            Text("Team", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Color", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
          }

          Spacer(modifier = Modifier.height(6.dp))

          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(players, key = { it.id }) { player ->
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(SteelMedium, RoundedCornerShape(4.dp))
                    .border(1.dp, SteelBorder, RoundedCornerShape(4.dp))
                    .clickable { editingPlayer = player }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("player_row_${player.id}"),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(2.2f)) {
                  Text(
                    text = player.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = if (player.isHuman) "Human Player" else "AI (${player.aiDifficulty.label})",
                    color = TextSecondary,
                    fontSize = 10.sp,
                  )
                }
                Text(
                  text = "Spawn ${player.spawnPoint}",
                  color = TextPrimary,
                  fontSize = 13.sp,
                  modifier = Modifier.weight(1.2f),
                )
                Text(
                  text = player.team.name,
                  color = TacticalGreenBright,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  modifier = Modifier.weight(1f),
                )
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier =
                      Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(player.color.primaryArgb))
                        .border(1.dp, Color.White, CircleShape)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = player.color.displayName, color = TextPrimary, fontSize = 12.sp)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Tap any player row to change Spawn Point, Team, Color, or Kick AI.",
            color = TextSecondary,
            fontSize = 11.sp,
          )
        }
      }
    }

    if (showGameOptions) {
      GameOptionsDialog(
        initialOptions = gameOptions,
        onApply = {
          onUpdateGameOptions(it)
          showGameOptions = false
        },
        onCancel = { showGameOptions = false },
      )
    }

    editingPlayer?.let { target ->
      PlayerEditDialog(
        player = target,
        onSave = {
          onUpdatePlayer(it)
          editingPlayer = null
        },
        onKick = {
          onKickPlayer(target.id)
          editingPlayer = null
        },
        onDismiss = { editingPlayer = null },
      )
    }
  }
}

@Composable
private fun PlayerEditDialog(
  player: LobbyPlayer,
  onSave: (LobbyPlayer) -> Unit,
  onKick: () -> Unit,
  onDismiss: () -> Unit,
) {
  var draft by remember(player) { mutableStateOf(player) }
  var spawnMenu by remember { mutableStateOf(false) }
  var teamMenu by remember { mutableStateOf(false) }
  var colorMenu by remember { mutableStateOf(false) }
  var diffMenu by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SteelDark,
    titleContentColor = TacticalGreenBright,
    title = { Text("Edit Player — ${player.name}", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Spawn Point", color = TextPrimary, fontSize = 13.sp)
          Box {
            TacticalButton(text = "Spawn ${draft.spawnPoint}", onClick = { spawnMenu = true })
            DropdownMenu(expanded = spawnMenu, onDismissRequest = { spawnMenu = false }, containerColor = SteelMedium) {
              (1..4).forEach { sp ->
                DropdownMenuItem(
                  text = { Text("Spawn $sp", color = TextPrimary) },
                  onClick = {
                    draft = draft.copy(spawnPoint = sp)
                    spawnMenu = false
                  },
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Team", color = TextPrimary, fontSize = 13.sp)
          Box {
            TacticalButton(text = draft.team.label, onClick = { teamMenu = true })
            DropdownMenu(expanded = teamMenu, onDismissRequest = { teamMenu = false }, containerColor = SteelMedium) {
              TeamId.entries.forEach { t ->
                DropdownMenuItem(
                  text = { Text(t.label, color = TextPrimary) },
                  onClick = {
                    draft = draft.copy(team = t)
                    teamMenu = false
                  },
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Color", color = TextPrimary, fontSize = 13.sp)
          Box {
            TacticalButton(text = draft.color.displayName, onClick = { colorMenu = true })
            DropdownMenu(expanded = colorMenu, onDismissRequest = { colorMenu = false }, containerColor = SteelMedium) {
              FactionColor.entries.forEach { c ->
                DropdownMenuItem(
                  text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(modifier = Modifier.size(12.dp).background(Color(c.primaryArgb), CircleShape))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(c.displayName, color = TextPrimary)
                    }
                  },
                  onClick = {
                    draft = draft.copy(color = c)
                    colorMenu = false
                  },
                )
              }
            }
          }
        }

        if (!draft.isHuman) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("AI Difficulty", color = TextPrimary, fontSize = 13.sp)
            Box {
              TacticalButton(text = draft.aiDifficulty.label, onClick = { diffMenu = true })
              DropdownMenu(expanded = diffMenu, onDismissRequest = { diffMenu = false }, containerColor = SteelMedium) {
                AiDifficulty.entries.forEach { d ->
                  DropdownMenuItem(
                    text = { Text(d.label, color = TextPrimary) },
                    onClick = {
                      draft = draft.copy(aiDifficulty = d)
                      diffMenu = false
                    },
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!player.isHuman) {
          TacticalButton(
            text = "Kick",
            onClick = onKick,
            isDanger = true,
            testTag = "player_edit_kick_button",
          )
        }
        TacticalButton(
          text = "Apply",
          onClick = { onSave(draft) },
          isPrimary = true,
          testTag = "player_edit_apply_button",
        )
      }
    },
    dismissButton = {
      TacticalButton(text = "Cancel", onClick = onDismiss)
    },
  )
}
