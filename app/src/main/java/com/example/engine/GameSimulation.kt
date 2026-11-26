package com.example.engine

import com.example.model.CloudState
import com.example.model.EntityState
import com.example.model.FogMode
import com.example.model.GameOptions
import com.example.model.GameSaveData
import com.example.model.GameSettings
import com.example.model.LobbyPlayer
import com.example.model.MapId
import com.example.model.MatchResult
import com.example.model.ParticleState
import com.example.model.PlayerMatchState
import com.example.model.ProductionOrder
import com.example.model.ProjectileState
import com.example.model.ScorchDecal
import com.example.model.StartingUnitsOption
import com.example.model.TargetDomain
import com.example.model.TeamId
import com.example.model.TileType
import com.example.model.UnitDomain
import com.example.model.UnitStance
import com.example.model.UnitType
import com.example.model.Vec2Data
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class GameSimulation(
  val map: GameMap,
  val options: GameOptions,
  initialPlayers: List<PlayerMatchState>,
  val mapSeed: Long,
) {
  val players = initialPlayers.toMutableList()
  val playersById: Map<Int, PlayerMatchState>
    get() = players.associateBy { it.id }

  val humanPlayerId: Int = players.firstOrNull { it.isHuman }?.id ?: players.first().id

  val entities = mutableListOf<EntityState>()
  val projectiles = mutableListOf<ProjectileState>()
  val particles = mutableListOf<ParticleState>()
  val scorchDecals = mutableListOf<ScorchDecal>()
  val clouds = mutableListOf<CloudState>()
  val controlGroups = mutableMapOf<Int, List<Int>>(1 to emptyList(), 2 to emptyList(), 3 to emptyList())

  val fogGrid = IntArray(map.widthTiles * map.heightTiles)
  var fogVersion: Int = 1
    private set

  var elapsedSeconds: Float = 0f
  var nextEntityId: Int = 1
  private var nextProjectileId: Int = 1
  private var incomeAccumulator: Float = 0f
  private var fogAccumulator: Float = 0f
  private var autosaveAccumulator: Float = 0f
  private val rng = Random(mapSeed + 99L)

  var audioEngine: GameAudioEngine? = null
  var settings: GameSettings = GameSettings()
  var matchResult: MatchResult? = null
    private set

  init {
    spawnInitialClouds()
  }

  private fun spawnInitialClouds() {
    clouds.clear()
    repeat(10) {
      clouds.add(
        CloudState(
          x = rng.nextFloat() * map.worldWidth,
          y = rng.nextFloat() * map.worldHeight,
          radiusX = 160f + rng.nextFloat() * 140f,
          radiusY = 95f + rng.nextFloat() * 80f,
          speedX = 12f + rng.nextFloat() * 14f,
          speedY = 4f + rng.nextFloat() * 6f,
          alpha = 0.14f + rng.nextFloat() * 0.08f,
        )
      )
    }
  }

  fun populateNewMatch() {
    entities.clear()
    projectiles.clear()
    particles.clear()
    scorchDecals.clear()
    for (p in players) {
      val spIdx = (p.spawnPoint - 1).coerceIn(0, map.spawnPoints.lastIndex)
      val sp = map.spawnPoints[spIdx]
      entities.add(
        EntityState(
          id = nextEntityId++,
          ownerId = p.id,
          type = UnitType.COMMAND_CENTER,
          x = sp.x,
          y = sp.y,
          hp = UnitType.COMMAND_CENTER.maxHp,
          maxHp = UnitType.COMMAND_CENTER.maxHp,
          isUnderConstruction = false,
          buildProgress = 1f,
          rallyPoint = Vec2Data(sp.x, sp.y + 76f),
        )
      )
      if (options.startingUnits == StartingUnitsOption.COMMAND_CENTER_PLUS_BUILDER) {
        entities.add(
          EntityState(
            id = nextEntityId++,
            ownerId = p.id,
            type = UnitType.BUILDER,
            x = sp.x + 58f,
            y = sp.y + 58f,
            hp = UnitType.BUILDER.maxHp,
            maxHp = UnitType.BUILDER.maxHp,
            bodyAngle = -0.8f,
            turretAngle = -0.8f,
          )
        )
      }
    }
    recalculatePlayerIncomes()
    updateFogOfWar()
  }

  fun step(dt: Float, onAutosave: (() -> Unit)? = null) {
    if (matchResult != null) return
    val clampedDt = dt.coerceIn(0.001f, 0.1f)
    elapsedSeconds += clampedDt

    // 1. Clouds
    if (settings.renderClouds) {
      for (i in clouds.indices) {
        val c = clouds[i]
        var nx = c.x + c.speedX * clampedDt
        var ny = c.y + c.speedY * clampedDt
        if (nx > map.worldWidth + c.radiusX) nx = -c.radiusX
        if (ny > map.worldHeight + c.radiusY) ny = -c.radiusY
        clouds[i] = c.copy(x = nx, y = ny)
      }
    }

    // 2. Income tick every 1s
    incomeAccumulator += clampedDt
    if (incomeAccumulator >= 1.0f) {
      incomeAccumulator -= 1.0f
      recalculatePlayerIncomes()
      for (i in players.indices) {
        val p = players[i]
        if (!p.isEliminated) {
          val bonus = if (!p.isHuman) p.aiDifficulty.incomeBonus else 1.0f
          val gain = (p.incomePerSec * bonus).toInt().coerceAtLeast(1)
          players[i] =
            p.copy(
              credits = p.credits + gain,
              totalCreditsEarned = p.totalCreditsEarned + gain,
            )
        }
      }
    }

    // 3. AI Think
    for (i in players.indices) {
      val p = players[i]
      if (!p.isHuman && !p.isEliminated) {
        val timer = p.aiThinkTimer - clampedDt
        if (timer <= 0f) {
          players[i] = p.copy(aiThinkTimer = p.aiDifficulty.thinkIntervalSec)
          AiController.think(this, players[i], rng)
        } else {
          players[i] = p.copy(aiThinkTimer = timer)
        }
      }
    }

    // 4. Update entities
    val pMap = playersById
    val entityMap = entities.associateBy { it.id }.toMutableMap()
    val newlySpawned = mutableListOf<EntityState>()

    for (i in entities.indices) {
      var e = entities[i]
      val owner = pMap[e.ownerId] ?: continue

      if (e.muzzleFlashTimer > 0f) {
        e = e.copy(muzzleFlashTimer = (e.muzzleFlashTimer - clampedDt).coerceAtLeast(0f))
      }
      if (e.reloadTimer > 0f) {
        e = e.copy(reloadTimer = (e.reloadTimer - clampedDt).coerceAtLeast(0f))
      }
      if (e.chaseRepathTimer > 0f) {
        e = e.copy(chaseRepathTimer = (e.chaseRepathTimer - clampedDt).coerceAtLeast(0f))
      }

      // Shield regeneration for Shield Tank / Tech units
      if (e.type.shieldMax > 0f && e.shieldHp < e.type.shieldMax) {
        e = e.copy(shieldHp = (e.shieldHp + 18f * clampedDt).coerceAtMost(e.type.shieldMax))
      }

      // Production queue
      if (e.type.canProduce && !e.isUnderConstruction && e.productionQueue.isNotEmpty()) {
        val queue = e.productionQueue.toMutableList()
        val front = queue[0]
        val rem = front.remainingSeconds - clampedDt
        if (rem <= 0f) {
          val countForOwner = entities.count { it.ownerId == e.ownerId && !it.type.isBuilding }
          if (countForOwner < settings.unitCap) {
            queue.removeAt(0)
            val spawnAngle = rng.nextFloat() * 6.28f
            val spawnDist = e.type.radius + front.unitType.radius + 10f
            val sx = (e.x + cos(spawnAngle) * spawnDist).coerceIn(32f, map.worldWidth - 32f)
            val sy = (e.y + sin(spawnAngle) * spawnDist).coerceIn(32f, map.worldHeight - 32f)
            val rally = e.rallyPoint ?: Vec2Data(e.x, (e.y + e.type.radius + 54f).coerceAtMost(map.worldHeight - 32f))
            val newUnit =
              EntityState(
                id = nextEntityId++,
                ownerId = e.ownerId,
                type = front.unitType,
                x = sx,
                y = sy,
                hp = front.unitType.maxHp,
                maxHp = front.unitType.maxHp,
                bodyAngle = atan2(rally.y - sy, rally.x - sx),
                turretAngle = atan2(rally.y - sy, rally.x - sx),
                shieldHp = front.unitType.shieldMax,
                maxShieldHp = front.unitType.shieldMax,
                waypoints =
                  Pathfinder.findPath(
                    map = map,
                    startWorld = Vec2Data(sx, sy),
                    goalWorld = rally,
                    entities = entities,
                    ignoreBuildingId = e.id,
                    isAirUnit = front.unitType.domain == UnitDomain.AIR,
                  ),
              )
            newlySpawned.add(newUnit)
            incrementPlayerUnitsBuilt(e.ownerId)
            if (e.ownerId == humanPlayerId) {
              audioEngine?.play(SoundEffect.BUILD_COMPLETE)
            }
          }
        } else {
          queue[0] = front.copy(remainingSeconds = rem)
        }
        e = e.copy(productionQueue = queue)
      }

      if (e.isUnderConstruction) {
        entities[i] = e
        continue
      }

      // Builder construction & repair behavior
      if (e.type == UnitType.BUILDER) {
        val taskBuilding = e.buildTaskBuildingId?.let { entityMap[it] }
        if (taskBuilding != null && taskBuilding.isUnderConstruction) {
          val dist = hypot(taskBuilding.x - e.x, taskBuilding.y - e.y)
          val buildRange = e.type.attackRange + taskBuilding.type.radius
          if (dist <= buildRange) {
            val angleToSite = atan2(taskBuilding.y - e.y, taskBuilding.x - e.x)
            e = e.copy(waypoints = emptyList(), bodyAngle = angleToSite, turretAngle = angleToSite)
            val newProg =
              (taskBuilding.buildProgress + clampedDt / taskBuilding.type.buildTimeSeconds).coerceAtMost(1f)
            val completed = newProg >= 1f
            val updatedBuilding =
              taskBuilding.copy(
                buildProgress = newProg,
                isUnderConstruction = !completed,
                hp =
                  (taskBuilding.hp + (taskBuilding.maxHp / taskBuilding.type.buildTimeSeconds) * clampedDt)
                    .coerceAtMost(taskBuilding.maxHp),
              )
            entityMap[taskBuilding.id] = updatedBuilding
            val bIdx = entities.indexOfFirst { it.id == taskBuilding.id }
            if (bIdx >= 0) entities[bIdx] = updatedBuilding
            if (completed) {
              if (e.ownerId == humanPlayerId) audioEngine?.play(SoundEffect.BUILD_COMPLETE)
              e = e.copy(buildTaskBuildingId = null)
            }
          } else if (e.waypoints.isEmpty()) {
            val path =
              Pathfinder.findPath(
                map,
                Vec2Data(e.x, e.y),
                Vec2Data(taskBuilding.x, taskBuilding.y),
                entities,
                ignoreBuildingId = taskBuilding.id,
              )
            e = e.copy(waypoints = path)
          }
        } else {
          if (e.buildTaskBuildingId != null) {
            e = e.copy(buildTaskBuildingId = null)
          }
          if (e.waypoints.isEmpty()) {
            val damaged =
              entities.firstOrNull { other ->
                other.id != e.id &&
                  other.ownerId == e.ownerId &&
                  !other.isUnderConstruction &&
                  other.hp < other.maxHp &&
                  hypot(other.x - e.x, other.y - e.y) <= e.type.attackRange + other.type.radius + 16f
              }
            if (damaged != null) {
              val ang = atan2(damaged.y - e.y, damaged.x - e.x)
              e = e.copy(turretAngle = ang, repairTargetId = damaged.id)
              val repaired = damaged.copy(hp = (damaged.hp + 30f * clampedDt).coerceAtMost(damaged.maxHp))
              val dIdx = entities.indexOfFirst { it.id == damaged.id }
              if (dIdx >= 0) entities[dIdx] = repaired
            } else {
              e = e.copy(repairTargetId = null)
            }
          } else {
            e = e.copy(repairTargetId = null)
          }
        }
      }

      // Explicit Attack Target Chasing
      val explicitTarget = e.explicitTargetId?.let { entityMap[it] }
      var stopToShoot = false
      if (e.explicitTargetId != null) {
        if (explicitTarget == null || !isValidTarget(e, explicitTarget, owner.team, pMap)) {
          e = e.copy(explicitTargetId = null)
        } else if (!e.type.isBuilding) {
          val distToExplicit = hypot(explicitTarget.x - e.x, explicitTarget.y - e.y)
          val fireRange = e.type.attackRange + explicitTarget.type.radius
          if (distToExplicit <= fireRange * 0.92f) {
            stopToShoot = true
            e = e.copy(waypoints = emptyList())
          } else if (e.chaseRepathTimer <= 0f) {
            val path =
              Pathfinder.findPath(
                map = map,
                startWorld = Vec2Data(e.x, e.y),
                goalWorld = Vec2Data(explicitTarget.x, explicitTarget.y),
                entities = entities,
                ignoreBuildingId = explicitTarget.id,
                isAirUnit = e.type.domain == UnitDomain.AIR,
              )
            e = e.copy(waypoints = path, chaseRepathTimer = 0.55f)
          }
        }
      }

      // Movement + Separation + Building Push-out
      if (!e.type.isBuilding && !stopToShoot && e.waypoints.isNotEmpty()) {
        val wp = e.waypoints.first()
        val dx = wp.x - e.x
        val dy = wp.y - e.y
        val dist = hypot(dx, dy)
        if (dist <= 11f) {
          e = e.copy(waypoints = e.waypoints.drop(1))
        } else {
          val moveStep = e.type.speed * clampedDt
          var vx = (dx / dist) * min(moveStep, dist)
          var vy = (dy / dist) * min(moveStep, dist)

          var sepX = 0f
          var sepY = 0f
          for (other in entities) {
            if (other.id == e.id) continue
            if (other.type.domain != e.type.domain) continue
            val ox = e.x - other.x
            val oy = e.y - other.y
            val d = hypot(ox, oy)
            val minSep = e.type.radius + other.type.radius + 4f
            if (d in 0.01f..minSep) {
              val push = (minSep - d) / minSep
              sepX += (ox / d) * push * 52f * clampedDt
              sepY += (oy / d) * push * 52f * clampedDt
            }
          }
          vx += sepX
          vy += sepY

          var nx = (e.x + vx).coerceIn(e.type.radius, map.worldWidth - e.type.radius)
          var ny = (e.y + vy).coerceIn(e.type.radius, map.worldHeight - e.type.radius)

          if (e.type.domain == UnitDomain.GROUND) {
            for (other in entities) {
              if (!other.type.isBuilding) continue
              val bx = nx - other.x
              val by = ny - other.y
              val bd = hypot(bx, by)
              val minD = e.type.radius + other.type.radius + 2f
              if (bd in 0.01f..minD) {
                nx = other.x + (bx / bd) * minD
                ny = other.y + (by / bd) * minD
              }
            }

            val tx = map.worldToTileX(nx)
            val ty = map.worldToTileY(ny)
            if (!map.tileAt(tx, ty).isWalkableGround) {
              val txOnly = map.worldToTileX(nx)
              val tyCur = map.worldToTileY(e.y)
              val txCur = map.worldToTileX(e.x)
              val tyOnly = map.worldToTileY(ny)
              if (map.tileAt(txOnly, tyCur).isWalkableGround) {
                ny = e.y
              } else if (map.tileAt(txCur, tyOnly).isWalkableGround) {
                nx = e.x
              } else {
                nx = e.x
                ny = e.y
              }
            }
          }

          val targetAngle = atan2(dy, dx)
          val newBodyAngle = rotateTowards(e.bodyAngle, targetAngle, 6.2f * clampedDt)
          e = e.copy(x = nx, y = ny, bodyAngle = newBodyAngle)
        }
      }

      // Combat targeting & Stances (Aggressive vs Hold Ground vs Retreat)
      if (e.type.targetDomain != TargetDomain.NONE && e.type.attackDamage > 0f) {
        val priorityExplicit =
          explicitTarget?.takeIf {
            isValidTarget(e, it, owner.team, pMap) &&
              hypot(it.x - e.x, it.y - e.y) <= e.type.attackRange + it.type.radius
          }
        val currentTarget = e.targetUnitId?.let { entityMap[it] }
        val validCurrent =
          currentTarget?.takeIf {
            isValidTarget(e, it, owner.team, pMap) &&
              hypot(it.x - e.x, it.y - e.y) <= e.type.attackRange + it.type.radius
          }

        val chosenTarget =
          when (e.stance) {
            UnitStance.HOLD_GROUND -> priorityExplicit ?: validCurrent ?: findBestTargetInRange(e, owner.team, pMap)
            UnitStance.RETREAT -> null
            UnitStance.AGGRESSIVE -> priorityExplicit ?: validCurrent ?: findBestTargetInRange(e, owner.team, pMap)
          }

        if (chosenTarget != null) {
          val aimAngle = atan2(chosenTarget.y - e.y, chosenTarget.x - e.x)
          val newTurret = rotateTowards(e.turretAngle, aimAngle, 8.2f * clampedDt)
          val newBody =
            if (e.type == UnitType.FIGHTER && e.waypoints.isEmpty()) {
              rotateTowards(e.bodyAngle, aimAngle, 5.5f * clampedDt)
            } else e.bodyAngle
          e = e.copy(targetUnitId = chosenTarget.id, turretAngle = newTurret, bodyAngle = newBody)
          val dist = hypot(chosenTarget.x - e.x, chosenTarget.y - e.y)
          if (dist <= e.type.attackRange + chosenTarget.type.radius && e.reloadTimer <= 0f) {
            fireProjectile(e, chosenTarget, owner)
            e = e.copy(reloadTimer = e.type.reloadSeconds, muzzleFlashTimer = 0.12f)
          }
        } else {
          val idleTurret =
            if (!e.type.isBuilding) rotateTowards(e.turretAngle, e.bodyAngle, 3.2f * clampedDt)
            else e.turretAngle
          e = e.copy(targetUnitId = null, turretAngle = idleTurret)

          val atkGoal = e.attackMoveTarget
          if (atkGoal != null && e.waypoints.isEmpty() && !e.type.isBuilding && e.stance == UnitStance.AGGRESSIVE) {
            if (hypot(atkGoal.x - e.x, atkGoal.y - e.y) > 48f) {
              val path =
                Pathfinder.findPath(
                  map,
                  Vec2Data(e.x, e.y),
                  atkGoal,
                  entities,
                  isAirUnit = e.type.domain == UnitDomain.AIR,
                )
              e = e.copy(waypoints = path)
            } else {
              e = e.copy(attackMoveTarget = null)
            }
          }
        }
      }

      entities[i] = e
    }

    if (newlySpawned.isNotEmpty()) {
      entities.addAll(newlySpawned)
    }

    // 5. Update Projectiles
    val destroyedIds = mutableSetOf<Int>()
    val projIter = projectiles.listIterator()
    while (projIter.hasNext()) {
      val proj = projIter.next()
      val targetEntity = proj.targetUnitId?.let { tid -> entities.firstOrNull { it.id == tid } }
      val tx = targetEntity?.x ?: proj.targetX
      val ty = targetEntity?.y ?: proj.targetY
      val dx = tx - proj.x
      val dy = ty - proj.y
      val dist = hypot(dx, dy)
      val stepDist = proj.speed * clampedDt

      if (dist <= stepDist + 8f) {
        applyProjectileImpact(proj, tx, ty, destroyedIds)
        projIter.remove()
      } else {
        projIter.set(
          proj.copy(
            prevX = proj.x,
            prevY = proj.y,
            x = proj.x + (dx / dist) * stepDist,
            y = proj.y + (dy / dist) * stepDist,
            targetX = tx,
            targetY = ty,
          )
        )
      }
    }

    if (destroyedIds.isNotEmpty()) {
      val deadEntities = entities.filter { it.id in destroyedIds }
      for (dead in deadEntities) {
        incrementPlayerUnitsLost(dead.ownerId)
        spawnExplosion(dead.x, dead.y, if (dead.type.isBuilding) 1.8f else 1.0f, leaveScorch = true)
      }
      audioEngine?.play(SoundEffect.EXPLOSION)
      entities.removeAll { it.id in destroyedIds }
      for (k in controlGroups.keys) {
        controlGroups[k] = controlGroups[k].orEmpty().filter { it !in destroyedIds }
      }
      checkEliminationsAndVictory()
    }

    // 6. Update Particles
    val partIter = particles.listIterator()
    while (partIter.hasNext()) {
      val pt = partIter.next()
      val rem = pt.life - clampedDt
      if (rem <= 0f) {
        partIter.remove()
      } else {
        partIter.set(
          pt.copy(
            x = pt.x + pt.vx * clampedDt,
            y = pt.y + pt.vy * clampedDt,
            vx = pt.vx * 0.94f,
            vy = pt.vy * 0.94f,
            life = rem,
          )
        )
      }
    }

    // 7. Fog refresh
    fogAccumulator += clampedDt
    if (fogAccumulator >= 0.2f) {
      fogAccumulator = 0f
      updateFogOfWar()
    }

    // 8. Autosave
    if (settings.autosaveEnabled && onAutosave != null) {
      autosaveAccumulator += clampedDt
      if (autosaveAccumulator >= 45f) {
        autosaveAccumulator = 0f
        onAutosave()
      }
    }
  }

  private fun isValidTarget(
    attacker: EntityState,
    target: EntityState,
    attackerTeam: com.example.model.TeamId,
    pMap: Map<Int, PlayerMatchState>,
  ): Boolean {
    val tOwner = pMap[target.ownerId] ?: return false
    if (tOwner.team == attackerTeam) return false
    return when (attacker.type.targetDomain) {
      TargetDomain.NONE -> false
      TargetDomain.GROUND -> target.type.domain != UnitDomain.AIR
      TargetDomain.AIR -> target.type.domain == UnitDomain.AIR
      TargetDomain.BOTH -> true
    }
  }

  private fun findBestTargetInRange(
    attacker: EntityState,
    attackerTeam: com.example.model.TeamId,
    pMap: Map<Int, PlayerMatchState>,
  ): EntityState? {
    var best: EntityState? = null
    var bestScore = Float.POSITIVE_INFINITY
    for (other in entities) {
      if (!isValidTarget(attacker, other, attackerTeam, pMap)) continue
      val d = hypot(other.x - attacker.x, other.y - attacker.y)
      if (d <= attacker.type.attackRange + other.type.radius) {
        val priorityBias = if (other.type.isBuilding) 80f else 0f
        val score = d + priorityBias
        if (score < bestScore) {
          bestScore = score
          best = other
        }
      }
    }
    return best
  }

  private fun fireProjectile(
    attacker: EntityState,
    target: EntityState,
    owner: PlayerMatchState,
  ) {
    val barrelOffset = attacker.type.radius * 0.85f
    val sx = attacker.x + cos(attacker.turretAngle) * barrelOffset
    val sy = attacker.y + sin(attacker.turretAngle) * barrelOffset
    val isEmp = attacker.type == UnitType.EMP_MECH
    val projSpeed =
      when (attacker.type) {
        UnitType.ARTILLERY -> 320f
        UnitType.ANTI_AIR -> 540f
        UnitType.FIGHTER -> 500f
        UnitType.EMP_MECH -> 400f
        else -> 440f
      }
    val projColor =
      when (attacker.type) {
        UnitType.ARTILLERY -> 0xFFFF9100.toInt()
        UnitType.ANTI_AIR -> 0xFF69F0AE.toInt()
        UnitType.FIGHTER -> 0xFF40C4FF.toInt()
        UnitType.EMP_MECH -> 0xFF00E5FF.toInt()
        else -> 0xFFFFEA00.toInt()
      }
    projectiles.add(
      ProjectileState(
        id = nextProjectileId++,
        ownerId = attacker.ownerId,
        team = owner.team,
        sourceType = attacker.type,
        x = sx,
        y = sy,
        prevX = sx,
        prevY = sy,
        targetX = target.x,
        targetY = target.y,
        targetUnitId = target.id,
        speed = projSpeed,
        damage = attacker.type.attackDamage,
        splashRadius = attacker.type.splashRadius,
        targetDomain = attacker.type.targetDomain,
        colorArgb = projColor,
        isEmp = isEmp,
      )
    )
    audioEngine?.play(
      when (attacker.type) {
        UnitType.ARTILLERY, UnitType.HEAVY_TANK -> SoundEffect.SHOOT_HEAVY
        UnitType.ANTI_AIR -> SoundEffect.SHOOT_AA
        else -> SoundEffect.SHOOT_LIGHT
      }
    )
  }

  fun fireNukeMissile(siloId: Int, targetX: Float, targetY: Float): Boolean {
    val sIdx = entities.indexOfFirst { it.id == siloId && it.type == UnitType.MISSILE_SILO && !it.isUnderConstruction }
    if (sIdx < 0) return false
    val silo = entities[sIdx]
    if (silo.reloadTimer > 0f) return false

    entities[sIdx] = silo.copy(reloadTimer = UnitType.MISSILE_SILO.reloadSeconds)
    projectiles.add(
      ProjectileState(
        id = nextProjectileId++,
        ownerId = silo.ownerId,
        team = playersById[silo.ownerId]?.team ?: TeamId.A,
        sourceType = UnitType.MISSILE_SILO,
        x = silo.x,
        y = silo.y,
        prevX = silo.x,
        prevY = silo.y,
        targetX = targetX,
        targetY = targetY,
        targetUnitId = null,
        speed = 260f,
        damage = UnitType.MISSILE_SILO.attackDamage,
        splashRadius = UnitType.MISSILE_SILO.splashRadius,
        targetDomain = TargetDomain.BOTH,
        colorArgb = 0xFFFF6D00.toInt(),
        isNuke = true,
      )
    )
    audioEngine?.play(SoundEffect.MISSILE_LAUNCH)
    return true
  }

  private fun applyProjectileImpact(
    proj: ProjectileState,
    ix: Float,
    iy: Float,
    destroyedIds: MutableSet<Int>,
  ) {
    val pMap = playersById
    val actualSplash = if (proj.isNuke) 220f else proj.splashRadius

    audioEngine?.play(if (proj.isNuke || actualSplash > 40f) SoundEffect.EXPLOSION else SoundEffect.HIT_IMPACT)
    for (s in 0..4) {
      val angle = rng.nextFloat() * 6.28f
      val speed = 35f + rng.nextFloat() * 65f
      particles.add(
        ParticleState(
          x = ix,
          y = iy,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          life = 0.2f,
          maxLife = 0.2f,
          radius = 2f,
          colorArgb = proj.colorArgb,
          isSmoke = false,
        )
      )
    }

    if (actualSplash > 0f) {
      for (idx in entities.indices) {
        val e = entities[idx]
        val eOwner = pMap[e.ownerId] ?: continue
        if (eOwner.team == proj.team) continue
        val dist = hypot(e.x - ix, e.y - iy)
        if (dist <= actualSplash + e.type.radius) {
          val falloff = (1f - (dist / (actualSplash + e.type.radius)) * 0.5f).coerceIn(0.3f, 1f)
          var dmg = proj.damage * falloff
          var newShield = e.shieldHp
          if (newShield > 0f) {
            val absorbed = min(newShield, dmg)
            newShield -= absorbed
            dmg -= absorbed
          }
          val newHp = e.hp - dmg
          entities[idx] = e.copy(hp = newHp, shieldHp = newShield)
          if (newHp <= 0f) {
            if (destroyedIds.add(e.id)) incrementEnemiesDestroyed(proj.ownerId)
          }
        }
      }
      spawnExplosion(ix, iy, if (proj.isNuke) 2.4f else 0.85f, leaveScorch = true)
    } else {
      val idx = entities.indexOfFirst { it.id == proj.targetUnitId }
      if (idx >= 0) {
        val e = entities[idx]
        var dmg = proj.damage
        var newShield = e.shieldHp
        if (newShield > 0f) {
          val absorbed = min(newShield, dmg)
          newShield -= absorbed
          dmg -= absorbed
        }
        val newHp = e.hp - dmg
        entities[idx] = e.copy(hp = newHp, shieldHp = newShield)
        if (newHp <= 0f) {
          if (destroyedIds.add(e.id)) incrementEnemiesDestroyed(proj.ownerId)
        }
      }
    }
  }

  private fun spawnExplosion(x: Float, y: Float, scale: Float, leaveScorch: Boolean) {
    if (leaveScorch && scorchDecals.size < 95) {
      scorchDecals.add(
        ScorchDecal(
          x = x,
          y = y,
          radius = 18f * scale,
          alpha = 0.5f,
        )
      )
    }
    particles.add(
      ParticleState(
        x = x,
        y = y,
        vx = 0f,
        vy = 0f,
        life = 0.4f * scale,
        maxLife = 0.4f * scale,
        radius = 30f * scale,
        colorArgb = 0xFFFFAB40.toInt(),
        isRing = true,
      )
    )
    repeat((14 * scale).toInt()) {
      val a = rng.nextFloat() * 6.28f
      val sp = (35f + rng.nextFloat() * 110f) * scale
      val isSmoke = rng.nextFloat() < 0.45f
      particles.add(
        ParticleState(
          x = x,
          y = y,
          vx = cos(a) * sp,
          vy = sin(a) * sp,
          life = (0.35f + rng.nextFloat() * 0.5f) * scale,
          maxLife = 0.85f * scale,
          radius = (7f + rng.nextFloat() * 9f) * scale,
          colorArgb = if (isSmoke) 0xFF37474F.toInt() else if (rng.nextBoolean()) 0xFFFF6D00.toInt() else 0xFFFFD54F.toInt(),
          isSmoke = isSmoke,
        )
      )
    }
  }

  fun updateFogOfWar() {
    val total = map.widthTiles * map.heightTiles
    if (options.fogMode == FogMode.NONE) {
      fogGrid.fill(2)
      fogVersion++
      return
    }
    for (i in 0 until total) {
      if (fogGrid[i] == 2) fogGrid[i] = 1
    }
    val humanTeam = playersById[humanPlayerId]?.team ?: return
    val pMap = playersById
    for (e in entities) {
      val owner = pMap[e.ownerId] ?: continue
      if (owner.team != humanTeam) continue
      val visionTiles = (e.type.visionRadius / map.tileSize).toInt() + 1
      val cx = map.worldToTileX(e.x)
      val cy = map.worldToTileY(e.y)
      val rSq = (e.type.visionRadius / map.tileSize) * (e.type.visionRadius / map.tileSize)
      for (dy in -visionTiles..visionTiles) {
        val ty = cy + dy
        if (ty !in 0 until map.heightTiles) continue
        for (dx in -visionTiles..visionTiles) {
          val tx = cx + dx
          if (tx !in 0 until map.widthTiles) continue
          if (dx * dx + dy * dy <= rSq) {
            fogGrid[ty * map.widthTiles + tx] = 2
          }
        }
      }
    }
    fogVersion++
  }

  private fun checkEliminationsAndVictory() {
    for (i in players.indices) {
      val p = players[i]
      if (!p.isEliminated) {
        val aliveEntities = entities.filter { it.ownerId == p.id }
        val hasBuildingOrBuilder = aliveEntities.any { it.type.isBuilding || it.type == UnitType.BUILDER }
        if (aliveEntities.isEmpty() || !hasBuildingOrBuilder) {
          players[i] = p.copy(isEliminated = true)
        }
      }
    }

    val aliveTeams = players.filter { !it.isEliminated }.map { it.team }.distinct()
    val humanPlayer = playersById[humanPlayerId]
    if (humanPlayer != null && humanPlayer.isEliminated) {
      val winner = aliveTeams.firstOrNull() ?: players.first().team
      matchResult =
        MatchResult(
          isVictory = false,
          winningTeam = winner,
          elapsedSeconds = elapsedSeconds,
          playerStats = humanPlayer,
          allPlayers = players.toList(),
        )
    } else if (aliveTeams.size == 1 && players.size > 1) {
      val winner = aliveTeams.first()
      val humanTeam = humanPlayer?.team
      matchResult =
        MatchResult(
          isVictory = (winner == humanTeam),
          winningTeam = winner,
          elapsedSeconds = elapsedSeconds,
          playerStats = humanPlayer ?: players.first(),
          allPlayers = players.toList(),
        )
    }
  }

  fun recalculatePlayerIncomes() {
    for (i in players.indices) {
      val p = players[i]
      val baseIncome =
        entities
          .filter { it.ownerId == p.id && !it.isUnderConstruction }
          .sumOf { it.type.incomePerSec }
      val total = (baseIncome * options.incomeMultiplier).coerceAtLeast(if (p.isEliminated) 0 else 2)
      players[i] = p.copy(incomePerSec = total)
    }
  }

  fun setUnitStance(unitIds: List<Int>, stance: UnitStance) {
    for (uid in unitIds) {
      val idx = entities.indexOfFirst { it.id == uid }
      if (idx >= 0 && !entities[idx].type.isBuilding) {
        entities[idx] = entities[idx].copy(stance = stance)
      }
    }
  }

  fun snapBuildPosition(buildingType: UnitType, worldX: Float, worldY: Float): Vec2Data {
    if (buildingType == UnitType.EXTRACTOR) {
      val nearest = map.resourcePools.minByOrNull { hypot(it.x - worldX, it.y - worldY) }
      if (nearest != null && hypot(nearest.x - worldX, nearest.y - worldY) <= 145f) {
        return nearest
      }
    }
    val tx = map.worldToTileX(worldX)
    val ty = map.worldToTileY(worldY)
    return map.tileCenterWorld(tx, ty)
  }

  fun canPlaceBuilding(buildingType: UnitType, worldX: Float, worldY: Float): Boolean {
    val tx = map.worldToTileX(worldX)
    val ty = map.worldToTileY(worldY)
    val tile = map.tileAt(tx, ty)
    if (buildingType == UnitType.EXTRACTOR) {
      if (tile != TileType.RESOURCE_POOL) return false
    } else {
      if (!tile.isBuildable) return false
    }
    for (e in entities) {
      if (e.type.isBuilding) {
        val minDist = buildingType.radius + e.type.radius + 6f
        if (hypot(e.x - worldX, e.y - worldY) < minDist) return false
      }
    }
    return true
  }

  fun placeBuildingForPlayer(
    ownerId: Int,
    buildingType: UnitType,
    worldX: Float,
    worldY: Float,
    builderIds: List<Int>,
  ): Int? {
    if (!buildingType.isBuilding) return null
    val pIdx = players.indexOfFirst { it.id == ownerId }
    if (pIdx < 0) return null
    val player = players[pIdx]
    if (player.credits < buildingType.cost) return null

    val snapped = snapBuildPosition(buildingType, worldX, worldY)
    if (!canPlaceBuilding(buildingType, snapped.x, snapped.y)) return null

    players[pIdx] =
      player.copy(
        credits = player.credits - buildingType.cost,
        buildingsBuilt = player.buildingsBuilt + 1,
      )
    val bId = nextEntityId++
    val building =
      EntityState(
        id = bId,
        ownerId = ownerId,
        type = buildingType,
        x = snapped.x,
        y = snapped.y,
        hp = buildingType.maxHp * 0.1f,
        maxHp = buildingType.maxHp,
        isUnderConstruction = true,
        buildProgress = 0.05f,
        rallyPoint =
          if (buildingType.canProduce) Vec2Data(snapped.x, (snapped.y + 72f).coerceAtMost(map.worldHeight - 32f))
          else null,
      )
    entities.add(building)

    for (bUid in builderIds) {
      assignBuilderToBuilding(bUid, bId)
    }
    if (ownerId == humanPlayerId) {
      audioEngine?.play(SoundEffect.UI_CONFIRM)
    }
    return bId
  }

  fun assignBuilderToBuilding(builderId: Int, buildingId: Int) {
    val idx = entities.indexOfFirst { it.id == builderId && it.type == UnitType.BUILDER }
    val target = entities.firstOrNull { it.id == buildingId } ?: return
    if (idx < 0) return
    val builder = entities[idx]
    val path =
      Pathfinder.findPath(
        map = map,
        startWorld = Vec2Data(builder.x, builder.y),
        goalWorld = Vec2Data(target.x, target.y),
        entities = entities,
        ignoreBuildingId = target.id,
      )
    entities[idx] =
      builder.copy(
        buildTaskBuildingId = buildingId,
        waypoints = path,
        attackMoveTarget = null,
        explicitTargetId = null,
      )
  }

  fun queueUnitProduction(ownerId: Int, buildingId: Int, unitType: UnitType): Boolean {
    val pIdx = players.indexOfFirst { it.id == ownerId }
    val bIdx = entities.indexOfFirst { it.id == buildingId && it.ownerId == ownerId }
    if (pIdx < 0 || bIdx < 0) return false
    val building = entities[bIdx]
    if (building.isUnderConstruction || building.productionQueue.size >= 5) return false
    val player = players[pIdx]
    if (unitType !in building.type.producibleUnits(player.techLevel)) return false
    if (player.credits < unitType.cost) return false

    players[pIdx] = player.copy(credits = player.credits - unitType.cost)
    val newQueue =
      building.productionQueue +
        ProductionOrder(
          unitType = unitType,
          remainingSeconds = unitType.buildTimeSeconds,
          totalSeconds = unitType.buildTimeSeconds,
        )
    entities[bIdx] = building.copy(productionQueue = newQueue)
    if (ownerId == humanPlayerId) audioEngine?.play(SoundEffect.UI_CLICK)
    return true
  }

  fun issueMoveOrder(unitIds: List<Int>, dest: Vec2Data) {
    val cols = kotlin.math.ceil(kotlin.math.sqrt(unitIds.size.toDouble())).toInt().coerceAtLeast(1)
    unitIds.forEachIndexed { orderIdx, uid ->
      val idx = entities.indexOfFirst { it.id == uid }
      if (idx < 0) return@forEachIndexed
      val u = entities[idx]
      if (u.type.isBuilding) {
        if (u.type.canProduce) {
          entities[idx] = u.copy(rallyPoint = dest)
        }
        return@forEachIndexed
      }
      val row = orderIdx / cols
      val col = orderIdx % cols
      val spread = 32f
      val ox = (col - (cols - 1) / 2f) * spread
      val oy = (row - (cols - 1) / 2f) * spread
      val goal =
        Vec2Data(
          (dest.x + ox).coerceIn(24f, map.worldWidth - 24f),
          (dest.y + oy).coerceIn(24f, map.worldHeight - 24f),
        )
      val path =
        Pathfinder.findPath(
          map = map,
          startWorld = Vec2Data(u.x, u.y),
          goalWorld = goal,
          entities = entities,
          isAirUnit = u.type.domain == UnitDomain.AIR,
        )
      entities[idx] =
        u.copy(
          waypoints = path,
          targetUnitId = null,
          explicitTargetId = null,
          attackMoveTarget = null,
          buildTaskBuildingId = null,
        )
    }
  }

  fun issueAttackMove(unitIds: List<Int>, dest: Vec2Data) {
    issueMoveOrder(unitIds, dest)
    for (uid in unitIds) {
      val idx = entities.indexOfFirst { it.id == uid }
      if (idx >= 0 && !entities[idx].type.isBuilding) {
        entities[idx] = entities[idx].copy(attackMoveTarget = dest)
      }
    }
  }

  fun issueAttackUnit(unitIds: List<Int>, targetId: Int) {
    val target = entities.firstOrNull { it.id == targetId } ?: return
    for (uid in unitIds) {
      val idx = entities.indexOfFirst { it.id == uid }
      if (idx < 0) continue
      val u = entities[idx]
      if (u.type.isBuilding) {
        entities[idx] = u.copy(targetUnitId = targetId, explicitTargetId = targetId)
      } else {
        val path =
          Pathfinder.findPath(
            map = map,
            startWorld = Vec2Data(u.x, u.y),
            goalWorld = Vec2Data(target.x, target.y),
            entities = entities,
            ignoreBuildingId = target.id,
            isAirUnit = u.type.domain == UnitDomain.AIR,
          )
        entities[idx] =
          u.copy(
            targetUnitId = targetId,
            explicitTargetId = targetId,
            chaseRepathTimer = 0.55f,
            waypoints = path,
            attackMoveTarget = null,
            buildTaskBuildingId = null,
          )
      }
    }
  }

  fun stopUnits(unitIds: List<Int>) {
    for (uid in unitIds) {
      val idx = entities.indexOfFirst { it.id == uid }
      if (idx >= 0) {
        entities[idx] =
          entities[idx].copy(
            waypoints = emptyList(),
            attackMoveTarget = null,
            explicitTargetId = null,
            buildTaskBuildingId = null,
          )
      }
    }
  }

  fun toSaveData(
    saveId: String,
    saveName: String,
    isAutosave: Boolean,
    cameraX: Float,
    cameraY: Float,
    cameraZoom: Float,
  ): GameSaveData {
    return GameSaveData(
      saveId = saveId,
      saveName = saveName,
      isAutosave = isAutosave,
      savedAtMillis = System.currentTimeMillis(),
      mapId = map.mapId,
      mapSeed = mapSeed,
      mapWidthTiles = map.widthTiles,
      mapHeightTiles = map.heightTiles,
      elapsedSeconds = elapsedSeconds,
      nextEntityId = nextEntityId,
      options = options,
      players = players.toList(),
      entities = entities.toList(),
      projectiles = projectiles.toList(),
      particles = particles.toList(),
      clouds = clouds.toList(),
      controlGroups = controlGroups.toMap(),
      exploredTiles = fogGrid.toList(),
      cameraX = cameraX,
      cameraY = cameraY,
      cameraZoom = cameraZoom,
    )
  }

  companion object {
    fun createNew(
      mapId: MapId,
      lobbyPlayers: List<LobbyPlayer>,
      options: GameOptions,
      settings: GameSettings,
      audioEngine: GameAudioEngine?,
    ): GameSimulation {
      val seed = mapId.ordinal * 1337L + 42L
      val map = MapGenerator.generate(mapId, seed)
      val players =
        lobbyPlayers.map { lp ->
          PlayerMatchState(
            id = lp.id,
            name = lp.name,
            isHuman = lp.isHuman,
            spawnPoint = lp.spawnPoint,
            team = lp.team,
            color = lp.color,
            aiDifficulty = if (lp.isHuman) options.aiDifficulty else lp.aiDifficulty,
            credits = options.startingCredits.toFloat(),
            incomePerSec = 10 * options.incomeMultiplier,
            techLevel = 1,
          )
        }
      return GameSimulation(map, options, players, seed).apply {
        this.settings = settings
        this.audioEngine = audioEngine
        populateNewMatch()
      }
    }

    fun fromSaveData(
      save: GameSaveData,
      settings: GameSettings,
      audioEngine: GameAudioEngine?,
    ): GameSimulation {
      val map = MapGenerator.generate(save.mapId, save.mapSeed)
      return GameSimulation(map, save.options, save.players, save.mapSeed).apply {
        this.settings = settings
        this.audioEngine = audioEngine
        this.elapsedSeconds = save.elapsedSeconds
        this.nextEntityId = save.nextEntityId
        this.nextProjectileId = (save.projectiles.maxOfOrNull { it.id } ?: 0) + 1
        this.entities.addAll(save.entities)
        this.projectiles.addAll(save.projectiles)
        this.particles.addAll(save.particles)
        if (save.clouds.isNotEmpty()) {
          this.clouds.clear()
          this.clouds.addAll(save.clouds)
        }
        save.controlGroups.forEach { (k, v) -> this.controlGroups[k] = v }
        val limit = min(this.fogGrid.size, save.exploredTiles.size)
        for (i in 0 until limit) {
          this.fogGrid[i] = save.exploredTiles[i]
        }
        updateFogOfWar()
      }
    }

    private fun rotateTowards(current: Float, target: Float, maxDelta: Float): Float {
      var diff = (target - current) % (2f * PI.toFloat())
      if (diff > PI) diff -= 2f * PI.toFloat()
      if (diff < -PI) diff += 2f * PI.toFloat()
      return if (kotlin.math.abs(diff) <= maxDelta) target else current + kotlin.math.sign(diff) * maxDelta
    }
  }

  private fun incrementPlayerUnitsBuilt(ownerId: Int) {
    val idx = players.indexOfFirst { it.id == ownerId }
    if (idx >= 0) players[idx] = players[idx].copy(unitsBuilt = players[idx].unitsBuilt + 1)
  }

  private fun incrementPlayerUnitsLost(ownerId: Int) {
    val idx = players.indexOfFirst { it.id == ownerId }
    if (idx >= 0) players[idx] = players[idx].copy(unitsLost = players[idx].unitsLost + 1)
  }

  private fun incrementEnemiesDestroyed(ownerId: Int) {
    val idx = players.indexOfFirst { it.id == ownerId }
    if (idx >= 0) players[idx] = players[idx].copy(enemiesDestroyed = players[idx].enemiesDestroyed + 1)
  }
}
