package com.example.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.FogMode

class FogOverlayCache(val widthTiles: Int, val heightTiles: Int) {
  val tileSize = 32
  val widthPx = widthTiles * tileSize
  val heightPx = heightTiles * tileSize
  private val bitmap: Bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
  private val canvas: Canvas = Canvas(bitmap)
  private val paint = Paint().apply { isAntiAlias = false }
  private var lastVersion = -1

  init {
    canvas.drawColor(Color.BLACK)
  }

  fun sync(fogGrid: Any, fogVersion: Int, fogMode: FogMode): ImageBitmap {
    if (fogVersion == lastVersion) return bitmap.asImageBitmap()
    lastVersion = fogVersion

    canvas.drawColor(Color.BLACK)
    for (y in 0 until heightTiles) {
      for (x in 0 until widthTiles) {
        val idx = y * widthTiles + x
        val f = when (fogGrid) {
          is IntArray -> if (idx < fogGrid.size) fogGrid[idx] else 2
          is ByteArray -> if (idx < fogGrid.size) fogGrid[idx].toInt() else 2
          is List<*> -> if (idx < fogGrid.size) (fogGrid[idx] as? Number)?.toInt() ?: 2 else 2
          else -> 2
        }
        val left = (x * tileSize).toFloat()
        val top = (y * tileSize).toFloat()

        when (f) {
          2 -> {
            paint.color = Color.TRANSPARENT
            canvas.drawRect(left, top, left + tileSize, top + tileSize, paint)
          }
          1 -> {
            paint.color = Color.argb(135, 6, 10, 16)
            canvas.drawRect(left, top, left + tileSize, top + tileSize, paint)
          }
          else -> {
            paint.color = Color.BLACK
            canvas.drawRect(left, top, left + tileSize, top + tileSize, paint)
          }
        }
      }
    }
    return bitmap.asImageBitmap()
  }
}
