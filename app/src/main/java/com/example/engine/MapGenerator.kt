package com.example.engine

import com.example.model.MapId
import com.example.model.TileType
import com.example.model.Vec2Data
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class GameMap(
  val mapId: MapId,
  val widthTiles: Int,
  val heightTiles: Int,
  val tileSize: Float,
  val tiles: Array<TileType>,
  val tileVariation: FloatArray,
  val spawnPoints: List<Vec2Data>,
  val resourcePools: List<Vec2Data>,
) {
  val worldWidth: Float
    get() = widthTiles * tileSize

  val worldHeight: Float
    get() = heightTiles * tileSize

  fun tileAt(tx: Int, ty: Int): TileType {
    if (tx !in 0 until widthTiles || ty !in 0 until heightTiles) return TileType.ROCK
    return tiles[ty * widthTiles + tx]
  }

  fun worldToTileX(wx: Float): Int = (wx / tileSize).toInt().coerceIn(0, widthTiles - 1)

  fun worldToTileY(wy: Float): Int = (wy / tileSize).toInt().coerceIn(0, heightTiles - 1)

  fun tileCenterWorld(tx: Int, ty: Int): Vec2Data =
    Vec2Data((tx + 0.5f) * tileSize, (ty + 0.5f) * tileSize)
}

object MapGenerator {
  const val DEFAULT_WIDTH = 64
  const val DEFAULT_HEIGHT = 64
  const val TILE_SIZE = 48f

