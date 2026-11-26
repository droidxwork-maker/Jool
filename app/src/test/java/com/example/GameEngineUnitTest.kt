package com.example

import com.example.engine.GameSimulation
import com.example.engine.MapGenerator
import com.example.engine.Pathfinder
import com.example.model.GameOptions
import com.example.model.GameSaveData
import com.example.model.GameSettings
import com.example.model.MapId
import com.example.model.UnitType
import com.example.model.Vec2Data
import com.example.viewmodel.GameViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineUnitTest {

  @Test
  fun `all 5 maps generate valid spawns and resource pools`() {
    for (mapId in MapId.entries) {
      val map = MapGenerator.generate(mapId)
      assertEquals(4, map.spawnPoints.size)
      assertTrue("Expected resource pools on $mapId", map.resourcePools.size >= 8)
      for (sp in map.spawnPoints) {
        val tx = map.worldToTileX(sp.x)
        val ty = map.worldToTileY(sp.y)
        assertTrue("Spawn point must be buildable on $mapId", map.tileAt(tx, ty).isBuildable)
      }
    }
  }

  @Test
  fun `a star pathfinder routes between spawn points on hills`() {
    val map = MapGenerator.generate(MapId.HILLS)
    val start = map.spawnPoints[0]
    val goal = map.spawnPoints[1]
    val path = Pathfinder.findPath(map, start, goal, emptyList(), isAirUnit = false)
    assertTrue("Expected valid A* path across Hills map", path.isNotEmpty())
  }

  @Test
  fun `builder constructs extractor and boosts income`() {
    val sim =
      GameSimulation.createNew(
        mapId = MapId.BIG_ISLAND,
        lobbyPlayers = GameViewModel.defaultLobbyPlayers(),
        options = GameOptions(startingCredits = 4000),
        settings = GameSettings(autosaveEnabled = false),
        audioEngine = null,
      )
    val builder = sim.entities.first { it.ownerId == sim.humanPlayerId && it.type == UnitType.BUILDER }
    val pool = sim.map.resourcePools.minBy { kotlin.math.hypot(it.x - builder.x, it.y - builder.y) }
    val bId = sim.placeBuildingForPlayer(sim.humanPlayerId, UnitType.EXTRACTOR, pool.x, pool.y, listOf(builder.id))
    assertNotNull(bId)

    // Step simulation long enough for builder to walk over and finish constructing extractor
    repeat(300) {
      sim.step(0.05f)
    }
    val extractor = sim.entities.firstOrNull { it.id == bId }
    assertNotNull(extractor)
    assertFalse("Extractor should finish construction", extractor!!.isUnderConstruction)
    val human = sim.playersById[sim.humanPlayerId]!!
    assertTrue("Income should increase after extractor completion", human.incomePerSec >= 28)
  }

  @Test
  fun `save game serialization roundtrip preserves state`() {
    val sim =
      GameSimulation.createNew(
        mapId = MapId.ICE_ISLAND,
        lobbyPlayers = GameViewModel.defaultLobbyPlayers(),
        options = GameOptions(),
        settings = GameSettings(),
        audioEngine = null,
      )
    repeat(20) { sim.step(0.05f) }
    val saveData = sim.toSaveData("test_save", "Test Save", false, 500f, 600f, 1.25f)

    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter(GameSaveData::class.java)
    val json = adapter.toJson(saveData)
    val restoredData = adapter.fromJson(json)
    assertNotNull(restoredData)

    val restoredSim = GameSimulation.fromSaveData(restoredData!!, GameSettings(), null)
    assertEquals(MapId.ICE_ISLAND, restoredSim.map.mapId)
    assertEquals(sim.entities.size, restoredSim.entities.size)
    assertEquals(sim.players.size, restoredSim.players.size)
  }

  @Test
  fun `tank ordered to attack distant enemy chases into range and fires`() {
    val sim =
      GameSimulation.createNew(
        mapId = MapId.BIG_ISLAND,
        lobbyPlayers = GameViewModel.defaultLobbyPlayers(),
        options = GameOptions(),
        settings = GameSettings(autosaveEnabled = false),
        audioEngine = null,
      )
    val start = sim.map.spawnPoints[0]
    val tank =
      com.example.model.EntityState(
        id = sim.nextEntityId++,
        ownerId = sim.humanPlayerId,
        type = UnitType.LIGHT_TANK,
        x = start.x + 120f,
        y = start.y + 120f,
        hp = UnitType.LIGHT_TANK.maxHp,
        maxHp = UnitType.LIGHT_TANK.maxHp,
      )
    sim.entities.add(tank)
    val enemyPlayerId = sim.players.first { it.id != sim.humanPlayerId }.id
    val enemyTank =
      com.example.model.EntityState(
        id = sim.nextEntityId++,
        ownerId = enemyPlayerId,
        type = UnitType.LIGHT_TANK,
        x = start.x + 180f,
        y = start.y + 180f,
        hp = UnitType.LIGHT_TANK.maxHp,
        maxHp = UnitType.LIGHT_TANK.maxHp,
      )
    sim.entities.add(enemyTank)
    val initialEnemyHp = enemyTank.hp

    sim.issueAttackUnit(listOf(tank.id), enemyTank.id)
    // Run 1 tick — tank must retain explicitTargetId even though enemy is slightly away
    sim.step(0.05f)
    val afterOneTick = sim.entities.first { it.id == tank.id }
    assertEquals(enemyTank.id, afterOneTick.explicitTargetId)

    // Step simulation until tank moves into range and damages enemy tank
    repeat(120) { sim.step(0.05f) }
    val enemyAfter = sim.entities.firstOrNull { it.id == enemyTank.id }
    assertTrue("Enemy should take damage or be destroyed", enemyAfter == null || enemyAfter.hp < initialEnemyHp)
  }
}
