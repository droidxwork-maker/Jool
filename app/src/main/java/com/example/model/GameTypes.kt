package com.example.model

import com.squareup.moshi.JsonClass

enum class FactionColor(
  val displayName: String,
  val primaryArgb: Int,
  val darkArgb: Int,
  val lightArgb: Int,
) {
  BLUE("Blue", 0xFF2196F3.toInt(), 0xFF1565C0.toInt(), 0xFF64B5F6.toInt()),
  RED("Red", 0xFFE53935.toInt(), 0xFFB71C1C.toInt(), 0xFFFF8A80.toInt()),
  GREEN("Green", 0xFF43A047.toInt(), 0xFF1B5E20.toInt(), 0xFF81C784.toInt()),
  YELLOW("Yellow", 0xFFFBC02D.toInt(), 0xFFF57F17.toInt(), 0xFFFFF176.toInt()),
}

enum class TeamId(val label: String) {
  A("Team A"),
  B("Team B"),
  C("Team C"),
  D("Team D"),
}

enum class AiDifficulty(val label: String, val thinkIntervalSec: Float, val incomeBonus: Float) {
  EASY("Easy", 3.2f, 0.8f),
  MEDIUM("Medium", 1.6f, 1.0f),
  HARD("Hard", 0.75f, 1.35f),
}

enum class StartingUnitsOption(val label: String) {
  COMMAND_CENTER_ONLY("Command Center Only"),
  COMMAND_CENTER_PLUS_BUILDER("Command Center + 1 Builder"),
}

enum class FogMode(val label: String) {
  NONE("No Fog"),
  FOG_OF_WAR("Fog of War"),
}

enum class MapId(val displayName: String, val subtitle: String) {
  BIG_ISLAND("Big Island", "Lush central continent surrounded by deep seas"),
  FIRE_BRIDGE("Fire Bridge", "Volcanic divide connected by three tactical bridges"),
  HILLS("Hills", "Rocky plateaus with flanking passes and ridges"),
  LAKE("Lake", "Expansive central lake with shoreline battlegrounds"),
  ICE_ISLAND("Ice Island", "Frozen glacial archipelago with rich central wells"),
}

enum class LobbyGameType(val label: String) {
  SKIRMISH_MAP("Skirmish Map"),
  SAVED_GAME("Saved Game"),
}

@JsonClass(generateAdapter = true)
data class LobbyPlayer(
  val id: Int,
  val name: String,
  val isHuman: Boolean,
  val spawnPoint: Int,
  val team: TeamId,
  val color: FactionColor,
  val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
)

@JsonClass(generateAdapter = true)
data class GameOptions(
  val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
  val startingUnits: StartingUnitsOption = StartingUnitsOption.COMMAND_CENTER_PLUS_BUILDER,
  val fogMode: FogMode = FogMode.FOG_OF_WAR,
  val startingCredits: Int = 4000,
  val incomeMultiplier: Int = 1,
)

enum class TileType(val isWalkableGround: Boolean, val isBuildable: Boolean) {
  GRASS(true, true),
  SAND(true, true),
  ICE(true, true),
  WATER(false, false),
  ROCK(false, false),
  LAVA_ROCK(false, false),
  RESOURCE_POOL(true, false),
}

enum class UnitDomain {
  GROUND,
  AIR,
  BUILDING,
}

enum class TargetDomain {
  NONE,
  GROUND,
  AIR,
  BOTH,
}