  fun generate(mapId: MapId, seed: Long = mapId.ordinal * 1337L + 42L): GameMap {
    val w = DEFAULT_WIDTH
    val h = DEFAULT_HEIGHT
    val tiles = Array(w * h) { TileType.GRASS }
    val variation = FloatArray(w * h)
    val rng = Random(seed)

    // Smooth deterministic value noise helper
    fun noise(x: Int, y: Int, scale: Float): Float {
      val nx = x / scale
      val ny = y / scale
      val v1 = sin(nx * 1.7 + ny * 0.9 + seed * 0.13)
      val v2 = cos(nx * 0.8 - ny * 1.9 + seed * 0.27)
      val v3 = sin((nx + ny) * 1.3 + seed * 0.07)
      return ((v1 + v2 + v3) / 3.0 * 0.5 + 0.5).toFloat()
    }

    for (y in 0 until h) {
      for (x in 0 until w) {
        val idx = y * w + x
        variation[idx] = rng.nextFloat()
        val dx = (x - w / 2f) / (w / 2f)
        val dy = (y - h / 2f) / (h / 2f)
        val radial = hypot(dx, dy)
        val n1 = noise(x, y, 6.5f)
        val n2 = noise(x, y, 2.8f)
        val n = n1 * 0.7f + n2 * 0.3f

        tiles[idx] =
          when (mapId) {
            MapId.BIG_ISLAND -> {
              val coast = radial + (n - 0.5f) * 0.25f
              when {
                coast > 0.90f -> TileType.WATER
                coast > 0.82f -> TileType.SAND
                n > 0.79f && radial < 0.65f -> TileType.ROCK
                else -> TileType.GRASS
              }
            }
            MapId.FIRE_BRIDGE -> {
              // Central horizontal river / chasm with 3 tactical land bridges
              val inRiver =
                abs(y - h / 2f + sin(x * 0.18f) * 2.5f) < 5.5f
              val onBridge =
                abs(x - 14) <= 2 || abs(x - w / 2) <= 3 || abs(x - (w - 15)) <= 2
              when {
                inRiver && !onBridge -> TileType.WATER
                inRiver && onBridge -> TileType.SAND
                n > 0.78f -> TileType.LAVA_ROCK
                n > 0.68f -> TileType.SAND
                else -> TileType.GRASS
              }
            }
            MapId.HILLS -> {
              val ridge =
                abs(sin(x * 0.14f + y * 0.08f)) * 0.45f + n * 0.55f
              when {
                ridge > 0.76f -> TileType.ROCK
                n < 0.14f && radial < 0.35f -> TileType.WATER
                ridge > 0.62f -> TileType.SAND
                else -> TileType.GRASS
              }
            }
            MapId.LAKE -> {
              val lakeDist = radial + (n - 0.5f) * 0.18f
              when {
                lakeDist < 0.32f -> TileType.WATER
                lakeDist < 0.39f -> TileType.SAND
                n > 0.81f && lakeDist > 0.48f -> TileType.ROCK
                else -> TileType.GRASS
              }
            }
            MapId.ICE_ISLAND -> {
              val island = radial + (n - 0.5f) * 0.24f
              when {
                island > 0.91f -> TileType.WATER
                n > 0.78f && island < 0.70f -> TileType.ROCK
                else -> TileType.ICE
              }
            }
          }
      }
    }

    // 4 canonical spawn points (corners / quadrants well inside map)
    val spawnCoords =
      listOf(
        Pair(10, 10),
        Pair(w - 11, h - 11),
        Pair(w - 11, 10),
        Pair(10, h - 11),
      )

    val baseTile = if (mapId == MapId.ICE_ISLAND) TileType.ICE else TileType.GRASS

    // Carve clean buildable plateaus around each spawn point & place local resource pools
    val poolCoords = mutableListOf<Pair<Int, Int>>()
    spawnCoords.forEachIndexed { idx, (sx, sy) ->
      for (dy in -5..5) {
        for (dx in -5..5) {
          val tx = (sx + dx).coerceIn(1, w - 2)
          val ty = (sy + dy).coerceIn(1, h - 2)
          if (hypot(dx.toFloat(), dy.toFloat()) <= 5.4f) {
            tiles[ty * w + tx] = baseTile
          }
        }
      }
      // Two safe home resource pools near each spawn
      val dirX = if (sx < w / 2) 1 else -1
      val dirY = if (sy < h / 2) 1 else -1
      poolCoords.add(Pair(sx + dirX * 4, sy - dirY * 2))
      poolCoords.add(Pair(sx - dirX * 2, sy + dirY * 4))
      // Forward expansion pool
      poolCoords.add(Pair(sx + dirX * 10, sy + dirY * 4))
      poolCoords.add(Pair(sx + dirX * 4, sy + dirY * 10))
    }

    // Contested mid-map resource pools
    val midCandidates =
      when (mapId) {
        MapId.LAKE ->
          listOf(
            Pair(w / 2 - 14, h / 2),
            Pair(w / 2 + 14, h / 2),
            Pair(w / 2, h / 2 - 14),
            Pair(w / 2, h / 2 + 14),
          )
        MapId.FIRE_BRIDGE ->
          listOf(
            Pair(w / 2 - 18, h / 2 - 10),
            Pair(w / 2 + 18, h / 2 + 10),
            Pair(w / 2, h / 2 - 11),
            Pair(w / 2, h / 2 + 11),
          )
        else ->
          listOf(
            Pair(w / 2 - 4, h / 2 - 4),
            Pair(w / 2 + 4, h / 2 + 4),
            Pair(w / 2 + 4, h / 2 - 4),
            Pair(w / 2 - 4, h / 2 + 4),
          )
      }
    poolCoords.addAll(midCandidates)

    // Ensure roads/corridors exist between spawn pairs on Hills/Big Island so AI & players can reach
    if (mapId == MapId.HILLS) {
      carvePath(tiles, w, h, spawnCoords[0], spawnCoords[1], baseTile)
      carvePath(tiles, w, h, spawnCoords[2], spawnCoords[3], baseTile)
      carvePath(tiles, w, h, spawnCoords[0], spawnCoords[2], baseTile)
      carvePath(tiles, w, h, spawnCoords[1], spawnCoords[3], baseTile)
    }

    val resourceWorlds = mutableListOf<Vec2Data>()
    val distinctPools = poolCoords.distinct()
    for ((px, py) in distinctPools) {
      val cx = px.coerceIn(2, w - 3)
      val cy = py.coerceIn(2, h - 3)
      // Make immediate neighbors walkable
      for (dy in -1..1) {
        for (dx in -1..1) {
          val ntx = cx + dx
          val nty = cy + dy
          val cur = tiles[nty * w + ntx]
          if (cur == TileType.ROCK || cur == TileType.LAVA_ROCK || cur == TileType.WATER) {
            tiles[nty * w + ntx] = baseTile
          }
        }
      }
      tiles[cy * w + cx] = TileType.RESOURCE_POOL
      resourceWorlds.add(Vec2Data((cx + 0.5f) * TILE_SIZE, (cy + 0.5f) * TILE_SIZE))
    }

    val spawnWorlds =
      spawnCoords.map { (sx, sy) ->
        Vec2Data((sx + 0.5f) * TILE_SIZE, (sy + 0.5f) * TILE_SIZE)
      }

    return GameMap(
      mapId = mapId,
      widthTiles = w,
      heightTiles = h,
      tileSize = TILE_SIZE,
      tiles = tiles,
      tileVariation = variation,
      spawnPoints = spawnWorlds,
      resourcePools = resourceWorlds,
    )
  }

  private fun carvePath(
    tiles: Array<TileType>,
    w: Int,
    h: Int,
    from: Pair<Int, Int>,
    to: Pair<Int, Int>,
    fill: TileType,
  ) {
    val steps = max(abs(to.first - from.first), abs(to.second - from.second)) * 2
    if (steps <= 0) return
    for (i in 0..steps) {
      val t = i.toFloat() / steps
      val cx = (from.first + (to.first - from.first) * t).toInt()
      val cy = (from.second + (to.second - from.second) * t).toInt()
      for (dy in -1..1) {
        for (dx in -1..1) {
          val tx = (cx + dx).coerceIn(1, w - 2)
          val ty = (cy + dy).coerceIn(1, h - 2)
          if (tiles[ty * w + tx] == TileType.ROCK || tiles[ty * w + tx] == TileType.WATER) {
            tiles[ty * w + tx] = fill
          }
        }
      }
    }
  }
}
