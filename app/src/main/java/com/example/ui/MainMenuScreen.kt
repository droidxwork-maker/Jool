package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SteelDark
import com.example.ui.theme.SteelMedium
import com.example.ui.theme.TacticalGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MainMenuScreen(
  onSkirmish: () -> Unit,
  onSavedGames: () -> Unit,
  onSettings: () -> Unit,
  onExitConfirmed: () -> Unit,
) {
  var showExitDialog by remember { mutableStateOf(false) }

  BackHandler { showExitDialog = true }

  RivetedMetalBackground {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      // Top Logo Banner
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier =
          Modifier
            .background(SteelDark.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
            .border(2.dp, SteelBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.iron_warfare_icon_1795671104797),
          contentDescription = "Iron Warfare emblem",
          contentScale = ContentScale.Crop,
          modifier =
            Modifier
              .size(56.dp)
              .clip(RoundedCornerShape(6.dp))
              .border(1.5.dp, TacticalGreenBright.copy(alpha = 0.7f), RoundedCornerShape(6.dp)),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
          Text(
            text = "IRON WARFARE",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = "TACTICAL REAL-TIME STRATEGY",
            color = TacticalGreenBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      // Primary Menu Buttons Panel
      MetalPanel(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
        TacticalButton(
          text = "Skirmish",
          onClick = onSkirmish,
          isPrimary = true,
          testTag = "menu_skirmish_button",
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        TacticalButton(
          text = "Saved Games",
          onClick = onSavedGames,
          testTag = "menu_saved_games_button",
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        TacticalButton(
          text = "Settings",
          onClick = onSettings,
          testTag = "menu_settings_button",
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        TacticalButton(
          text = "Exit",
          onClick = { showExitDialog = true },
          isDanger = true,
          testTag = "menu_exit_button",
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    if (showExitDialog) {
      AlertDialog(
        onDismissRequest = { showExitDialog = false },
        containerColor = SteelDark,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
          Text(
            text = "Close?",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
          )
        },
        text = {
          Text(
            text = "What would you like to do?",
            fontSize = 14.sp,
          )
        },
        confirmButton = {
          TacticalButton(
            text = "Exit Game",
            onClick = onExitConfirmed,
            isDanger = true,
            testTag = "exit_confirm_button",
          )
        },
        dismissButton = {
          TacticalButton(
            text = "Stay",
            onClick = { showExitDialog = false },
            isPrimary = true,
            testTag = "exit_stay_button",
          )
        },
      )
    }
  }
}
