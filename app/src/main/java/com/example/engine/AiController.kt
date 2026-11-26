package com.example.engine

import com.example.model.AiDifficulty
import com.example.model.EntityState
import com.example.model.PlayerMatchState
import com.example.model.UnitDomain
import com.example.model.UnitType
import com.example.model.Vec2Data
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

object AiController {
  fun think(
    sim: GameSimulation,
    aiPlayer: PlayerMatchState,
    rng: Random,
  ) {
    if (aiPlayer.isEliminated) return
    val myEntities = sim.entities.filter { it.ownerId == aiPlayer.id }
    if (myEntities.isEmpty()) return

    val myBuildings = myEntities.filter { it.type.isBuilding }
    val myBuilders = myEntities.filter { it.type == UnitType.BUILDER }
    val myCombat = myEntities.filter { !it.type.isBuilding && it.type != UnitType.BUILDER }
    val myCc = myBuildings.firstOrNull { it.type == UnitType.COMMAND_CENTER } ?: myBuildings.firstOrNull()
    val homeX = myCc?.x ?: sim.map.spawnPoints.getOrNull(aiPlayer.spawnPoint - 1)?.x ?: (sim.map.worldWidth / 2f)
    val homeY = myCc?.y ?: sim.map.spawnPoints.getOrNull(aiPlayer.spawnPoint - 1)?.y ?: (sim.map.worldHeight / 2f)

    // 1. Tech Level Upgrade
    val techCenter = myBuildings.firstOrNull { it.type == UnitType.TECH_CENTER && !it.isUnderConstruction }
    if (aiPlayer.techLevel < 2 && techCenter == null && aiPlayer.credits >= UnitType.TECH_CENTER.cost) {
      val builder = myBuilders.firstOrNull { it.buildTaskBuildingId == null && it.waypoints.isEmpty() }
      if (builder != null) {
        findBuildSpotNear(sim, homeX, homeY, 130f, 250f, rng)?.let { spot ->
          sim.placeBuildingForPlayer(aiPlayer.id, UnitType.TECH_CENTER, spot.x, spot.y, listOf(builder.id))
        }
      }
    } else if (techCenter != null && aiPlayer.techLevel < 2) {
      val pIdx = sim.players.indexOfFirst { it.id == aiPlayer.id }
      if (pIdx >= 0) {
        sim.players[pIdx] = aiPlayer.copy(techLevel = 2)
      }
    }

    // 2. Produce Builders
    val desiredBuilders =
      when (aiPlayer.aiDifficulty) {
        AiDifficulty.EASY -> 1
        AiDifficulty.MEDIUM -> 2
        AiDifficulty.HARD -> 3
      }
    if (myBuilders.size < desiredBuilders && aiPlayer.credits >= UnitType.BUILDER.cost) {
      val producer =
        myBuildings.firstOrNull {
          !it.isUnderConstruction &&
            (it.type == UnitType.COMMAND_CENTER || it.type == UnitType.LAND_FACTORY) &&
            it.productionQueue.size < 2
        }
      if (producer != null) {
        sim.queueUnitProduction(aiPlayer.id, producer.id, UnitType.BUILDER)
      }
    }

    // 3. Idle Builder Tasks
    val unfinished = myBuildings.firstOrNull { it.isUnderConstruction }
    val idleBuilders =
      myBuilders.filter { b ->
        b.buildTaskBuildingId == null && b.waypoints.isEmpty()
      }

    if (idleBuilders.isNotEmpty()) {
      val builder = idleBuilders.first()
      if (unfinished != null) {
        sim.assignBuilderToBuilding(builder.id, unfinished.id)
      } else {
        val extractorsCount = myBuildings.count { it.type == UnitType.EXTRACTOR }
        val refineriesCount = myBuildings.count { it.type == UnitType.OIL_REFINERY }
        val wallsCount = myBuildings.count { it.type == UnitType.DEFENSE_WALL }
        val landFactoriesCount = myBuildings.count { it.type == UnitType.LAND_FACTORY }
        val airFactoriesCount = myBuildings.count { it.type == UnitType.AIR_FACTORY }
        val turretsCount = myBuildings.count { it.type == UnitType.TURRET }
        val aaCount = myBuildings.count { it.type == UnitType.ANTI_AIR }
        val siloCount = myBuildings.count { it.type == UnitType.MISSILE_SILO }

        val maxExtractors =
          when (aiPlayer.aiDifficulty) {
            AiDifficulty.EASY -> 2
            AiDifficulty.MEDIUM -> 3
            AiDifficulty.HARD -> 5
          }

        val nearestFreePool =
          sim.map.resourcePools
            .filter { pool ->
              sim.entities.none { e ->
                e.type == UnitType.EXTRACTOR && hypot(e.x - pool.x, e.y - pool.y) < 38f
              }
            }
            .minByOrNull { hypot(it.x - homeX, it.y - homeY) }

        val enemyAirExists =
          sim.entities.any { e ->
            val owner = sim.playersById[e.ownerId]
            owner != null && owner.team != aiPlayer.team && e.type.domain == UnitDomain.AIR
          }

        when {
          nearestFreePool != null &&
            extractorsCount < maxExtractors &&
            hypot(nearestFreePool.x - homeX, nearestFreePool.y - homeY) < 1200f &&
            aiPlayer.credits >= UnitType.EXTRACTOR.cost -> {
            sim.placeBuildingForPlayer(
              ownerId = aiPlayer.id,
              buildingType = UnitType.EXTRACTOR,
              worldX = nearestFreePool.x,
              worldY = nearestFreePool.y,
              builderIds = listOf(builder.id),
            )
          }
          refineriesCount < 2 && aiPlayer.credits >= UnitType.OIL_REFINERY.cost -> {
            findBuildSpotNear(sim, homeX, homeY, 90f, 180f, rng)?.let { spot ->
              sim.placeBuildingForPlayer(aiPlayer.id, UnitType.OIL_REFINERY, spot.x, spot.y, listOf(builder.id))
            }
          }
          landFactoriesCount == 0 && aiPlayer.credits >= UnitType.LAND_FACTORY.cost -> {
            findBuildSpotNear(sim, homeX, homeY, 120f, 240f, rng)?.let { spot ->
              sim.placeBuildingForPlayer(aiPlayer.id, UnitType.LAND_FACTORY, spot.x, spot.y, listOf(builder.id))
            }
          }
          turretsCount < 2 && aiPlayer.credits >= UnitType.TURRET.cost -> {
            findBuildSpotNear(sim, homeX, homeY, 140f, 270f, rng)?.let { spot ->
              sim.placeBuildingForPlayer(aiPlayer.id, UnitType.TURRET, spot.x, spot.y, listOf(builder.id))
            }
          }
          wallsCount < 4 && aiPlayer.credits >= UnitType.DEFENSE_WALL.cost -> {
            findBuildSpotNear(sim, homeX, homeY, 160f, 240f, rng)?.let { spot ->
              sim.placeBuildingForPlayer(aiPlayer.id, UnitType.DEFENSE_WALL, spot.x, spot.y, listOf(builder.id))
            }
          }
          (enemyAirExists || aiPlayer.aiDifficulty != AiDifficulty.EASY) &&
            aaCount < 2 &&
            aiPlayer.credits >= UnitType.ANTI_AIR.cost -> {
            findBuildSpotNear(sim, homeX, homeY, 110f, 250f, rng)?.let { spot ->
              sim.placeBuildingForPlayer(aiPlayer.id, UnitType.ANTI_AIR, spot.x, spot.y, listOf(builder.id))
            }
          }
          airFactoriesCount == 0 &&
            aiPlayer.aiDifficulty != AiDifficulty.EASY &&
            aiPlayer.credits >= UnitType.AIR_FACTORY.cost -> {
            findBuildSpotNear(sim, homeX, homeY, 150f, 290f, rng)?.let { spot ->
              sim.placeBuildingForPlayer(aiPlayer.id, UnitType.AIR_FACTORY, spot.x, spot.y, listOf(builder.id))
            }
          }
          aiPlayer.aiDifficulty == AiDifficulty.HARD &&
            siloCount < 1 &&
            aiPlayer.credits >= UnitType.MISSILE_SILO.cost -> {
            findBuildSpotNear(sim, homeX, homeY, 160f, 320f, rng)?.let { spot ->
              sim.placeBuildingForPlayer(aiPlayer.id, UnitType.MISSILE_SILO, spot.x, spot.y, listOf(builder.id))
            }
          }
        }
      }
    }

    // 4. Produce Combat Units
    val readyLandFactories =
      myBuildings.filter {
        !it.isUnderConstruction && it.type == UnitType.LAND_FACTORY && it.productionQueue.size < 3
      }
    for (fac in readyLandFactories) {
      val producible = fac.type.producibleUnits(aiPlayer.techLevel)
      val choice =
        when {
          UnitType.SHIELD_TANK in producible && aiPlayer.credits >= UnitType.SHIELD_TANK.cost && rng.nextFloat() < 0.35f ->
            UnitType.SHIELD_TANK
          UnitType.EMP_MECH in producible && aiPlayer.credits >= UnitType.EMP_MECH.cost && rng.nextFloat() < 0.3f ->
            UnitType.EMP_MECH
          aiPlayer.credits >= UnitType.ARTILLERY.cost && rng.nextFloat() < 0.22f ->
            UnitType.ARTILLERY
          aiPlayer.credits >= UnitType.HEAVY_TANK.cost && rng.nextFloat() < 0.45f ->
            UnitType.HEAVY_TANK
          aiPlayer.credits >= UnitType.LIGHT_TANK.cost ->
            UnitType.LIGHT_TANK
          else -> null
        }
      if (choice != null) {
        sim.queueUnitProduction(aiPlayer.id, fac.id, choice)
      }
    }

    // 5. Strategic Nuke Silo Launch by Hard AI
    val readySilos = myBuildings.filter { it.type == UnitType.MISSILE_SILO && !it.isUnderConstruction && it.reloadTimer <= 0f }
    if (readySilos.isNotEmpty() && aiPlayer.aiDifficulty == AiDifficulty.HARD) {
      val enemyTarget =
        sim.entities.firstOrNull { e ->
          val owner = sim.playersById[e.ownerId]
          owner != null && owner.team != aiPlayer.team && e.type.isBuilding
        } ?: sim.entities.firstOrNull { e ->
          val owner = sim.playersById[e.ownerId]
          owner != null && owner.team != aiPlayer.team
        }
      if (enemyTarget != null) {
        val silo = readySilos.first()
        sim.fireNukeMissile(silo.id, enemyTarget.x, enemyTarget.y)
      }
    }

    // 6. Combat Waves & Stances
    val enemies =
      sim.entities.filter { e ->
        val owner = sim.playersById[e.ownerId]
        owner != null && owner.team != aiPlayer.team
      }
    if (enemies.isEmpty()) return

    val baseThreat =
      enemies.firstOrNull { e -> hypot(e.x - homeX, e.y - homeY) < 450f }

    val idleCombat = myCombat.filter { it.waypoints.isEmpty() && it.targetUnitId == null }
    if (baseThreat != null && idleCombat.isNotEmpty()) {
      sim.issueAttackMove(idleCombat.map { it.id }, Vec2Data(baseThreat.x, baseThreat.y))
      return
    }

    val waveThreshold =
      when (aiPlayer.aiDifficulty) {
        AiDifficulty.EASY -> 6
        AiDifficulty.MEDIUM -> 4
        AiDifficulty.HARD -> 4
      }
    if (idleCombat.size >= waveThreshold) {
      val target =
        enemies
          .filter { it.type.isBuilding }
          .minByOrNull { hypot(it.x - homeX, it.y - homeY) }
          ?: enemies.minByOrNull { hypot(it.x - homeX, it.y - homeY) }
      if (target != null) {
        sim.issueAttackMove(idleCombat.map { it.id }, Vec2Data(target.x, target.y))
      }
    }
  }

  private fun findBuildSpotNear(
    sim: GameSimulation,
    cx: Float,
    cy: Float,
    minDist: Float,
    maxDist: Float,
    rng: Random,
  ): Vec2Data? {
    repeat(18) {
      val angle = rng.nextFloat() * (Math.PI.toFloat() * 2f)
      val dist = minDist + rng.nextFloat() * (maxDist - minDist)
      val wx = (cx + cos(angle) * dist).coerceIn(64f, sim.map.worldWidth - 64f)
      val wy = (cy + sin(angle) * dist).coerceIn(64f, sim.map.worldHeight - 64f)
      if (sim.canPlaceBuilding(UnitType.LAND_FACTORY, wx, wy)) {
        return Vec2Data(wx, wy)
      }
    }
    return null
  }
}
