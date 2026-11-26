package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SaveGameRepository
import com.example.data.SettingsRepository
import com.example.engine.GameAudioEngine
import com.example.engine.GameMap
import com.example.engine.GameSimulation
import com.example.engine.MapGenerator
import com.example.engine.SoundEffect
import com.example.model.AiDifficulty
import com.example.model.FactionColor
import com.example.model.GameOptions
import com.example.model.GameSettings
import com.example.model.LobbyGameType
import com.example.model.LobbyPlayer
import com.example.model.MapId
import com.example.model.MatchResult
import com.example.model.SavedGameSummary
import com.example.model.TeamId
import com.example.model.UnitType
import com.example.model.Vec2Data
import com.example.render.FogOverlayCache
import com.example.render.MapTerrainCache
import java.util.UUID
import kotlin.math.hypot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
  MAIN_MENU,
  SKIRMISH_LOBBY,
  SAVED_GAMES,
  SETTINGS,
  IN_GAME,
}

data class HudSnapshot(
  val credits: Int = 4000,
  val incomeRate: Int = 10,
  val unitCount: Int = 0,
  val unitCap: Int = 500,
  val elapsedSeconds: Float = 0f,
  val fps: Int = 60,
  val selectedCount: Int = 0,
  val primarySelectedType: UnitType? = null,
  val primarySelectedHp: Float = 0f,
  val primarySelectedMaxHp: Float = 0f,
  val primarySelectedQueueSize: Int = 0,
  val primarySelectedBuildProgress: Float = 1f,
  val primaryIsUnderConstruction: Boolean = false,
  val hasBuilderSelected: Boolean = false,
  val hasCombatSelected: Boolean = false,
  val groupCounts: Map<Int, Int> = emptyMap(),
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
  private val settingsRepo = SettingsRepository(application)
  private val saveRepo = SaveGameRepository(application)
  val audioEngine = GameAudioEngine(application.cacheDir)

  private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  private val _settings = MutableStateFlow(GameSettings())
  val settings: StateFlow<GameSettings> = _settings.asStateFlow()

  private val _savedGames = MutableStateFlow<List<SavedGameSummary>>(emptyList())
  val savedGames: StateFlow<List<SavedGameSummary>> = _savedGames.asStateFlow()

  // Lobby state
  var lobbyGameType by mutableStateOf(LobbyGameType.SKIRMISH_MAP)
  var selectedMapId by mutableStateOf(MapId.BIG_ISLAND)
  var gameOptions by mutableStateOf(GameOptions())
  var lobbyPlayers by mutableStateOf(defaultLobbyPlayers())
  var selectedSaveForLobby by mutableStateOf<String?>(null)

  private val mapCacheByMapId = HashMap<MapId, Pair<GameMap, MapTerrainCache>>()

  // Active game state
  var activeSimulation by mutableStateOf<GameSimulation?>(null)
    private set
  var activeTerrainCache by mutableStateOf<MapTerrainCache?>(null)
    private set
  var activeFogCache by mutableStateOf<FogOverlayCache?>(null)
    private set

  // Camera state (world center coordinates + zoom)
  var cameraX by mutableFloatStateOf(1536f)
  var cameraY by mutableFloatStateOf(1536f)
  var cameraZoom by mutableFloatStateOf(1.0f)

  // Selection & Command state
  var selectedEntityIds by mutableStateOf<Set<Int>>(emptySet())
  var pendingBuildType by mutableStateOf<UnitType?>(null)
  var boxSelectMode by mutableStateOf(false)
  var isPaused by mutableStateOf(false)
  var matchResult by mutableStateOf<MatchResult?>(null)
  var renderTick by mutableIntStateOf(0)
  var hudSnapshot by mutableStateOf(HudSnapshot())

  private var simLoopJob: Job? = null
  private var returnScreenFromSettings = AppScreen.MAIN_MENU

  init {
    viewModelScope.launch {
      settingsRepo.settingsFlow.collect { loaded ->
        _settings.value = loaded
        audioEngine.updateSettings(loaded)
        activeSimulation?.settings = loaded
      }
    }
    refreshSavedGames()
  }

  fun playClick() = audioEngine.play(SoundEffect.UI_CLICK)

  fun playConfirm() = audioEngine.play(SoundEffect.UI_CONFIRM)

  fun getLobbyMapPreview(mapId: MapId): Pair<GameMap, MapTerrainCache> {
    return mapCacheByMapId.getOrPut(mapId) {
      val map = MapGenerator.generate(mapId)
      Pair(map, MapTerrainCache(map))
    }
  }

  fun navigateTo(screen: AppScreen) {
    playClick()
    if (screen == AppScreen.SETTINGS) {
      returnScreenFromSettings = _currentScreen.value
    }
    if (screen == AppScreen.SAVED_GAMES || screen == AppScreen.SKIRMISH_LOBBY) {
      refreshSavedGames()
    }
    _currentScreen.value = screen
  }

  fun returnFromSettings() {
    playClick()
    _currentScreen.value = returnScreenFromSettings
  }

  fun saveSettings(newSettings: GameSettings) {
    playConfirm()
    _settings.value = newSettings
    audioEngine.updateSettings(newSettings)
    activeSimulation?.settings = newSettings
    viewModelScope.launch { settingsRepo.saveSettings(newSettings) }
  }

  fun refreshSavedGames() {
    viewModelScope.launch {
      val list = saveRepo.listSaves()
      _savedGames.value = list
      if (selectedSaveForLobby == null && list.isNotEmpty()) {
        selectedSaveForLobby = list.first().saveId
      }
    }
  }

  fun addAiPlayer() {
    if (lobbyPlayers.size >= 4) return
    playClick()
    val usedSpawns = lobbyPlayers.map { it.spawnPoint }.toSet()
    val nextSpawn = (1..4).firstOrNull { it !in usedSpawns } ?: (lobbyPlayers.size + 1)
    val usedColors = lobbyPlayers.map { it.color }.toSet()
    val nextColor = FactionColor.entries.firstOrNull { it !in usedColors } ?: FactionColor.entries[lobbyPlayers.size % 4]
    val nextTeam = TeamId.entries[lobbyPlayers.size % TeamId.entries.size]
    val nextId = (lobbyPlayers.maxOfOrNull { it.id } ?: 0) + 1
    lobbyPlayers =
      lobbyPlayers +
        LobbyPlayer(
          id = nextId,
          name = "AI Commander ${lobbyPlayers.size}",
          isHuman = false,
          spawnPoint = nextSpawn,
          team = nextTeam,
          color = nextColor,
          aiDifficulty = gameOptions.aiDifficulty,
        )
  }

  fun updateLobbyPlayer(updated: LobbyPlayer) {
    playClick()
    lobbyPlayers = lobbyPlayers.map { if (it.id == updated.id) updated else it }
  }

  fun kickLobbyPlayer(playerId: Int) {
    playClick()
    lobbyPlayers = lobbyPlayers.filter { it.id != playerId || it.isHuman }
  }

  fun startSkirmishMatch() {
    playConfirm()
    if (lobbyGameType == LobbyGameType.SAVED_GAME) {
      val targetId = selectedSaveForLobby ?: _savedGames.value.firstOrNull()?.saveId
      if (targetId != null) {
        loadSavedMatch(targetId)
        return
      }
    }
    val sim =
      GameSimulation.createNew(
        mapId = selectedMapId,
        lobbyPlayers = lobbyPlayers,
        options = gameOptions,
        settings = _settings.value,
        audioEngine = audioEngine,
      )
    val (_, cache) = getLobbyMapPreview(selectedMapId)
    val humanSpawnIdx = (lobbyPlayers.first { it.isHuman }.spawnPoint - 1).coerceIn(0, sim.map.spawnPoints.lastIndex)
    val startPos = sim.map.spawnPoints[humanSpawnIdx]
    launchSimulation(sim, cache, startPos.x, startPos.y, 1.0f)
  }

  fun loadSavedMatch(saveId: String) {
    playConfirm()
    viewModelScope.launch {
      val data = saveRepo.loadGame(saveId) ?: return@launch
      val sim = GameSimulation.fromSaveData(data, _settings.value, audioEngine)
      val cache = MapTerrainCache(sim.map)
      launchSimulation(sim, cache, data.cameraX, data.cameraY, data.cameraZoom)
    }
  }

  fun deleteSavedMatch(saveId: String) {
    playClick()
    viewModelScope.launch {
      saveRepo.deleteSave(saveId)
      refreshSavedGames()
    }
  }

  fun saveCurrentMatchManual(saveName: String, onComplete: (Boolean) -> Unit = {}) {
    val sim = activeSimulation ?: return
    val cleanName = saveName.trim().ifEmpty { "${sim.map.mapId.displayName} Save" }
    val saveId = "save_${UUID.randomUUID().toString().take(8)}"
    val data = sim.toSaveData(saveId, cleanName, isAutosave = false, cameraX, cameraY, cameraZoom)
    viewModelScope.launch {
      val ok = saveRepo.saveGame(data)
      refreshSavedGames()
      onComplete(ok)
    }
  }

  private fun triggerAutosave() {
    val sim = activeSimulation ?: return
    val data =
      sim.toSaveData(
        saveId = "autosave",
        saveName = "Autosave - ${sim.map.mapId.displayName}",
        isAutosave = true,
        cameraX = cameraX,
        cameraY = cameraY,
        cameraZoom = cameraZoom,
      )
    viewModelScope.launch { saveRepo.saveGame(data) }
  }

  private fun launchSimulation(
    sim: GameSimulation,
    cache: MapTerrainCache,
    camX: Float,
    camY: Float,
    zoom: Float,
  ) {
    simLoopJob?.cancel()
    activeSimulation = sim
    activeTerrainCache = cache
    activeFogCache = FogOverlayCache(sim.map.widthTiles, sim.map.heightTiles)
    cameraX = camX
    cameraY = camY
    cameraZoom = zoom
    selectedEntityIds = emptySet()
    pendingBuildType = null
    boxSelectMode = false
    isPaused = false
    matchResult = null
    updateHudSnapshot(sim, 60)
    _currentScreen.value = AppScreen.IN_GAME

    simLoopJob =
      viewModelScope.launch {
        var lastNs = System.nanoTime()
        var fpsFrames = 0
        var fpsTimer = 0f
        var hudTimer = 0f
        var measuredFps = 60

        while (isActive) {
          val targetFps = _settings.value.targetFps
          val frameMs = (1000L / targetFps).coerceAtLeast(16L)
          val nowNs = System.nanoTime()
          val dt = ((nowNs - lastNs) / 1_000_000_000.0).toFloat().coerceIn(0.005f, 0.08f)
          lastNs = nowNs

          if (!isPaused && sim.matchResult == null) {
            sim.step(dt) { triggerAutosave() }
            if (sim.matchResult != null) {
              matchResult = sim.matchResult
            }
          }

          fpsFrames++
          fpsTimer += dt
          if (fpsTimer >= 0.5f) {
            measuredFps = (fpsFrames / fpsTimer).toInt().coerceIn(1, 144)
            fpsFrames = 0
            fpsTimer = 0f
          }

          val aliveIds = sim.entities.mapTo(HashSet()) { it.id }
          if (selectedEntityIds.any { it !in aliveIds }) {
            selectedEntityIds = selectedEntityIds.filterTo(HashSet()) { it in aliveIds }
          }

          hudTimer += dt
          if (hudTimer >= 0.12f) {
            hudTimer = 0f
            updateHudSnapshot(sim, measuredFps)
          }
          renderTick++
          delay(frameMs)
        }
      }
  }

  private fun refreshHudNow() {
    val sim = activeSimulation ?: return
    updateHudSnapshot(sim, hudSnapshot.fps)
  }

  private fun updateHudSnapshot(sim: GameSimulation, fps: Int) {
    val human = sim.playersById[sim.humanPlayerId]
    val myUnitCount = sim.entities.count { it.ownerId == sim.humanPlayerId && !it.type.isBuilding }
    val selectedEntities = sim.entities.filter { it.id in selectedEntityIds }
    val primary = selectedEntities.firstOrNull()
    val groupCounts = sim.controlGroups.mapValues { (_, ids) -> ids.size }
    hudSnapshot =
      HudSnapshot(
        credits = human?.credits?.toInt() ?: 0,
        incomeRate = human?.incomePerSec ?: 0,
        unitCount = myUnitCount,
        unitCap = _settings.value.unitCap,
        elapsedSeconds = sim.elapsedSeconds,
        fps = fps,
        selectedCount = selectedEntities.size,
        primarySelectedType = primary?.type,
        primarySelectedHp = primary?.hp ?: 0f,
        primarySelectedMaxHp = primary?.maxHp ?: 0f,
        primarySelectedQueueSize = primary?.productionQueue?.size ?: 0,
        primarySelectedBuildProgress = primary?.buildProgress ?: 1f,
        primaryIsUnderConstruction = primary?.isUnderConstruction ?: false,
        hasBuilderSelected = selectedEntities.any { it.type == UnitType.BUILDER && it.ownerId == sim.humanPlayerId },
        hasCombatSelected =
          selectedEntities.any {
            it.ownerId == sim.humanPlayerId && !it.type.isBuilding && it.type != UnitType.BUILDER
          },
        groupCounts = groupCounts,
      )
  }

  // --- In-Game Actions ---

  fun unselectAll() {
    playClick()
    selectedEntityIds = emptySet()
    pendingBuildType = null
    refreshHudNow()
  }

  fun onDoubleTapMapWorld(worldX: Float, worldY: Float) {
    val sim = activeSimulation ?: return
    val tapped =
      sim.entities
        .filter { e ->
          e.ownerId == sim.humanPlayerId &&
            !e.type.isBuilding &&
            hypot(e.x - worldX, e.y - worldY) <= (e.type.radius + 18f)
        }
        .minByOrNull { hypot(it.x - worldX, it.y - worldY) }
        ?: return

    playConfirm()
    selectedEntityIds =
      sim.entities
        .filter { it.ownerId == sim.humanPlayerId && it.type == tapped.type }
        .mapTo(HashSet()) { it.id }
    pendingBuildType = null
    refreshHudNow()
  }

  fun onTapMapWorld(worldX: Float, worldY: Float) {
    val sim = activeSimulation ?: return
    val humanTeam = sim.playersById[sim.humanPlayerId]?.team ?: return

    // 1. Building placement mode
    val buildType = pendingBuildType
    if (buildType != null) {
      val builderIds =
        selectedEntityIds.filter { id ->
          sim.entities.any { it.id == id && it.ownerId == sim.humanPlayerId && it.type == UnitType.BUILDER }
        }
      val placed = sim.placeBuildingForPlayer(sim.humanPlayerId, buildType, worldX, worldY, builderIds)
      if (placed != null) {
        pendingBuildType = null
        refreshHudNow()
      } else {
        audioEngine.play(SoundEffect.UI_CLICK)
      }
      return
    }

    // 2. Hit-test entity under tap
    val tappedEntity =
      sim.entities
        .filter { e ->
          val hitR = (e.type.radius + 16f).coerceAtLeast(26f)
          hypot(e.x - worldX, e.y - worldY) <= hitR
        }
        .minByOrNull { hypot(it.x - worldX, it.y - worldY) }

    if (tappedEntity != null) {
      val owner = sim.playersById[tappedEntity.ownerId]
      if (tappedEntity.ownerId == sim.humanPlayerId) {
        val selectedBuilders =
          selectedEntityIds.filter { id ->
            sim.entities.any { it.id == id && it.ownerId == sim.humanPlayerId && it.type == UnitType.BUILDER }
          }
        if (selectedBuilders.isNotEmpty() && tappedEntity.isUnderConstruction) {
          selectedBuilders.forEach { sim.assignBuilderToBuilding(it, tappedEntity.id) }
          playConfirm()
          return
        }
        playClick()
        selectedEntityIds = setOf(tappedEntity.id)
        refreshHudNow()
        return
      } else if (owner != null && owner.team != humanTeam && selectedEntityIds.isNotEmpty()) {
        val mySelected =
          selectedEntityIds.filter { id ->
            sim.entities.any { it.id == id && it.ownerId == sim.humanPlayerId }
          }
        if (mySelected.isNotEmpty()) {
          playConfirm()
          sim.issueAttackUnit(mySelected, tappedEntity.id)
          return
        }
      }
    }

    // 3. Move / Rally Point command
    val ownedSelected =
      selectedEntityIds.filter { id ->
        sim.entities.any { it.id == id && it.ownerId == sim.humanPlayerId }
      }
    if (ownedSelected.isNotEmpty()) {
      playClick()
      sim.issueMoveOrder(ownedSelected, Vec2Data(worldX, worldY))
    }
  }

  fun selectInWorldRect(minX: Float, minY: Float, maxX: Float, maxY: Float) {
    val sim = activeSimulation ?: return
    val unitsInBox =
      sim.entities
        .filter { e ->
          e.ownerId == sim.humanPlayerId &&
            !e.type.isBuilding &&
            e.x in minX..maxX &&
            e.y in minY..maxY
        }
        .map { it.id }
        .toSet()

    if (unitsInBox.isNotEmpty()) {
      playClick()
      selectedEntityIds = unitsInBox
      pendingBuildType = null
      refreshHudNow()
      return
    }

    val buildingsInBox =
      sim.entities
        .filter { e ->
          e.ownerId == sim.humanPlayerId &&
            e.type.isBuilding &&
            e.x in minX..maxX &&
            e.y in minY..maxY
        }
        .map { it.id }
        .toSet()
    if (buildingsInBox.isNotEmpty()) {
      playClick()
      selectedEntityIds = setOf(buildingsInBox.first())
      pendingBuildType = null
      refreshHudNow()
    }
  }

  fun queueProduction(unitType: UnitType) {
    val sim = activeSimulation ?: return
    val producer =
      sim.entities.firstOrNull {
        it.id in selectedEntityIds &&
          it.ownerId == sim.humanPlayerId &&
          !it.isUnderConstruction &&
          unitType in it.type.producibleUnits(sim.playersById[it.ownerId]?.techLevel ?: 1)
      } ?: return
    if (sim.queueUnitProduction(sim.humanPlayerId, producer.id, unitType)) {
      refreshHudNow()
    }
  }

  fun startBuildPlacement(buildingType: UnitType) {
    playClick()
    pendingBuildType = if (pendingBuildType == buildingType) null else buildingType
  }

  fun stopSelectedUnits() {
    val sim = activeSimulation ?: return
    playClick()
    sim.stopUnits(selectedEntityIds.toList())
  }

  fun selectAllCombatUnits() {
    val sim = activeSimulation ?: return
    playClick()
    selectedEntityIds =
      sim.entities
        .filter { it.ownerId == sim.humanPlayerId && !it.type.isBuilding && it.type != UnitType.BUILDER }
        .mapTo(HashSet()) { it.id }
    pendingBuildType = null
    refreshHudNow()
  }

  fun selectIdleBuilder() {
    val sim = activeSimulation ?: return
    val builders = sim.entities.filter { it.ownerId == sim.humanPlayerId && it.type == UnitType.BUILDER }
    val target =
      builders.firstOrNull { it.buildTaskBuildingId == null && it.waypoints.isEmpty() }
        ?: builders.firstOrNull()
        ?: return
    playClick()
    selectedEntityIds = setOf(target.id)
    cameraX = target.x
    cameraY = target.y
    pendingBuildType = null
    refreshHudNow()
  }

  fun assignControlGroup(groupNum: Int) {
    val sim = activeSimulation ?: return
    val owned =
      selectedEntityIds.filter { id ->
        sim.entities.any { it.id == id && it.ownerId == sim.humanPlayerId }
      }
    sim.controlGroups[groupNum] = owned
    playConfirm()
    refreshHudNow()
  }

  fun recallControlGroup(groupNum: Int, centerCamera: Boolean = false) {
    val sim = activeSimulation ?: return
    val ids = sim.controlGroups[groupNum].orEmpty().filter { id -> sim.entities.any { it.id == id } }
    if (ids.isEmpty()) return
    playClick()
    selectedEntityIds = ids.toSet()
    pendingBuildType = null
    if (centerCamera) {
      val ents = sim.entities.filter { it.id in ids }
      if (ents.isNotEmpty()) {
        cameraX = ents.map { it.x }.average().toFloat()
        cameraY = ents.map { it.y }.average().toFloat()
      }
    }
    refreshHudNow()
  }

  fun quitMatchTo(screen: AppScreen) {
    simLoopJob?.cancel()
    simLoopJob = null
    activeSimulation = null
    activeTerrainCache = null
    activeFogCache = null
    isPaused = false
    matchResult = null
    _currentScreen.value = screen
    if (screen == AppScreen.SAVED_GAMES || screen == AppScreen.SKIRMISH_LOBBY) {
      refreshSavedGames()
    }
  }

  override fun onCleared() {
    super.onCleared()
    simLoopJob?.cancel()
    audioEngine.release()
  }

  companion object {
    fun defaultLobbyPlayers(): List<LobbyPlayer> =
      listOf(
        LobbyPlayer(
          id = 1,
          name = "Commander (You)",
          isHuman = true,
          spawnPoint = 1,
          team = TeamId.A,
          color = FactionColor.BLUE,
        ),
        LobbyPlayer(
          id = 2,
          name = "AI Commander 1",
          isHuman = false,
          spawnPoint = 2,
          team = TeamId.B,
          color = FactionColor.RED,
          aiDifficulty = AiDifficulty.MEDIUM,
        ),
      )
  }
}
