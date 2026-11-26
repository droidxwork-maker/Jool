package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameSimulation
import com.example.model.FogMode
import com.example.model.LobbyPlayer
import com.example.model.UnitType
import com.example.render.MapTerrainCache
import com.example.render.ProceduralSprites.drawEntitySprite
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SteelDark
import com.example.ui.theme.SteelDarkest
import com.example.ui.theme.SteelHighlight
import com.example.ui.theme.SteelLight
import com.example.ui.theme.SteelMedium
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TacticalGreenBright
import com.example.ui.theme.TacticalGreenDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RivetedMetalBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(modifier = modifier.fillMaxSize().background(SteelDarkest)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFF171E24), Color(0xFF1E2830), Color(0xFF12181D)),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
          )
      )
      // Industrial metal plate grid seams
      val plateW = 240f
      val plateH = 160f
      var y = 0f
      while (y < size.height) {
        var x = 0f
        while (x < size.width) {
          drawRect(
            color = Color(0xFF28343E),
            topLeft = Offset(x + 2f, y + 2f),
            size = Size(plateW - 4f, plateH - 4f),
            style = Stroke(width = 1.5f),
          )
          // Corner rivets / bolts
          val rOff = 12f
          val rivets =
            listOf(
              Offset(x + rOff, y + rOff),
              Offset(x + plateW - rOff, y + rOff),
              Offset(x + rOff, y + plateH - rOff),
              Offset(x + plateW - rOff, y + plateH - rOff),
            )
          rivets.forEach { pt ->
            if (pt.x < size.width && pt.y < size.height) {
              drawCircle(color = Color(0xFF0E1317), radius = 3.5f, center = pt + Offset(1f, 1f))
              drawCircle(color = Color(0xFF455563), radius = 3.2f, center = pt)
              drawCircle(color = Color(0xFF6A7D8E), radius = 1.3f, center = pt - Offset(0.8f, 0.8f))
            }
          }
          x += plateW
        }
        y += plateH
      }
    }
    content()
  }
}

@Composable
fun MetalPanel(
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(12.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier = modifier,
    color = SteelDark.copy(alpha = 0.94f),
    shape = RoundedCornerShape(4.dp),
    border = BorderStroke(1.5.dp, SteelBorder),
    shadowElevation = 6.dp,
  ) {
    Column(modifier = Modifier.padding(contentPadding), content = content)
  }
}

@Composable
fun TacticalButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isPrimary: Boolean = false,
  isDanger: Boolean = false,
  enabled: Boolean = true,
  testTag: String? = null,
) {
  val bg =
    when {
      !enabled -> SteelMedium
      isPrimary -> TacticalGreenDark
      isDanger -> Color(0xFF7A2220)
      else -> SteelLight
    }
  val borderCol =
    when {
      !enabled -> SteelBorder.copy(alpha = 0.4f)
      isPrimary -> TacticalGreenBright.copy(alpha = 0.7f)
      isDanger -> Color(0xFFE57373)
      else -> SteelHighlight
    }

  Button(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(3.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = bg,
        contentColor = TextPrimary,
        disabledContainerColor = SteelMedium,
        disabledContentColor = TextSecondary.copy(alpha = 0.5f),
      ),
    border = BorderStroke(1.5.dp, borderCol),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .let { if (testTag != null) it.testTag(testTag) else it },
  ) {
    Text(
      text = text,
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp,
      letterSpacing = 0.5.sp,
    )
  }
}

@Composable
fun GreenSectionHeader(title: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp)) {
    Text(
      text = title,
      color = TacticalGreenBright,
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp,
      letterSpacing = 0.6.sp,
    )
    Spacer(modifier = Modifier.height(3.dp))
    HorizontalDivider(color = TacticalGreen.copy(alpha = 0.55f), thickness = 1.5.dp)
  }
}

@Composable
fun SettingCheckboxRow(
  label: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .clickable { onCheckedChange(!checked) }
        .padding(horizontal = 6.dp, vertical = 4.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
      if (subtitle != null) {
        Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
      }
    }
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors =
        CheckboxDefaults.colors(
          checkedColor = TacticalGreen,
          uncheckedColor = SteelHighlight,
          checkmarkColor = SteelDarkest,
        ),
    )
  }
}

@Composable
fun SettingSliderRow(
  label: String,
  value: Float,
  onValueChange: (Float) -> Unit,
  testTag: String,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
      Text(
        text = "${(value * 100).toInt()}%",
        color = TacticalGreenBright,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
      )
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = 0f..1f,
      modifier = Modifier.fillMaxWidth().testTag(testTag),
      colors =
        SliderDefaults.colors(
          thumbColor = TacticalGreenBright,
          activeTrackColor = TacticalGreen,
          inactiveTrackColor = SteelLight,
        ),
    )
  }
}

@Composable
fun UnitTypeBadge(
  type: UnitType,
  factionColor: Color = Color(0xFF42A5F5),
  darkColor: Color = Color(0xFF1565C0),
  lightColor: Color = Color(0xFF90CAF9),
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val r = size.minDimension * 0.38f
    drawEntitySprite(
      type = type,
      center = Offset(size.width / 2f, size.height / 2f),
      radius = r,
      bodyAngleRad = -0.55f,
      turretAngleRad = -0.35f,
      primaryColor = factionColor,
      darkColor = darkColor,
      lightColor = lightColor,
      elapsedTime = 1.2f,
    )
  }
}

