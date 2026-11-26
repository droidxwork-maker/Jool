package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameSimulation
import com.example.model.FogMode
import com.example.model.GameSettings
import com.example.model.MatchResult
import com.example.model.TileType
import com.example.model.UnitDomain
import com.example.model.UnitStance
import com.example.model.UnitType
import com.example.render.FogOverlayCache
import com.example.render.MapTerrainCache
import com.example.render.ProceduralSprites.drawEntitySprite
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.SteelDark
import com.example.ui.theme.SteelDarkest
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalGreenBright
import com.example.ui.theme.TacticalRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.HudSnapshot
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun InGameScreen(
  simulation: GameSimulation,
  terrainCache: MapTerrainCache,
  fogCache: FogOverlayCache,
  settings: GameSettings,
  hud: HudSnapshot,
  cameraX: Float,
  cameraY: Float,
  cameraZoom: Float,
  onCameraChange: (Float, Float, Float) -> Unit,
  selectedEntityIds: Set<Int>,
  pendingBuildType: UnitType?,
  boxSelectMode: Boolean,
  onToggleBoxSelectMode: () -> Unit,
  isPaused: Boolean,
  onSetPaused: (Boolean) -> Unit,
  matchResult: MatchResult?,
  renderTick: Int,
  onTapMapWorld: (Float, Float) -> Unit,
  onDoubleTapMapWorld: (Float, Float) -> Unit,
  onSelectWorldRect: (Float, Float, Float, Float) -> Unit,
  onUnselectAll: () -> Unit,
  onBuildBuilding: (UnitType) -> Unit,
  onProduceUnit: (UnitType) -> Unit,
  onStopUnits: () -> Unit,
  onSelectAllCombat: () -> Unit,
  onSelectIdleBuilder: () -> Unit,
  onSetStance: (UnitStance) -> Unit,
  onAssignGroup: (Int) -> Unit,
  onRecallGroup: (Int, Boolean) -> Unit,
  onSaveGame: (String, (Boolean) -> Unit) -> Unit,
  onOpenSettings: () -> Unit,
  onBackToLobby: () -> Unit,
  onQuitToMenu: () -> Unit,
) {
  var viewportWpx by remember { mutableFloatStateOf(900f) }
  var viewportHpx by remember { mutableFloatStateOf(600f) }
  var dragBoxStart by remember { mutableStateOf<Offset?>(null) }
  var dragBoxEnd by remember { mutableStateOf<Offset?>(null) }
  var lastTapTimeMs by remember { mutableLongStateOf(0L) }
  var lastTapScreenPos by remember { mutableStateOf(Offset.Zero) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  BackHandler { onSetPaused(!isPaused) }

  fun screenToWorld(sx: Float, sy: Float): Offset {
    val wx = cameraX + (sx - viewportWpx / 2f) / cameraZoom
    val wy = cameraY + (sy - viewportHpx / 2f) / cameraZoom
    return Offset(wx, wy)
  }

  fun clampCamera(cx: Float, cy: Float): Pair<Float, Float> {
    val clampedX = cx.coerceIn(0f, simulation.map.worldWidth)
    val clampedY = cy.coerceIn(0f, simulation.map.worldHeight)
    return Pair(clampedX, clampedY)
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(SteelDarkest)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Row(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxHeight()
            .onSizeChanged { sz ->
              viewportWpx = sz.width.toFloat().coerceAtLeast(100f)
              viewportHpx = sz.height.toFloat().coerceAtLeast(100f)
            }
      ) {
        Canvas(
          modifier =
            Modifier
              .fillMaxSize()
              .pointerInput(boxSelectMode, pendingBuildType, cameraX, cameraY, cameraZoom) {
                awaitEachGesture {
                  val down = awaitFirstDown(requireUnconsumed = false)
                  val downPos = down.position
                  val downTime = System.currentTimeMillis()
                  var movedTotal = 0f
                  var isMultiTouch = false
                  var autoLongPressBox = false
                  var curCamX = cameraX
                  var curCamY = cameraY
                  var curZoom = cameraZoom

                  if (boxSelectMode && pendingBuildType == null) {
                    dragBoxStart = downPos
                    dragBoxEnd = downPos
                  }

                  while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) {
                      val activeBox = (boxSelectMode || autoLongPressBox) && pendingBuildType == null
                      if (activeBox && dragBoxStart != null && dragBoxEnd != null && movedTotal > 18f) {
                        val w1 = screenToWorld(dragBoxStart!!.x, dragBoxStart!!.y)
                        val w2 = screenToWorld(dragBoxEnd!!.x, dragBoxEnd!!.y)
                        onSelectWorldRect(
                          min(w1.x, w2.x),
                          min(w1.y, w2.y),
                          max(w1.x, w2.x),
                          max(w1.y, w2.y),
                        )
                      } else if (!isMultiTouch && movedTotal < 16f) {
                        val now = System.currentTimeMillis()
                        val worldPt = screenToWorld(downPos.x, downPos.y)
                        val isDoubleTap =
                          (now - lastTapTimeMs) < 320L &&
                            hypot(downPos.x - lastTapScreenPos.x, downPos.y - lastTapScreenPos.y) < 42f
                        if (isDoubleTap && pendingBuildType == null) {
                          onDoubleTapMapWorld(worldPt.x, worldPt.y)
                          lastTapTimeMs = 0L
                        } else {
                          onTapMapWorld(worldPt.x, worldPt.y)
                          lastTapTimeMs = now
                          lastTapScreenPos = downPos
                        }
                      }
                      dragBoxStart = null
                      dragBoxEnd = null
                      break
                    }

                    if (pressed.size >= 2) {
                      isMultiTouch = true
                      dragBoxStart = null
                      dragBoxEnd = null
                      val zoomChange = event.calculateZoom()
                      val panChange = event.calculatePan()
                      val newZoom = (curZoom * zoomChange).coerceIn(0.45f, 2.4f)
                      val (nx, ny) =
                        clampCamera(
                          curCamX - panChange.x / newZoom,
                          curCamY - panChange.y / newZoom,
                        )
                      curZoom = newZoom
                      curCamX = nx
                      curCamY = ny
                      onCameraChange(curCamX, curCamY, curZoom)
                      pressed.forEach { it.consume() }
                    } else {
                      val c = pressed.first()
                      val delta = c.position - c.previousPosition
                      movedTotal += hypot(delta.x, delta.y)

                      if (!boxSelectMode && !autoLongPressBox && pendingBuildType == null && movedTotal < 12f) {
                        if (System.currentTimeMillis() - downTime > 240L) {
                          autoLongPressBox = true
                          dragBoxStart = downPos
                          dragBoxEnd = c.position
                        }
                      }

                      if ((boxSelectMode || autoLongPressBox) && pendingBuildType == null) {
                        dragBoxEnd = c.position
                        c.consume()
                      } else if (movedTotal > 10f) {
                        val (nx, ny) =
                          clampCamera(
                            curCamX - delta.x / curZoom,
                            curCamY - delta.y / curZoom,
                          )
                        curCamX = nx
                        curCamY = ny
                        onCameraChange(curCamX, curCamY, curZoom)
                        c.consume()
                      }
                    }
                  }
                }
              }
              .testTag("battlefield_canvas")
        ) {
          @Suppress("UNUSED_VARIABLE")
          val tick = renderTick

          val halfW = size.width / 2f
          val halfH = size.height / 2f

          withTransform({
            translate(left = halfW - cameraX * cameraZoom, top = halfH - cameraY * cameraZoom)
            scale(scaleX = cameraZoom, scaleY = cameraZoom, pivot = Offset.Zero)
          }) {
            val mapW = simulation.map.worldWidth
            val mapH = simulation.map.worldHeight

            drawImage(
              image = terrainCache.terrainBitmap,
              srcOffset = IntOffset.Zero,
              srcSize = IntSize(terrainCache.terrainBitmap.width, terrainCache.terrainBitmap.height),
              dstOffset = IntOffset.Zero,
              dstSize = IntSize(mapW.toInt(), mapH.toInt()),
              filterQuality = FilterQuality.None,
            )

            val minTx = simulation.map.worldToTileX(cameraX - halfW / cameraZoom - 48f)
            val maxTx = simulation.map.worldToTileX(cameraX + halfW / cameraZoom + 48f)
            val minTy = simulation.map.worldToTileY(cameraY - halfH / cameraZoom - 48f)
            val maxTy = simulation.map.worldToTileY(cameraY + halfH / cameraZoom + 48f)
            val ts = simulation.map.tileSize
            val wavePhase = simulation.elapsedSeconds * 2.2f

            for (ty in minTy..maxTy) {
              for (tx in minTx..maxTx) {
                if (simulation.map.tileAt(tx, ty) == TileType.WATER) {
                  val shift = sin(wavePhase + tx * 0.7f + ty * 0.5f) * 6f
                  drawLine(
                    color = Color(0xFFB3E5FC).copy(alpha = 0.25f),
                    start = Offset(tx * ts + 8f + shift, ty * ts + ts * 0.45f),
                    end = Offset(tx * ts + 26f + shift, ty * ts + ts * 0.45f),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round,
                  )
                }
              }
            }

            for (sc in simulation.scorchDecals) {
              drawCircle(color = Color(0xFF141210).copy(alpha = sc.alpha), radius = sc.radius, center = Offset(sc.x, sc.y))
              drawCircle(color = Color(0xFF0A0807).copy(alpha = sc.alpha * 0.85f), radius = sc.radius * 0.52f, center = Offset(sc.x, sc.y))
            }

            val pMap = simulation.playersById
            val humanTeam = pMap[simulation.humanPlayerId]?.team

            if (settings.showUnitWaypoints) {
              val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
              for (e in simulation.entities) {
                if (e.id !in selectedEntityIds || e.ownerId != simulation.humanPlayerId) continue
                if (e.waypoints.isNotEmpty()) {
                  var prev = Offset(e.x, e.y)
                  for (wp in e.waypoints) {
                    val cur = Offset(wp.x, wp.y)
                    drawLine(color = Color(0xFF69F0AE).copy(alpha = 0.78f), start = prev, end = cur, strokeWidth = 2.2f, pathEffect = dashEffect)
                    prev = cur
                  }
                  drawCircle(color = Color(0xFF69F0AE), radius = 6f, center = prev, style = Stroke(width = 2f))
                }
                e.rallyPoint?.let { rp ->
                  drawLine(color = Color(0xFF40C4FF).copy(alpha = 0.72f), start = Offset(e.x, e.y), end = Offset(rp.x, rp.y), strokeWidth = 2f, pathEffect = dashEffect)
                  drawCircle(color = Color(0xFF40C4FF), radius = 7f, center = Offset(rp.x, rp.y), style = Stroke(2f))
                }
              }
            }

            for (e in simulation.entities) {
              if (e.id in selectedEntityIds && e.type.attackRange > 0f) {
                drawCircle(color = Color.White.copy(alpha = 0.36f), radius = e.type.attackRange, center = Offset(e.x, e.y), style = Stroke(width = 1.6f))
              }
            }

            if (pendingBuildType != null) {
              if (pendingBuildType == UnitType.EXTRACTOR) {
                val pulse = 0.55f + 0.35f * sin(simulation.elapsedSeconds * 5f)
                for (pool in simulation.map.resourcePools) {
                  val occupied = simulation.entities.any { it.type == UnitType.EXTRACTOR && hypot(it.x - pool.x, it.y - pool.y) < 38f }
                  if (!occupied) {
                    drawCircle(color = Color(0xFF69F0AE).copy(alpha = pulse), radius = 28f, center = Offset(pool.x, pool.y), style = Stroke(width = 2.5f))
                  }
                }
              }
              val snapped = simulation.snapBuildPosition(pendingBuildType, cameraX, cameraY)
              val valid = simulation.canPlaceBuilding(pendingBuildType, snapped.x, snapped.y)
              val ghostColor = if (valid) Color(0xFF69F0AE) else Color(0xFFFF5252)
              drawCircle(color = ghostColor.copy(alpha = 0.24f), radius = pendingBuildType.radius, center = Offset(snapped.x, snapped.y))
              drawCircle(color = ghostColor, radius = pendingBuildType.radius, center = Offset(snapped.x, snapped.y), style = Stroke(width = 2.5f))
            }

            val ordered = simulation.entities.sortedBy {
              when (it.type.domain) {
                UnitDomain.BUILDING -> 0
                UnitDomain.GROUND -> 1
                UnitDomain.AIR -> 2
              }
            }
            val entityLookup = simulation.entities.associateBy { it.id }

            for (e in ordered) {
              val owner = pMap[e.ownerId] ?: continue
              val tx = simulation.map.worldToTileX(e.x)
              val ty = simulation.map.worldToTileY(e.y)
              val fog = simulation.fogGrid[ty * simulation.map.widthTiles + tx]
              val isEnemy = owner.team != humanTeam
              if (isEnemy && simulation.options.fogMode == FogMode.FOG_OF_WAR) {
                if (fog == 0) continue
                if (fog == 1 && !e.type.isBuilding) continue
              }

              val center = Offset(e.x, e.y)
              val isSelected = e.id in selectedEntityIds

              if (isSelected) {
                drawSelectionBrackets(center, e.type.radius + 6f, Color(0xFF69F0AE))
              }

              drawEntitySprite(
                type = e.type,
                center = center,
                radius = e.type.radius,
                bodyAngleRad = e.bodyAngle,
                turretAngleRad = e.turretAngle,
                primaryColor = Color(owner.color.primaryArgb),
                darkColor = Color(owner.color.darkArgb),
                lightColor = Color(owner.color.lightArgb),
                isUnderConstruction = e.isUnderConstruction,
                buildProgress = e.buildProgress,
                muzzleFlash = e.muzzleFlashTimer > 0f,
                elapsedTime = simulation.elapsedSeconds,
                alpha = if (isEnemy && fog == 1) 0.65f else 1f,
                shieldHp = e.shieldHp,
                maxShieldHp = e.maxShieldHp,
              )

              if (e.type == UnitType.BUILDER) {
                val beamTarget = (e.buildTaskBuildingId ?: e.repairTargetId)?.let { entityLookup[it] }
                if (beamTarget != null && hypot(beamTarget.x - e.x, beamTarget.y - e.y) <= e.type.attackRange + beamTarget.type.radius + 24f) {
                  val endPt = Offset(beamTarget.x, beamTarget.y)
                  val pulse = 0.65f + 0.35f * sin(simulation.elapsedSeconds * 24f)
                  drawLine(color = Color(0xFF00E676).copy(alpha = 0.36f * pulse), start = center, end = endPt, strokeWidth = 7f, cap = StrokeCap.Round)
                  drawLine(color = Color(0xFFB9F6CA), start = center, end = endPt, strokeWidth = 2.4f, cap = StrokeCap.Round)
                  drawCircle(color = Color(0xFF69F0AE).copy(alpha = pulse), radius = 7.5f, center = endPt)
                }
              }

              if (settings.alwaysShowUnitHealth || isSelected || e.hp < e.maxHp * 0.99f) {
                val bw = e.type.radius * 2.15f
                val bh = 4.5f
                val bx = e.x - bw / 2f
                val by = e.y - e.type.radius - 11f
                val ratio = (e.hp / e.maxHp).coerceIn(0f, 1f)
                drawRect(color = Color.Black.copy(alpha = 0.8f), topLeft = Offset(bx - 1f, by - 1f), size = Size(bw + 2f, bh + 2f))
                drawRect(
                  color = if (ratio > 0.5f) Color(0xFF69F0AE) else if (ratio > 0.25f) Color(0xFFFFB300) else Color(0xFFE53935),
                  topLeft = Offset(bx, by),
                  size = Size(bw * ratio, bh),
                )
              }
            }

            for (proj in simulation.projectiles) {
              drawLine(color = Color(proj.colorArgb).copy(alpha = 0.55f), start = Offset(proj.prevX, proj.prevY), end = Offset(proj.x, proj.y), strokeWidth = 3.2f, cap = StrokeCap.Round)
              drawCircle(color = Color(proj.colorArgb).copy(alpha = 0.45f), radius = 6f, center = Offset(proj.x, proj.y))
              drawCircle(color = Color.White, radius = 2.6f, center = Offset(proj.x, proj.y))
            }

            for (pt in simulation.particles) {
              val lifeRatio = (pt.life / pt.maxLife).coerceIn(0f, 1f)
              if (pt.isRing) {
                val expand = 1f - lifeRatio
                drawCircle(color = Color(pt.colorArgb).copy(alpha = lifeRatio * 0.65f), radius = pt.radius * (0.35f + expand * 1.15f), center = Offset(pt.x, pt.y), style = Stroke(width = 3.2f * lifeRatio))
              } else {
                drawCircle(color = Color(pt.colorArgb).copy(alpha = lifeRatio * if (pt.isSmoke) 0.52f else 0.92f), radius = pt.radius * (if (pt.isSmoke) 1.55f - lifeRatio * 0.5f else lifeRatio + 0.35f), center = Offset(pt.x, pt.y))
              }
            }

            if (simulation.options.fogMode == FogMode.FOG_OF_WAR) {
              val fogBmp = fogCache.sync(simulation.fogGrid, simulation.fogVersion, simulation.options.fogMode)
              drawImage(image = fogBmp, srcOffset = IntOffset.Zero, srcSize = IntSize(fogBmp.width, fogBmp.height), dstOffset = IntOffset.Zero, dstSize = IntSize(mapW.toInt(), mapH.toInt()), filterQuality = FilterQuality.Low)
            }

            if (settings.renderClouds) {
              for (c in simulation.clouds) {
                drawOval(color = Color.Black.copy(alpha = c.alpha), topLeft = Offset(c.x - c.radiusX, c.y - c.radiusY), size = Size(c.radiusX * 2f, c.radiusY * 2f))
              }
            }
          }

          if (dragBoxStart != null && dragBoxEnd != null) {
            val left = min(dragBoxStart!!.x, dragBoxEnd!!.x)
            val top = min(dragBoxStart!!.y, dragBoxEnd!!.y)
            val w = abs(dragBoxEnd!!.x - dragBoxStart!!.x)
            val h = abs(dragBoxEnd!!.y - dragBoxStart!!.y)
            drawRect(color = Color(0xFF69F0AE).copy(alpha = 0.18f), topLeft = Offset(left, top), size = Size(w, h))
            drawRect(color = Color(0xFF69F0AE), topLeft = Offset(left, top), size = Size(w, h), style = Stroke(width = 2.2f))
          }
        }

        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .align(Alignment.TopStart)
              .padding(8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            IconButton(
              onClick = { onSetPaused(true) },
              modifier =
                Modifier
                  .size(48.dp)
                  .background(SteelDark.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                  .border(1.5.dp, SteelBorder, RoundedCornerShape(4.dp))
                  .testTag("hud_pause_button"),
            ) {
              Icon(imageVector = Icons.Default.Settings, contentDescription = "Pause and Menu", tint = TextPrimary)
            }

            Row(
              modifier =
                Modifier
                  .background(SteelDark.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                  .border(1.5.dp, SteelBorder, RoundedCornerShape(4.dp))
                  .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "$${hud.credits} (+${hud.incomeRate})",
                color = TacticalGreenBright,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                modifier = Modifier.testTag("hud_credits_text"),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(text = "Units: ${hud.unitCount}/${hud.unitCap}", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Text(text = formatDuration(hud.elapsedSeconds), color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
          }

          if (settings.showFps) {
            Text(
              text = "${hud.fps} FPS",
              color = TacticalAmber,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              modifier =
                Modifier
                  .background(SteelDark.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                  .border(1.dp, SteelBorder, RoundedCornerShape(4.dp))
                  .padding(horizontal = 8.dp, vertical = 5.dp)
                  .testTag("hud_fps_text"),
            )
          }
        }

        Row(
          modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier =
              Modifier
                .height(48.dp)
                .background(
                  if (boxSelectMode) TacticalGreenBright else SteelDark.copy(alpha = 0.92f),
                  RoundedCornerShape(4.dp),
                )
                .border(1.5.dp, if (boxSelectMode) Color.White else SteelBorder, RoundedCornerShape(4.dp))
                .clickable { onToggleBoxSelectMode() }
                .padding(horizontal = 12.dp)
                .testTag("hud_box_select_toggle"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.HighlightAlt,
              contentDescription = "Box Selection Mode",
              tint = if (boxSelectMode) Color.Black else TextPrimary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (boxSelectMode) "Box Select: ON" else "Pan (Hold=Box)",
              color = if (boxSelectMode) Color.Black else TextPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
            )
          }

          if (settings.showZoomButtons) {
            IconButton(
              onClick = { onCameraChange(cameraX, cameraY, (cameraZoom + 0.22f).coerceAtMost(2.4f)) },
              modifier = Modifier.size(48.dp).background(SteelDark.copy(alpha = 0.92f), RoundedCornerShape(4.dp)).border(1.5.dp, SteelBorder, RoundedCornerShape(4.dp)).testTag("hud_zoom_in"),
            ) {
              Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = TextPrimary)
            }
            IconButton(
              onClick = { onCameraChange(cameraX, cameraY, (cameraZoom - 0.22f).coerceAtLeast(0.45f)) },
              modifier = Modifier.size(48.dp).background(SteelDark.copy(alpha = 0.92f), RoundedCornerShape(4.dp)).border(1.5.dp, SteelBorder, RoundedCornerShape(4.dp)).testTag("hud_zoom_out"),
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextPrimary)
            }
          }

          if (pendingBuildType != null) {
            Row(
              modifier =
                Modifier
                  .height(48.dp)
                  .background(SteelDark.copy(alpha = 0.95f), RoundedCornerShape(4.dp))
                  .border(1.5.dp, TacticalGreenBright, RoundedCornerShape(4.dp))
                  .clickable { onBuildBuilding(pendingBuildType) }
                  .padding(horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Place ${pendingBuildType.shortName} (Tap map)",
                color = TacticalGreenBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(Icons.Default.Close, contentDescription = "Cancel placement", tint = TacticalRed, modifier = Modifier.size(18.dp))
            }
          }
        }

        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
        )
      }

      InGameCommandSidebar(
        simulation = simulation,
        terrainCache = terrainCache,
        fogCache = fogCache,
        hud = hud,
        cameraX = cameraX,
        cameraY = cameraY,
        viewportWorldW = viewportWpx / cameraZoom,
        viewportWorldH = viewportHpx / cameraZoom,
        pendingBuildType = pendingBuildType,
        renderTick = renderTick,
        onMinimapNavigate = { wx, wy ->
          val (cx, cy) = clampCamera(wx, wy)
          onCameraChange(cx, cy, cameraZoom)
        },
        onUnselectAll = onUnselectAll,
        onBuildBuilding = onBuildBuilding,
        onProduceUnit = onProduceUnit,
        onStopUnits = onStopUnits,
        onSelectAllCombat = onSelectAllCombat,
        onSelectIdleBuilder = onSelectIdleBuilder,
        onSetStance = onSetStance,
        onAssignGroup = onAssignGroup,
        onRecallGroup = onRecallGroup,
        modifier = Modifier.width(235.dp),
      )
    }

    if (isPaused && matchResult == null) {
      InGamePauseDialog(
        defaultSaveName = "${simulation.map.mapId.displayName} ${formatDuration(hud.elapsedSeconds)}",
        onResume = { onSetPaused(false) },
        onSaveGame = { name ->
          onSaveGame(name) { ok ->
            scope.launch {
              snackbarHostState.showSnackbar(if (ok) "Battle saved: $name" else "Failed to save battle")
            }
            onSetPaused(false)
          }
        },
        onOpenSettings = onOpenSettings,
        onQuitToMenu = onQuitToMenu,
      )
    }

    if (matchResult != null) {
      MatchResultDialog(
        result = matchResult,
        onBackToLobby = onBackToLobby,
        onMainMenu = onQuitToMenu,
      )
    }
  }
}

private fun DrawScope.drawSelectionBrackets(center: Offset, r: Float, color: Color) {
  drawCircle(color = color.copy(alpha = 0.16f), radius = r, center = center)
  val arm = (r * 0.42f).coerceAtLeast(6f)
  val stroke = 2.4f
  val l = center.x - r
  val t = center.y - r
  val ri = center.x + r
  val b = center.y + r
  drawLine(color, Offset(l, t + arm), Offset(l, t), stroke)
  drawLine(color, Offset(l, t), Offset(l + arm, t), stroke)
  drawLine(color, Offset(ri - arm, t), Offset(ri, t), stroke)
  drawLine(color, Offset(ri, t), Offset(ri, t + arm), stroke)
  drawLine(color, Offset(l, b - arm), Offset(l, b), stroke)
  drawLine(color, Offset(l, b), Offset(l + arm, b), stroke)
  drawLine(color, Offset(ri - arm, b), Offset(ri, b), stroke)
  drawLine(color, Offset(ri, b), Offset(ri, b - arm), stroke)
}
