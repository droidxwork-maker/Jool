package com.example.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.engine.GameMap
import com.example.model.MapId
import com.example.model.TileType
import kotlin.math.sin

class MapTerrainCache(map: GameMap) {
  val terrainBitmap: ImageBitmap
  val minimapBaseBitmap: ImageBitmap

  init {
    terrainBitmap = buildTerrainBitmap(map).asImageBitmap()
    minimapBaseBitmap = buildMinimapBitmap(map).asImageBitmap()
  }

  private fun buildTerrainBitmap(map: GameMap): Bitmap {
    val px = 32 // Higher resolution texture tiles for maximum visual fidelity
    val bmpW = map.widthTiles * px
    val bmpH = map.heightTiles * px
    val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint().apply { isAntiAlias = true }

    // Pass 1: High fidelity procedural terrain textures & shading
    for (ty in 0 until map.heightTiles) {
      for (tx in 0 until map.widthTiles) {
        val tile = map.tileAt(tx, ty)
        val v = map.tileVariation[ty * map.widthTiles + tx]
        val v2 = ((sin(tx * 1.6 + ty * 2.2) * 0.5 + 0.5)).toFloat()
        val left = (tx * px).toFloat()
        val top = (ty * px).toFloat()

        val nUp = map.tileAt(tx, ty - 1)
        val nDown = map.tileAt(tx, ty + 1)
        val nLeft = map.tileAt(tx - 1, ty)
        val nRight = map.tileAt(tx + 1, ty)

        when (tile) {
          TileType.GRASS -> {
            val r = (42 + v * 18 + v2 * 12).toInt().coerceIn(0, 255)
            val g = (98 + v * 26 + v2 * 16).toInt().coerceIn(0, 255)
            val b = (36 + v * 12).toInt().coerceIn(0, 255)
            paint.color = android.graphics.Color.rgb(r, g, b)
            canvas.drawRect(left, top, left + px, top + px, paint)

            // Rich mossy biome patches
            if (v > 0.4f) {
              paint.color = android.graphics.Color.argb(45, 24, 58, 20)
              canvas.drawCircle(left + 8f + v2 * 12f, top + 8f + v * 12f, 10f, paint)
            }
            // Detailed grass blades
            paint.color = android.graphics.Color.argb(85, 128, 196, 76)
            val gx = left + 6f + ((v * 19f).toInt() % 20)
            val gy = top + 6f + ((v2 * 17f).toInt() % 18)
            canvas.drawRect(gx, gy, gx + 2f, gy + 6f, paint)
            canvas.drawRect(gx + 4f, gy + 2f, gx + 6f, gy + 7f, paint)

            // Small tactical stones / wildflowers
            if (v > 0.8f) {
              paint.color = android.graphics.Color.argb(110, 140, 150, 130)
              canvas.drawCircle(left + 22f, top + 22f, 2.5f, paint)
            } else if (v < 0.2f) {
              paint.color = android.graphics.Color.argb(120, 220, 210, 100)
              canvas.drawCircle(left + 10f, top + 18f, 2f, paint)
            }
          }

          TileType.SAND -> {
            val isBridge =
              map.mapId == MapId.FIRE_BRIDGE &&
                (nLeft == TileType.WATER || nRight == TileType.WATER || nUp == TileType.WATER || nDown == TileType.WATER)
            if (isBridge) {
              // High-tech reinforced tactical steel bridge
              paint.color = android.graphics.Color.rgb(68, 76, 85)
              canvas.drawRect(left, top, left + px, top + px, paint)
              paint.color = android.graphics.Color.rgb(48, 54, 62)
              canvas.drawRect(left + 2f, top + 2f, left + px - 2f, top + px - 2f, paint)
              // Steel deck plating ribs & safety yellow border
              paint.color = android.graphics.Color.rgb(90, 100, 110)
              for (rib in 4 until px step 8) {
                canvas.drawRect(left + 3f, top + rib, left + px - 3f, top + rib + 3f, paint)
              }
              if (nLeft == TileType.WATER) {
                paint.color = android.graphics.Color.rgb(245, 180, 20)
                canvas.drawRect(left, top, left + 4f, top + px, paint)
              }
              if (nRight == TileType.WATER) {
                paint.color = android.graphics.Color.rgb(245, 180, 20)
                canvas.drawRect(left + px - 4f, top, left + px, top + px, paint)
              }
            } else {
              val r = (182 + v * 20).toInt().coerceIn(0, 255)
              val g = (158 + v * 18).toInt().coerceIn(0, 255)
              val b = (108 + v * 16).toInt().coerceIn(0, 255)
              paint.color = android.graphics.Color.rgb(r, g, b)
              canvas.drawRect(left, top, left + px, top + px, paint)
              paint.color = android.graphics.Color.argb(55, 134, 110, 68)
              canvas.drawCircle(left + 10f, top + 22f, 3f, paint)
              canvas.drawCircle(left + 24f, top + 10f, 2.5f, paint)
            }
          }

          TileType.ICE -> {
            val r = (190 + v * 24).toInt().coerceIn(0, 255)
            val g = (220 + v * 22).toInt().coerceIn(0, 255)
            val b = (240 + v * 15).toInt().coerceIn(0, 255)
            paint.color = android.graphics.Color.rgb(r, g, b)
            canvas.drawRect(left, top, left + px, top + px, paint)
            paint.color = android.graphics.Color.argb(110, 250, 255, 255)
            canvas.drawLine(left + 4f, top + 24f, left + 24f, top + 8f, paint)
            paint.color = android.graphics.Color.argb(65, 135, 185, 220)
            canvas.drawLine(left + 8f, top + 27f, left + 27f, top + 12f, paint)
          }

          TileType.WATER -> {
            val shallow =
              nUp != TileType.WATER || nDown != TileType.WATER || nLeft != TileType.WATER || nRight != TileType.WATER
            val baseR = if (shallow) 24 else 14
            val baseG = if (shallow) (118 + (v * 16).toInt()) else (74 + (v * 16).toInt())
            val baseB = if (shallow) (178 + (v * 18).toInt()) else (148 + (v * 20).toInt())
            paint.color = android.graphics.Color.rgb(baseR, baseG, baseB)
            canvas.drawRect(left, top, left + px, top + px, paint)

            // Radiant coastal surf / foam highlights
            paint.color = android.graphics.Color.argb(130, 175, 235, 255)
            if (nUp != TileType.WATER) canvas.drawRect(left, top, left + px, top + 4f, paint)
            if (nLeft != TileType.WATER) canvas.drawRect(left, top, left + 4f, top + px, paint)
            if (nDown != TileType.WATER) {
              paint.color = android.graphics.Color.argb(80, 150, 215, 245)
              canvas.drawRect(left, top + px - 3f, left + px, top + px, paint)
            }
            if (nRight != TileType.WATER) {
              paint.color = android.graphics.Color.argb(80, 150, 215, 245)
              canvas.drawRect(left + px - 3f, top, left + px, top + px, paint)
            }
          }

          TileType.ROCK -> {
            val c = (68 + v * 22).toInt().coerceIn(0, 255)
            paint.color = android.graphics.Color.rgb(c, c + 6, c + 12)
            canvas.drawRect(left, top, left + px, top + px, paint)
            // 3D Bevel & Mountain Ridge Shading
            paint.color = android.graphics.Color.argb(110, 210, 222, 235)
            canvas.drawRect(left, top, left + px, top + 4f, paint)
            canvas.drawRect(left, top, left + 4f, top + px, paint)
            paint.color = android.graphics.Color.argb(150, 14, 18, 24)
            canvas.drawRect(left, top + px - 5f, left + px, top + px, paint)
            canvas.drawRect(left + px - 5f, top, left + px, top + px, paint)
            // High-detail rock fracture line
            paint.color = android.graphics.Color.argb(120, 20, 26, 32)
            canvas.drawLine(left + 6f, top + 8f, left + 22f, top + 20f, paint)
          }

          TileType.LAVA_ROCK -> {
            val r = (68 + v * 26).toInt().coerceIn(0, 255)
            val g = (34 + v * 12).toInt().coerceIn(0, 255)
            paint.color = android.graphics.Color.rgb(r, g, 28)
            canvas.drawRect(left, top, left + px, top + px, paint)
            // Bevel
            paint.color = android.graphics.Color.argb(130, 18, 10, 10)
            canvas.drawRect(left, top + px - 4f, left + px, top + px, paint)
            canvas.drawRect(left + px - 4f, top, left + px, top + px, paint)
            // Glowing molten magma fissure
            if (v > 0.3f) {
              paint.color = android.graphics.Color.rgb(255, 84, 10)
              paint.strokeWidth = 3f
              canvas.drawLine(left + 5f, top + 12f, left + 20f, top + 18f, paint)
              paint.color = android.graphics.Color.rgb(255, 218, 55)
              paint.strokeWidth = 1.4f
              canvas.drawLine(left + 8f, top + 13f, left + 17f, top + 16f, paint)
            }
          }

          TileType.RESOURCE_POOL -> {
            val bgCol =
              when (map.mapId) {
                MapId.ICE_ISLAND -> android.graphics.Color.rgb(180, 212, 232)
                MapId.FIRE_BRIDGE -> android.graphics.Color.rgb(86, 78, 64)
                else -> android.graphics.Color.rgb(52, 90, 42)
              }
            paint.color = bgCol
            canvas.drawRect(left, top, left + px, top + px, paint)

            val cx = left + px / 2f
            val cy = top + px / 2f
            // Deep industrial oil/ore well with metallic rim
            paint.color = android.graphics.Color.argb(160, 8, 12, 16)
            canvas.drawCircle(cx + 2f, cy + 2f, 15f, paint)
            paint.color = android.graphics.Color.rgb(82, 94, 104)
            canvas.drawCircle(cx, cy, 14f, paint)
            paint.color = android.graphics.Color.rgb(44, 52, 58)
            canvas.drawCircle(cx, cy, 12f, paint)
            paint.color = android.graphics.Color.rgb(10, 16, 22)
            canvas.drawCircle(cx, cy, 10f, paint)
            paint.color = android.graphics.Color.rgb(24, 124, 142)
            canvas.drawCircle(cx - 2f, cy - 2f, 5f, paint)
            paint.color = android.graphics.Color.rgb(115, 235, 220)
            canvas.drawCircle(cx - 3.5f, cy - 3.5f, 1.8f, paint)
          }
        }
      }
    }

    // Pass 2: High fidelity ambient shadow casting from rocks
    paint.color = android.graphics.Color.argb(85, 6, 10, 14)
    for (ty in 0 until map.heightTiles) {
      for (tx in 0 until map.widthTiles) {
        val t = map.tileAt(tx, ty)
        if (t == TileType.ROCK || t == TileType.LAVA_ROCK) {
          val right = map.tileAt(tx + 1, ty)
          val down = map.tileAt(tx, ty + 1)
          val left = (tx * px).toFloat()
          val top = (ty * px).toFloat()
          if (right != TileType.ROCK && right != TileType.LAVA_ROCK) {
            canvas.drawRect(left + px, top + 4f, left + px + 6f, top + px + 4f, paint)
          }
          if (down != TileType.ROCK && down != TileType.LAVA_ROCK) {
            canvas.drawRect(left + 4f, top + px, left + px + 4f, top + px + 6f, paint)
          }
        }
      }
    }

    return bmp
  }

  private fun buildMinimapBitmap(map: GameMap): Bitmap {
    val bmp = Bitmap.createBitmap(map.widthTiles, map.heightTiles, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(map.widthTiles * map.heightTiles)
    for (i in pixels.indices) {
      pixels[i] =
        when (map.tiles[i]) {
          TileType.GRASS -> 0xFF366224.toInt()
          TileType.SAND -> 0xFF9E8454.toInt()
          TileType.ICE -> 0xFFACD0E2.toInt()
          TileType.WATER -> 0xFF14528C.toInt()
          TileType.ROCK -> 0xFF4E545C.toInt()
          TileType.LAVA_ROCK -> 0xFF542820.toInt()
          TileType.RESOURCE_POOL -> 0xFF0A222A.toInt()
        }
    }
    bmp.setPixels(pixels, 0, map.widthTiles, 0, 0, map.widthTiles, map.heightTiles)
    return bmp
  }
}
