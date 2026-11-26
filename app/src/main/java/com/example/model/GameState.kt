package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Vec2Data(val x: Float, val y: Float)

@JsonClass(generateAdapter = true)
data class ProductionOrder(
  val unitType: UnitType,
  val remainingSeconds: Float,
  val totalSeconds: Float,
)

enum class UnitStance(val label: String) {
  AGGRESSIVE("Aggressive"),
  HOLD_GROUND("Hold Ground"),
  RETREAT("Retreat"),
}

@JsonClass(generateAdapter = true)
data class EntityState(
  val id: Int,
  val ownerId: Int,
  val type: UnitType,
  val x: Float,
  val y: Float,
  val hp: Float,
  val maxHp: Float,
  val bodyAngle: Float = 0f,
  val turretAngle: Float = 0f,
  val reloadTimer: Float = 0f,
  val isUnderConstruction: Boolean = false,
  val buildProgress: Float = 1f,
  val targetUnitId: Int? = null,
  val explicitTargetId: Int? = null,
  val chaseRepathTimer: Float = 0f,
  val attackMoveTarget: Vec2Data? = null,
  val buildTaskBuildingId: Int? = null,
  val repairTargetId: Int? = null,
  val waypoints: List<Vec2Data> = emptyList(),
  val rallyPoint: Vec2Data? = null,
  val productionQueue: List<ProductionOrder> = emptyList(),
  val muzzleFlashTimer: Float = 0f,
  val stance: UnitStance = UnitStance.AGGRESSIVE,
  val shieldHp: Float = 0f,
  val maxShieldHp: Float = 0f,
  val isUpgraded: Boolean = false,
  val upgradeLevel: Int = 1,
  val isGateOpen: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ProjectileState(
  val id: Int,
  val ownerId: Int,
  val team: TeamId,
  val sourceType: UnitType,
  val x: Float,
  val y: Float,
  val prevX: Float,
  val prevY: Float,
  val targetX: Float,
  val targetY: Float,
  val targetUnitId: Int?,
  val speed: Float,
  val damage: Float,
  val splashRadius: Float,
  val targetDomain: TargetDomain,
  val colorArgb: Int,
  val isEmp: Boolean = false,
  val isNuke: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ParticleState(
  val x: Float,
  val y: Float,
  val vx: Float,
  val vy: Float,
  val life: Float,
  val maxLife: Float,
  val radius: Float,
  val colorArgb: Int,
  val isSmoke: Boolean = false,
  val isRing: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ScorchDecal(
  val x: Float,
  val y: Float,
  val radius: Float,
  val alpha: Float,
)

@JsonClass(generateAdapter = true)
data class CloudState(
  val x: Float,
  val y: Float,
  val radiusX: Float,
  val radiusY: Float,
  val speedX: Float,
  val speedY: Float,
  val alpha: Float,
)

@JsonClass(generateAdapter = true)
data class PlayerMatchState(
  val id: Int,
  val name: String,
  val isHuman: Boolean,
  val spawnPoint: Int,
  val team: TeamId,
  val color: FactionColor,
  val aiDifficulty: AiDifficulty,
  val credits: Float,
  val incomePerSec: Int,
  val isEliminated: Boolean = false,
  val unitsBuilt: Int = 0,
  val unitsLost: Int = 0,
  val enemiesDestroyed: Int = 0,
  val buildingsBuilt: Int = 0,
  val totalCreditsEarned: Int = 0,
  val aiThinkTimer: Float = 0.5f,
  val techLevel: Int = 1,
)

@JsonClass(generateAdapter = true)
data class GameSaveData(
  val saveId: String,
  val saveName: String,
  val isAutosave: Boolean,
  val savedAtMillis: Long,
  val mapId: MapId,
  val mapSeed: Long,
  val mapWidthTiles: Int,
  val mapHeightTiles: Int,
  val elapsedSeconds: Float,
  val nextEntityId: Int,
  val options: GameOptions,
  val players: List<PlayerMatchState>,
  val entities: List<EntityState>,
  val projectiles: List<ProjectileState>,
  val particles: List<ParticleState>,
  val clouds: List<CloudState>,
  val controlGroups: Map<Int, List<Int>>,
  val exploredTiles: List<Int>,
  val cameraX: Float,
  val cameraY: Float,
  val cameraZoom: Float,
)

@JsonClass(generateAdapter = true)
data class SavedGameSummary(
  val saveId: String,
  val saveName: String,
  val mapId: MapId,
  val isAutosave: Boolean,
  val savedAtMillis: Long,
  val elapsedSeconds: Float,
)

data class MatchResult(
  val isVictory: Boolean,
  val winningTeam: TeamId,
  val elapsedSeconds: Float,
  val playerStats: PlayerMatchState,
  val allPlayers: List<PlayerMatchState>,
)