@Composable
fun MinimapCanvas(
  terrainBitmap: ImageBitmap,
  mapWidthWorld: Float,
  mapHeightWorld: Float,
  simulation: GameSimulation? = null,
  fogCache: com.example.render.FogOverlayCache? = null,
  lobbyPlayers: List<LobbyPlayer> = emptyList(),
  spawnPoints: List<com.example.model.Vec2Data> = emptyList(),
  cameraX: Float = 0f,
  cameraY: Float = 0f,
  viewportWorldW: Float = 900f,
  viewportWorldH: Float = 600f,
  renderTick: Int = 0,
  onNavigateWorld: ((Float, Float) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .border(1.5.dp, SteelBorder, RoundedCornerShape(2.dp))
        .background(Color.Black)
        .let { base ->
          if (onNavigateWorld != null) {
            base
              .pointerInput(mapWidthWorld, mapHeightWorld) {
                detectTapGestures { pos ->
                  val wx = (pos.x / size.width).coerceIn(0f, 1f) * mapWidthWorld
                  val wy = (pos.y / size.height).coerceIn(0f, 1f) * mapHeightWorld
                  onNavigateWorld(wx, wy)
                }
              }
              .pointerInput(mapWidthWorld, mapHeightWorld) {
                detectDragGestures { change, _ ->
                  change.consume()
                  val wx = (change.position.x / size.width).coerceIn(0f, 1f) * mapWidthWorld
                  val wy = (change.position.y / size.height).coerceIn(0f, 1f) * mapHeightWorld
                  onNavigateWorld(wx, wy)
                }
              }
          } else base
        }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      @Suppress("UNUSED_VARIABLE")
      val tick = renderTick
      drawImage(
        image = terrainBitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(terrainBitmap.width, terrainBitmap.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
        filterQuality = FilterQuality.None,
      )

      if (simulation != null) {
        val sx = size.width / mapWidthWorld
        val sy = size.height / mapHeightWorld
        val pMap = simulation.playersById
        val humanTeam = pMap[simulation.humanPlayerId]?.team

        // Single-draw Fog overlay on minimap
        if (simulation.options.fogMode == FogMode.FOG_OF_WAR && fogCache != null) {
          val fogBmp = fogCache.sync(simulation.fogGrid, simulation.fogVersion, simulation.options.fogMode)
          drawImage(
            image = fogBmp,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(fogBmp.width, fogBmp.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.Low,
          )
        }

        // Entities on minimap
        for (e in simulation.entities) {
          val owner = pMap[e.ownerId] ?: continue
          val tx = simulation.map.worldToTileX(e.x)
          val ty = simulation.map.worldToTileY(e.y)
          val fog = simulation.fogGrid[ty * simulation.map.widthTiles + tx]
          if (owner.team != humanTeam && simulation.options.fogMode == FogMode.FOG_OF_WAR && fog < 2) {
            continue
          }
          val dotSize = if (e.type.isBuilding) 4.5f else 2.8f
          drawRect(
            color = Color(owner.color.primaryArgb),
            topLeft = Offset(e.x * sx - dotSize / 2f, e.y * sy - dotSize / 2f),
            size = Size(dotSize, dotSize),
          )
        }

        // Camera white box
        val boxW = (viewportWorldW * sx).coerceIn(8f, size.width)
        val boxH = (viewportWorldH * sy).coerceIn(8f, size.height)
        val boxL = (cameraX * sx - boxW / 2f).coerceIn(0f, (size.width - boxW).coerceAtLeast(0f))
        val boxT = (cameraY * sy - boxH / 2f).coerceIn(0f, (size.height - boxH).coerceAtLeast(0f))
        drawRect(
          color = Color.White,
          topLeft = Offset(boxL, boxT),
          size = Size(boxW, boxH),
          style = Stroke(width = 1.6f),
        )
      } else if (spawnPoints.isNotEmpty()) {
        // Lobby preview spawn points
        val sx = size.width / mapWidthWorld
        val sy = size.height / mapHeightWorld
        spawnPoints.forEachIndexed { idx, pt ->
          val spNum = idx + 1
          val player = lobbyPlayers.firstOrNull { it.spawnPoint == spNum }
          val col = if (player != null) Color(player.color.primaryArgb) else Color(0xFF90A4AE)
          val px = pt.x * sx
          val py = pt.y * sy
          drawCircle(color = Color.Black.copy(alpha = 0.7f), radius = 10f, center = Offset(px, py))
          drawCircle(color = col, radius = 8f, center = Offset(px, py))
        }
      }
    }

    // Spawn point number labels for Lobby preview
    if (simulation == null && spawnPoints.isNotEmpty()) {
      Box(modifier = Modifier.fillMaxSize()) {
        // Rendered cleanly via small positioned badges or Canvas-aligned layout
        spawnPoints.forEachIndexed { idx, pt ->
          val spNum = idx + 1
          val player = lobbyPlayers.firstOrNull { it.spawnPoint == spNum }
          Row(
            modifier =
              Modifier
                .align(
                  when (idx) {
                    0 -> Alignment.TopStart
                    1 -> Alignment.BottomEnd
                    2 -> Alignment.TopEnd
                    else -> Alignment.BottomStart
                  }
                )
                .padding(8.dp)
                .background(
                  color =
                    if (player != null) Color(player.color.primaryArgb).copy(alpha = 0.9f)
                    else Color.Black.copy(alpha = 0.6f),
                  shape = RoundedCornerShape(3.dp),
                )
                .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (player != null) "$spNum: ${player.team.name}" else "$spNum",
              color = Color.White,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

fun formatDuration(seconds: Float): String {
  val total = seconds.toInt().coerceAtLeast(0)
  val m = total / 60
  val s = total % 60
  return "%02d:%02d".format(m, s)
}
