package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.example.model.AiDifficulty
import com.example.model.FogMode
import com.example.model.GameOptions
import com.example.model.StartingUnitsOption
import com.example.ui.theme.SteelDark
import com.example.ui.theme.SteelMedium
import com.example.ui.theme.TacticalGreenBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameOptionsDialog(
  initialOptions: GameOptions,
  onApply: (GameOptions) -> Unit,
  onCancel: () -> Unit,
) {
  var draft by remember(initialOptions) { mutableStateOf(initialOptions) }

  AlertDialog(
    onDismissRequest = onCancel,
    containerColor = SteelDark,
    titleContentColor = TacticalGreenBright,
    title = {
      Text(text = "Game Options", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OptionDropdownRow(
          label = "AI Difficulty",
          selectedText = draft.aiDifficulty.label,
          options = AiDifficulty.entries.map { it.label to it },
          onSelect = { draft = draft.copy(aiDifficulty = it) },
          testTag = "opt_ai_difficulty",
        )
        OptionDropdownRow(
          label = "Starting Units",
          selectedText = draft.startingUnits.label,
          options = StartingUnitsOption.entries.map { it.label to it },
          onSelect = { draft = draft.copy(startingUnits = it) },
          testTag = "opt_starting_units",
        )
        OptionDropdownRow(
          label = "Fog Mode",
          selectedText = draft.fogMode.label,
          options = FogMode.entries.map { it.label to it },
          onSelect = { draft = draft.copy(fogMode = it) },
          testTag = "opt_fog_mode",
        )
        OptionDropdownRow(
          label = "Starting Credits",
          selectedText = "$${draft.startingCredits}",
          options = listOf("$2000" to 2000, "$4000" to 4000, "$10000" to 10000),
          onSelect = { draft = draft.copy(startingCredits = it) },
          testTag = "opt_starting_credits",
        )
        OptionDropdownRow(
          label = "Income Multiplier",
          selectedText = "${draft.incomeMultiplier}x",
          options = listOf("1x" to 1, "2x" to 2, "3x" to 3),
          onSelect = { draft = draft.copy(incomeMultiplier = it) },
          testTag = "opt_income_mult",
        )
      }
    },
    confirmButton = {
      TacticalButton(
        text = "Apply",
        onClick = { onApply(draft) },
        isPrimary = true,
        testTag = "game_options_apply_button",
      )
    },
    dismissButton = {
      TacticalButton(
        text = "Cancel",
        onClick = onCancel,
        testTag = "game_options_cancel_button",
      )
    },
  )
}

@Composable
private fun <T> OptionDropdownRow(
  label: String,
  selectedText: String,
  options: List<Pair<String, T>>,
  onSelect: (T) -> Unit,
  testTag: String,
) {
  var expanded by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.width(8.dp))
    Box {
      TacticalButton(
        text = selectedText,
        onClick = { expanded = true },
        testTag = testTag,
      )
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        containerColor = SteelMedium,
      ) {
        options.forEach { (text, value) ->
          DropdownMenuItem(
            text = { Text(text, color = TextPrimary, fontSize = 13.sp) },
            onClick = {
              onSelect(value)
              expanded = false
            },
          )
        }
      }
    }
  }
}