enum class UnitType(
  val displayName: String,
  val shortName: String,
  val cost: Int,
  val maxHp: Float,
  val speed: Float,
  val radius: Float,
  val attackRange: Float,
  val attackDamage: Float,
  val reloadSeconds: Float,
  val splashRadius: Float,
  val visionRadius: Float,
  val buildTimeSeconds: Float,
  val incomePerSec: Int,
  val domain: UnitDomain,
  val targetDomain: TargetDomain,
  val canProduce: Boolean = false,
  val shieldMax: Float = 0f,
) {
  // Mobile Units
  BUILDER(
    displayName = "Builder",
    shortName = "Builder",
    cost = 500,
    maxHp = 220f,
    speed = 80f,
    radius = 14f,
    attackRange = 110f,
    attackDamage = 0f,
    reloadSeconds = 0.25f,
    splashRadius = 0f,
    visionRadius = 210f,
    buildTimeSeconds = 5.0f,
    incomePerSec = 0,
    domain = UnitDomain.GROUND,
    targetDomain = TargetDomain.NONE,
  ),
  LIGHT_TANK(
    displayName = "Light Tank",
    shortName = "L.Tank",
    cost = 350,
    maxHp = 260f,
    speed = 100f,
    radius = 14f,
    attackRange = 180f,
    attackDamage = 28f,
    reloadSeconds = 0.7f,
    splashRadius = 0f,
    visionRadius = 230f,
    buildTimeSeconds = 4.0f,
    incomePerSec = 0,
    domain = UnitDomain.GROUND,
    targetDomain = TargetDomain.GROUND,
  ),
  HEAVY_TANK(
    displayName = "Heavy Tank",
    shortName = "H.Tank",
    cost = 750,
    maxHp = 650f,
    speed = 64f,
    radius = 18f,
    attackRange = 220f,
    attackDamage = 68f,
    reloadSeconds = 1.3f,
    splashRadius = 0f,
    visionRadius = 250f,
    buildTimeSeconds = 7.5f,
    incomePerSec = 0,
    domain = UnitDomain.GROUND,
    targetDomain = TargetDomain.GROUND,
  ),
  ARTILLERY(
    displayName = "Artillery",
    shortName = "Arty",
    cost = 900,
    maxHp = 280f,
    speed = 50f,
    radius = 17f,
    attackRange = 420f,
    attackDamage = 100f,
    reloadSeconds = 2.6f,
    splashRadius = 60f,
    visionRadius = 260f,
    buildTimeSeconds = 8.5f,
    incomePerSec = 0,
    domain = UnitDomain.GROUND,
    targetDomain = TargetDomain.GROUND,
  ),
  FIGHTER(
    displayName = "Fighter Jet",
    shortName = "Fighter",
    cost = 850,
    maxHp = 320f,
    speed = 170f,
    radius = 16f,
    attackRange = 240f,
    attackDamage = 36f,
    reloadSeconds = 0.5f,
    splashRadius = 0f,
    visionRadius = 310f,
    buildTimeSeconds = 7.0f,
    incomePerSec = 0,
    domain = UnitDomain.AIR,
    targetDomain = TargetDomain.BOTH,
  ),
  EMP_MECH(
    displayName = "EMP Mech",
    shortName = "EMP Mech",
    cost = 950,
    maxHp = 450f,
    speed = 76f,
    radius = 16f,
    attackRange = 250f,
    attackDamage = 18f,
    reloadSeconds = 1.8f,
    splashRadius = 50f,
    visionRadius = 270f,
    buildTimeSeconds = 9.0f,
    incomePerSec = 0,
    domain = UnitDomain.GROUND,
    targetDomain = TargetDomain.BOTH,
  ),
  SHIELD_TANK(
    displayName = "Shield Tank",
    shortName = "Shield Tank",
    cost = 1100,
    maxHp = 700f,
    speed = 58f,
    radius = 19f,
    attackRange = 210f,
    attackDamage = 52f,
    reloadSeconds = 1.1f,
    splashRadius = 0f,
    visionRadius = 250f,
    buildTimeSeconds = 10.0f,
    incomePerSec = 0,
    domain = UnitDomain.GROUND,
    targetDomain = TargetDomain.GROUND,
    shieldMax = 400f,
  ),

  // Buildings
  COMMAND_CENTER(
    displayName = "Command Center",
    shortName = "Command",
    cost = 2500,
    maxHp = 2600f,
    speed = 0f,
    radius = 34f,
    attackRange = 230f,
    attackDamage = 24f,
    reloadSeconds = 0.8f,
    splashRadius = 0f,
    visionRadius = 360f,
    buildTimeSeconds = 15.0f,
    incomePerSec = 10,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.BOTH,
    canProduce = true,
  ),
  EXTRACTOR(
    displayName = "Resource Extractor",
    shortName = "Extractor",
    cost = 700,
    maxHp = 650f,
    speed = 0f,
    radius = 22f,
    attackRange = 0f,
    attackDamage = 0f,
    reloadSeconds = 1f,
    splashRadius = 0f,
    visionRadius = 190f,
    buildTimeSeconds = 6.0f,
    incomePerSec = 18,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.NONE,
  ),
  TURRET(
    displayName = "Gun Turret",
    shortName = "Turret",
    cost = 550,
    maxHp = 780f,
    speed = 0f,
    radius = 20f,
    attackRange = 280f,
    attackDamage = 40f,
    reloadSeconds = 0.6f,
    splashRadius = 0f,
    visionRadius = 300f,
    buildTimeSeconds = 5.0f,
    incomePerSec = 0,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.GROUND,
  ),
  ANTI_AIR(
    displayName = "Anti-Air Turret",
    shortName = "Anti-Air",
    cost = 600,
    maxHp = 680f,
    speed = 0f,
    radius = 20f,
    attackRange = 350f,
    attackDamage = 50f,
    reloadSeconds = 0.5f,
    splashRadius = 26f,
    visionRadius = 360f,
    buildTimeSeconds = 5.5f,
    incomePerSec = 0,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.AIR,
  ),
  LAND_FACTORY(
    displayName = "Land Factory",
    shortName = "Land Fac.",
    cost = 1000,
    maxHp = 1250f,
    speed = 0f,
    radius = 30f,
    attackRange = 0f,
    attackDamage = 0f,
    reloadSeconds = 1f,
    splashRadius = 0f,
    visionRadius = 240f,
    buildTimeSeconds = 8.0f,
    incomePerSec = 0,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.NONE,
    canProduce = true,
  ),
  AIR_FACTORY(
    displayName = "Air Factory",
    shortName = "Air Fac.",
    cost = 1200,
    maxHp = 1150f,
    speed = 0f,
    radius = 30f,
    attackRange = 0f,
    attackDamage = 0f,
    reloadSeconds = 1f,
    splashRadius = 0f,
    visionRadius = 240f,
    buildTimeSeconds = 9.0f,
    incomePerSec = 0,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.NONE,
    canProduce = true,
  ),
  TECH_CENTER(
    displayName = "Tech Center",
    shortName = "Tech Ctr",
    cost = 1800,
    maxHp = 1500f,
    speed = 0f,
    radius = 28f,
    attackRange = 0f,
    attackDamage = 0f,
    reloadSeconds = 1f,
    splashRadius = 0f,
    visionRadius = 250f,
    buildTimeSeconds = 12.0f,
    incomePerSec = 0,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.NONE,
    canProduce = true,
  ),
  MISSILE_SILO(
    displayName = "Missile Silo (Nuke)",
    shortName = "Nuke Silo",
    cost = 3000,
    maxHp = 1400f,
    speed = 0f,
    radius = 32f,
    attackRange = 9999f,
    attackDamage = 500f,
    reloadSeconds = 15.0f,
    splashRadius = 220f,
    visionRadius = 400f,
    buildTimeSeconds = 18.0f,
    incomePerSec = 0,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.BOTH,
  ),
  OIL_REFINERY(
    displayName = "Oil Refinery",
    shortName = "Refinery",
    cost = 1200,
    maxHp = 900f,
    speed = 0f,
    radius = 25f,
    attackRange = 0f,
    attackDamage = 0f,
    reloadSeconds = 1f,
    splashRadius = 0f,
    visionRadius = 220f,
    buildTimeSeconds = 10.0f,
    incomePerSec = 35,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.NONE,
  ),
  DEFENSE_WALL(
    displayName = "Defense Wall",
    shortName = "Wall",
    cost = 150,
    maxHp = 1200f,
    speed = 0f,
    radius = 16f,
    attackRange = 0f,
    attackDamage = 0f,
    reloadSeconds = 1f,
    splashRadius = 0f,
    visionRadius = 120f,
    buildTimeSeconds = 3.0f,
    incomePerSec = 0,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.NONE,
  ),
  STEEL_GATE(
    displayName = "Steel Gate",
    shortName = "Gate",
    cost = 300,
    maxHp = 1600f,
    speed = 0f,
    radius = 18f,
    attackRange = 0f,
    attackDamage = 0f,
    reloadSeconds = 1f,
    splashRadius = 0f,
    visionRadius = 140f,
    buildTimeSeconds = 4.0f,
    incomePerSec = 0,
    domain = UnitDomain.BUILDING,
    targetDomain = TargetDomain.NONE,
  );

  val isBuilding: Boolean
    get() = domain == UnitDomain.BUILDING

  fun producibleUnits(techLevel: Int): List<UnitType> =
    when (this) {
      COMMAND_CENTER -> listOf(BUILDER)
      LAND_FACTORY ->
        buildList {
          add(BUILDER)
          add(LIGHT_TANK)
          add(HEAVY_TANK)
          add(ARTILLERY)
          if (techLevel >= 2) {
            add(EMP_MECH)
            add(SHIELD_TANK)
          }
        }
      AIR_FACTORY -> listOf(FIGHTER)
      TECH_CENTER -> emptyList()
      else -> emptyList()
    }
}
