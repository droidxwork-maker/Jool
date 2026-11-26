package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SavedGameSummary
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SteelDark
import com.example.ui.theme.SteelLight
import com.example.ui.theme.SteelMedium
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedGamesScreen(
  savedGames: List<SavedGameSummary>,
  onLoadGame: (String) -> Unit,
  onDeleteGame: (String) -> Unit,
  onBack: () -> Unit,
) {
  var selectedSaveId by remember(savedGames) { mutableStateOf(savedGames.firstOrNull()?.saveId) }
  var pendingDeleteId by remember { mutableStateOf<String?>(null) }
  val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

  BackHandler { onBack() }

  RivetedMetalBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(16.dp)
    ) {
      GreenSectionHeader(title = "SAVED GAMES")

      MetalPanel(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (savedGames.isEmpty()) {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = "No saved games",
              tint = TextSecondary,
              modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "No saved battles found.",
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
            )
            Text(
              text = "Start a Skirmish match and save manually or enable Autosave.",
              color = TextSecondary,
              fontSize = 13.sp,
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(savedGames, key = { it.saveId }) { item ->
              val isSelected = item.saveId == selectedSaveId
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .background(
                      if (isSelected) SteelLight else SteelMedium,
                      RoundedCornerShape(4.dp),
                    )
                    .border(
                      width = if (isSelected) 2.dp else 1.dp,
                      color = if (isSelected) TacticalGreenBright else SteelBorder,
                      shape = RoundedCornerShape(4.dp),
                    )
                    .clickable { selectedSaveId = item.saveId }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("save_item_${item.saveId}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = item.saveName,
                      color = TextPrimary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                    )
                    if (item.isAutosave) {
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = "AUTOSAVE",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                          Modifier
                            .background(TacticalAmber, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(3.dp))
                  Text(
                    text = "Map: ${item.mapId.displayName}  •  Saved: ${dateFormatter.format(Date(item.savedAtMillis))}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                  )
                }

                Text(
                  text = "Time ${formatDuration(item.elapsedSeconds)}",
                  color = TacticalGreenBright,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TacticalButton(
          text = "Back",
          onClick = onBack,
          testTag = "saved_games_back_button",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          TacticalButton(
            text = "Delete",
            onClick = { pendingDeleteId = selectedSaveId },
            isDanger = true,
            enabled = selectedSaveId != null,
            testTag = "saved_games_delete_button",
          )
          TacticalButton(
            text = "Load Game",
            onClick = { selectedSaveId?.let(onLoadGame) },
            isPrimary = true,
            enabled = selectedSaveId != null,
            testTag = "saved_games_load_button",
          )
        }
      }
    }

    pendingDeleteId?.let { delId ->
      AlertDialog(
        onDismissRequest = { pendingDeleteId = null },
        containerColor = SteelDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Delete Save?", fontWeight = FontWeight.Bold) },
        text = { Text("This saved game file will be permanently removed.") },
        confirmButton = {
          TacticalButton(
            text = "Delete",
            onClick = {
              onDeleteGame(delId)
              pendingDeleteId = null
            },
            isDanger = true,
          )
        },
        dismissButton = {
          TacticalButton(
            text = "Cancel",
            onClick = { pendingDeleteId = null },
          )
        },
      )
    }
  }
}
