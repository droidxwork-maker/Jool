package com.example.engine

import com.example.model.EntityState
import com.example.model.Vec2Data
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object Pathfinder {
  private data class Node(val idx: Int, val fScore: Float) : Comparable<Node> {
    override fun compareTo(other: Node): Int = fScore.compareTo(other.fScore)
  }

  private val dx = intArrayOf(1, -1, 0, 0, 1, 1, -1, -1)
  private val dy = intArrayOf(0, 0, 1, -1, 1, -1, 1, -1)
  private val stepCost = floatArrayOf(1f, 1f, 1f, 1f, 1.414f, 1.414f, 1.414f, 1.414f)

  fun findPath(
    map: GameMap,
    startWorld: Vec2Data,
    goalWorld: Vec2Data,
    entities: List<EntityState>,
    ignoreBuildingId: Int? = null,
    isAirUnit: Boolean = false,
  ): List<Vec2Data> {
    if (isAirUnit) {
      return listOf(
        Vec2Data(
          goalWorld.x.coerceIn(24f, map.worldWidth - 24f),
          goalWorld.y.coerceIn(24f, map.worldHeight - 24f),
        )
      )
    }

    val w = map.widthTiles
    val h = map.heightTiles
    val total = w * h

    // Build dynamic blocked grid (terrain + buildings)
    val blocked = BooleanArray(total)
    for (i in 0 until total) {
      if (!map.tiles[i].isWalkableGround) blocked[i] = true
    }
    for (e in entities) {
      if (e.type.isBuilding && e.id != ignoreBuildingId) {
        val btx = map.worldToTileX(e.x)
        val bty = map.worldToTileY(e.y)
        val span = if (e.type.radius > 25f) 1 else 0
        for (oy in -span..span) {
          for (ox in -span..span) {
            val nx = btx + ox
            val ny = bty + oy
            if (nx in 0 until w && ny in 0 until h) {
              blocked[ny * w + nx] = true
            }
          }
        }
      }
    }

    val sx = map.worldToTileX(startWorld.x)
    val sy = map.worldToTileY(startWorld.y)
    var gx = map.worldToTileX(goalWorld.x)
    var gy = map.worldToTileY(goalWorld.y)

    // Allow starting tile even if unit overlaps a building edge
    val startIdx = sy * w + sx
    blocked[startIdx] = false

    // If goal tile is blocked, find closest walkable tile near goal
    if (blocked[gy * w + gx]) {
      var found = false
      var bestDist = Int.MAX_VALUE
      var bestX = gx
      var bestY = gy
      for (r in 1..6) {
        for (oy in -r..r) {
          for (ox in -r..r) {
            val nx = gx + ox
            val ny = gy + oy
            if (nx in 0 until w && ny in 0 until h && !blocked[ny * w + nx]) {
              val d = ox * ox + oy * oy
              if (d < bestDist) {
                bestDist = d
                bestX = nx
                bestY = ny
                found = true
              }
            }
          }
        }
        if (found) break
      }
      if (!found) return emptyList()
      gx = bestX
      gy = bestY
    }

    val goalIdx = gy * w + gx
    if (startIdx == goalIdx) {
      return listOf(goalWorld)
    }

    val gScore = FloatArray(total) { Float.POSITIVE_INFINITY }
    val cameFrom = IntArray(total) { -1 }
    val closed = BooleanArray(total)
    val open = PriorityQueue<Node>()

    fun heuristic(x: Int, y: Int): Float {
      val adx = abs(x - gx).toFloat()
      val ady = abs(y - gy).toFloat()
      return max(adx, ady) + 0.414f * min(adx, ady)
    }

    gScore[startIdx] = 0f
    open.add(Node(startIdx, heuristic(sx, sy)))

    var reached = false
    var iterations = 0
    val maxIterations = 8000

    while (open.isNotEmpty() && iterations < maxIterations) {
      iterations++
      val current = open.poll() ?: break
      val cIdx = current.idx
      if (cIdx == goalIdx) {
        reached = true
        break
      }
      if (closed[cIdx]) continue
      closed[cIdx] = true

      val cx = cIdx % w
      val cy = cIdx / w

      for (dir in 0 until 8) {
        val nx = cx + dx[dir]
        val ny = cy + dy[dir]
        if (nx !in 0 until w || ny !in 0 until h) continue
        val nIdx = ny * w + nx
        if (blocked[nIdx] || closed[nIdx]) continue

        // Prevent corner cutting on diagonal moves
        if (dir >= 4) {
          if (blocked[cy * w + nx] || blocked[ny * w + cx]) continue
        }

        val tentativeG = gScore[cIdx] + stepCost[dir]
        if (tentativeG < gScore[nIdx]) {
          cameFrom[nIdx] = cIdx
          gScore[nIdx] = tentativeG
          open.add(Node(nIdx, tentativeG + heuristic(nx, ny)))
        }
      }
    }

    if (!reached) {
      // Fallback direct clamped point if unreachable island
      return emptyList()
    }

    // Reconstruct tile path
    val rawTiles = ArrayList<Pair<Int, Int>>(64)
    var cur = goalIdx
    while (cur != -1 && cur != startIdx) {
      rawTiles.add(Pair(cur % w, cur / w))
      cur = cameFrom[cur]
    }
    rawTiles.reverse()
    if (rawTiles.isEmpty()) return listOf(goalWorld)

    // Smooth collinear waypoints
    val smoothed = ArrayList<Vec2Data>(rawTiles.size)
    var prevDx = Int.MAX_VALUE
    var prevDy = Int.MAX_VALUE
    var prevTx = sx
    var prevTy = sy
    for (i in rawTiles.indices) {
      val (tx, ty) = rawTiles[i]
      val stepDx = tx - prevTx
      val stepDy = ty - prevTy
      if (i > 0 && (stepDx != prevDx || stepDy != prevDy)) {
        smoothed.add(map.tileCenterWorld(prevTx, prevTy))
      }
      prevDx = stepDx
      prevDy = stepDy
      prevTx = tx
      prevTy = ty
    }
    // Final destination snaps accurately if inside walkable goal tile
    if (map.worldToTileX(goalWorld.x) == gx && map.worldToTileY(goalWorld.y) == gy) {
      smoothed.add(goalWorld)
    } else {
      smoothed.add(map.tileCenterWorld(gx, gy))
    }
    return smoothed
  }
}
