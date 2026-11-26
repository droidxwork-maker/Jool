package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.InGameScreen
import com.example.ui.MainMenuScreen
import com.example.ui.SavedGamesScreen
import com.example.ui.SettingsScreen
import com.example.ui.SkirmishLobbyScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())

    setContent {
      MyApplicationTheme {
        val vm: GameViewModel = viewModel()
        val currentScreen by vm.currentScreen.collectAsState()
        val settings by vm.settings.collectAsState()
        val savedGames by vm.savedGames.collectAsState()

        when (currentScreen) {
          AppScreen.MAIN_MENU -> {
            MainMenuScreen(
              onSkirmish = { vm.navigateTo(AppScreen.SKIRMISH_LOBBY) },
              onSavedGames = { vm.navigateTo(AppScreen.SAVED_GAMES) },
              onSettings = { vm.navigateTo(AppScreen.SETTINGS) },
              onExitConfirmed = { finish() },
            )
          }
          AppScreen.SKIRMISH_LOBBY -> {
            val preview = vm.getLobbyMapPreview(vm.selectedMapId)
            SkirmishLobbyScreen(
              gameType = vm.lobbyGameType,
              onGameTypeChange = {
                vm.playClick()
                vm.lobbyGameType = it
              },
              selectedMapId = vm.selectedMapId,
              onMapSelected = {
                vm.playClick()
                vm.selectedMapId = it
              },
              savedGames = savedGames,
              selectedSaveId = vm.selectedSaveForLobby,
              onSelectSaveId = {
                vm.playClick()
                vm.selectedSaveForLobby = it
              },
              mapPreview = preview,
              gameOptions = vm.gameOptions,
              onUpdateGameOptions = {
                vm.playConfirm()
                vm.gameOptions = it
              },
              players = vm.lobbyPlayers,
              onAddAi = { vm.addAiPlayer() },
              onUpdatePlayer = { vm.updateLobbyPlayer(it) },
              onKickPlayer = { vm.kickLobbyPlayer(it) },
              onStartGame = { vm.startSkirmishMatch() },
              onBack = { vm.navigateTo(AppScreen.MAIN_MENU) },
            )
          }
          AppScreen.SAVED_GAMES -> {
            SavedGamesScreen(
              savedGames = savedGames,
              onLoadGame = { vm.loadSavedMatch(it) },
              onDeleteGame = { vm.deleteSavedMatch(it) },
              onBack = { vm.navigateTo(AppScreen.MAIN_MENU) },
            )
          }
          AppScreen.SETTINGS -> {
            SettingsScreen(
              currentSettings = settings,
              onSaveAndDone = { updated ->
                vm.saveSettings(updated)
                vm.returnFromSettings()
              },
              onCancel = { vm.returnFromSettings() },
              onPlayClick = { vm.playClick() },
            )
          }
          AppScreen.IN_GAME -> {
            val sim = vm.activeSimulation
            val cache = vm.activeTerrainCache
            val fogCache = vm.activeFogCache
            if (sim != null && cache != null && fogCache != null) {
              InGameScreen(
                simulation = sim,
                terrainCache = cache,
                fogCache = fogCache,
                settings = settings,
                hud = vm.hudSnapshot,
                cameraX = vm.cameraX,
                cameraY = vm.cameraY,
                cameraZoom = vm.cameraZoom,
                onCameraChange = { cx, cy, z ->
                  vm.cameraX = cx
                  vm.cameraY = cy
                  vm.cameraZoom = z
                },
                selectedEntityIds = vm.selectedEntityIds,
                pendingBuildType = vm.pendingBuildType,
                boxSelectMode = vm.boxSelectMode,
                onToggleBoxSelectMode = {
                  vm.playClick()
                  vm.boxSelectMode = !vm.boxSelectMode
                },
                isPaused = vm.isPaused,
                onSetPaused = {
                  vm.playClick()
                  vm.isPaused = it
                },
                matchResult = vm.matchResult,
                renderTick = vm.renderTick,
                onTapMapWorld = { wx, wy -> vm.onTapMapWorld(wx, wy) },
                onDoubleTapMapWorld = { wx, wy -> vm.onDoubleTapMapWorld(wx, wy) },
                onSelectWorldRect = { x1, y1, x2, y2 -> vm.selectInWorldRect(x1, y1, x2, y2) },
                onUnselectAll = { vm.unselectAll() },
                onBuildBuilding = { vm.startBuildPlacement(it) },
                onProduceUnit = { vm.queueProduction(it) },
                onStopUnits = { vm.stopSelectedUnits() },
                onSelectAllCombat = { vm.selectAllCombatUnits() },
                onSelectIdleBuilder = { vm.selectIdleBuilder() },
                onSetStance = { stance ->
                  val sim = vm.activeSimulation
                  if (sim != null) {
                    sim.setUnitStance(vm.selectedEntityIds.toList(), stance)
                  }
                },
                onAssignGroup = { vm.assignControlGroup(it) },
                onRecallGroup = { g, center -> vm.recallControlGroup(g, center) },
                onSaveGame = { name, cb -> vm.saveCurrentMatchManual(name, cb) },
                onOpenSettings = { vm.navigateTo(AppScreen.SETTINGS) },
                onBackToLobby = { vm.quitMatchTo(AppScreen.SKIRMISH_LOBBY) },
                onQuitToMenu = { vm.quitMatchTo(AppScreen.MAIN_MENU) },
              )
            }
          }
        }
      }
    }
  }
}
