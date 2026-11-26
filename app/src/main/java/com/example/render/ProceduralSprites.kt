package com.example.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.UnitType
import kotlin.math.PI

object ProceduralSprites {
  private val outlineDark = Color(0xFF080C10)
  private val gunmetalDark = Color(0xFF182026)
  private val gunmetalMid = Color(0xFF323E48)
  private val gunmetalLight = Color(0xFF687C8C)
  private val steelShine = Color(0xFF9EBCD0)
  private val hazardOrange = Color(0xFFFF9100)

  fun DrawScope.drawEntitySprite(
    type: UnitType,
    center: Offset,
    radius: Float,
    bodyAngleRad: Float,
    turretAngleRad: Float,
    primaryColor: Color,
    darkColor: Color,
    lightColor: Color,
    isUnderConstruction: Boolean = false,
    buildProgress: Float = 1f,
    muzzleFlash: Boolean = false,
    elapsedTime: Float = 0f,
    alpha: Float = 1f,
    shieldHp: Float = 0f,
    maxShieldHp: Float = 0f,
  ) {
    val bodyDeg = bodyAngleRad * (180f / PI.toFloat())
    val turretDeg = turretAngleRad * (180f / PI.toFloat())

    // Soft drop shadow for 3D realism
    val shadowOffset =
      if (type == UnitType.FIGHTER) Offset(radius * 0.8f, radius * 0.95f)
      else Offset(radius * 0.18f, radius * 0.24f)
    drawOval(
      color = Color.Black.copy(alpha = (if (type == UnitType.FIGHTER) 0.32f else 0.48f) * alpha),
      topLeft = center + shadowOffset - Offset(radius * 1.05f, radius * 0.9f),
      size = Size(radius * 2.1f, radius * 1.8f),
    )

    val effAlpha = if (isUnderConstruction) (0.45f + 0.55f * buildProgress) * alpha else alpha

    when (type) {
      UnitType.COMMAND_CENTER ->
        drawCommandCenter(center, radius, primaryColor, darkColor, lightColor, elapsedTime, effAlpha)
      UnitType.EXTRACTOR ->
        drawExtractor(center, radius, primaryColor, darkColor, lightColor, elapsedTime, effAlpha)
      UnitType.OIL_REFINERY ->
        drawOilRefinery(center, radius, primaryColor, darkColor, lightColor, elapsedTime, effAlpha)
      UnitType.DEFENSE_WALL ->
        drawDefenseWall(center, radius, primaryColor, darkColor, lightColor, effAlpha)
      UnitType.STEEL_GATE ->
        drawSteelGate(center, radius, primaryColor, darkColor, lightColor, false, effAlpha)
      UnitType.TURRET ->
        drawTurret(center, radius, turretDeg, primaryColor, darkColor, lightColor, muzzleFlash, effAlpha)
      UnitType.ANTI_AIR ->
        drawAntiAir(center, radius, turretDeg, primaryColor, darkColor, lightColor, muzzleFlash, effAlpha)
      UnitType.LAND_FACTORY ->
        drawLandFactory(center, radius, primaryColor, darkColor, lightColor, elapsedTime, effAlpha)
      UnitType.AIR_FACTORY ->
        drawAirFactory(center, radius, primaryColor, darkColor, lightColor, elapsedTime, effAlpha)
      UnitType.TECH_CENTER ->
        drawTechCenter(center, radius, primaryColor, darkColor, lightColor, elapsedTime, effAlpha)
      UnitType.MISSILE_SILO ->
        drawMissileSilo(center, radius, primaryColor, darkColor, lightColor, muzzleFlash, effAlpha)
      UnitType.BUILDER ->
        drawBuilder(center, radius, bodyDeg, turretDeg, primaryColor, darkColor, lightColor, effAlpha)
      UnitType.LIGHT_TANK ->
        drawLightTank(center, radius, bodyDeg, turretDeg, primaryColor, darkColor, lightColor, muzzleFlash, effAlpha)
      UnitType.HEAVY_TANK ->
        drawHeavyTank(center, radius, bodyDeg, turretDeg, primaryColor, darkColor, lightColor, muzzleFlash, effAlpha)
      UnitType.ARTILLERY ->
        drawArtillery(center, radius, bodyDeg, turretDeg, primaryColor, darkColor, lightColor, muzzleFlash, effAlpha)
      UnitType.FIGHTER ->
        drawFighter(center, radius, bodyDeg, primaryColor, darkColor, lightColor, muzzleFlash, effAlpha)
      UnitType.EMP_MECH ->
        drawEmpMech(center, radius, bodyDeg, turretDeg, primaryColor, darkColor, lightColor, muzzleFlash, effAlpha)
      UnitType.SHIELD_TANK ->
        drawShieldTank(center, radius, bodyDeg, turretDeg, primaryColor, darkColor, lightColor, muzzleFlash, effAlpha)
    }

    // High fidelity Energy Shield Bubble
    if (maxShieldHp > 0f && shieldHp > 0f) {
      val shieldRatio = (shieldHp / maxShieldHp).coerceIn(0f, 1f)
      drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = (0.25f + 0.2f * shieldRatio) * alpha),
        radius = radius * 1.4f,
        center = center,
      )
      drawCircle(
        color = Color(0xFF84FFFF).copy(alpha = 0.85f * alpha),
        radius = radius * 1.4f,
        center = center,
        style = Stroke(width = 2f),
      )
    }

    if (isUnderConstruction) {
      drawCircle(color = Color(0xFF00E676).copy(alpha = 0.25f), radius = radius * 1.08f, center = center)
      drawCircle(color = Color(0xFF69F0AE).copy(alpha = 0.85f), radius = radius * 1.08f, center = center, style = Stroke(width = 1.6f))
      drawArc(
        color = Color(0xFF69F0AE),
        startAngle = -90f,
        sweepAngle = 360f * buildProgress,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.78f),
        size = Size(radius * 1.56f, radius * 1.56f),
        style = Stroke(width = 2.4f, cap = StrokeCap.Round),
      )
    }
  }

  private fun DrawScope.drawTreads(c: Offset, r: Float, halfWidth: Float, treadThick: Float, length: Float, alpha: Float) {
    val startX = c.x - length / 2f
    listOf(c.y - halfWidth, c.y + halfWidth - treadThick).forEach { ty ->
      drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(startX - 1.5f, ty - 1.5f), size = Size(length + 3f, treadThick + 3f), cornerRadius = CornerRadius(r * 0.1f))
      drawRoundRect(color = Color(0xFF1A2026).copy(alpha = alpha), topLeft = Offset(startX, ty), size = Size(length, treadThick), cornerRadius = CornerRadius(r * 0.08f))
      val step = (length / 6f).coerceAtLeast(2f)
      var x = startX + step * 0.3f
      while (x < startX + length - 1f) {
        drawRect(color = Color(0xFF526270).copy(alpha = alpha), topLeft = Offset(x, ty + 0.5f), size = Size((step * 0.4f).coerceAtLeast(1f), treadThick - 1f))
        x += step
      }
    }
  }

  private fun DrawScope.drawCommandCenter(c: Offset, r: Float, primary: Color, dark: Color, light: Color, time: Float, alpha: Float) {
    val s = r * 1.9f
    drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - s / 2f - 2f, c.y - s / 2f - 2f), size = Size(s + 4f, s + 4f), cornerRadius = CornerRadius(r * 0.28f))
    drawRoundRect(
      brush = Brush.linearGradient(listOf(gunmetalLight.copy(alpha = alpha), gunmetalDark.copy(alpha = alpha)), c - Offset(r, r), c + Offset(r, r)),
      topLeft = Offset(c.x - s / 2f, c.y - s / 2f),
      size = Size(s, s),
      cornerRadius = CornerRadius(r * 0.25f),
    )
    val innerS = s * 0.84f
    drawRoundRect(color = Color(0xFF141B22).copy(alpha = alpha), topLeft = Offset(c.x - innerS / 2f, c.y - innerS / 2f), size = Size(innerS, innerS), cornerRadius = CornerRadius(r * 0.18f))

    val bSize = r * 0.48f
    val off = s * 0.36f
    listOf(Offset(-off, -off), Offset(off, -off), Offset(-off, off), Offset(off, off)).forEach { o ->
      val bc = c + o
      drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = bc - Offset(bSize / 2f + 1.2f, bSize / 2f + 1.2f), size = Size(bSize + 2.4f, bSize + 2.4f), cornerRadius = CornerRadius(r * 0.1f))
      drawRoundRect(color = dark.copy(alpha = alpha), topLeft = bc - Offset(bSize / 2f, bSize / 2f), size = Size(bSize, bSize), cornerRadius = CornerRadius(r * 0.08f))
      drawCircle(color = primary.copy(alpha = alpha), radius = bSize * 0.3f, center = bc)
    }

    drawCircle(color = outlineDark.copy(alpha = alpha), radius = r * 0.68f, center = c)
    drawCircle(color = dark.copy(alpha = alpha), radius = r * 0.5f, center = c)
    drawCircle(color = primary.copy(alpha = alpha), radius = r * 0.38f, center = c)
    drawCircle(color = light.copy(alpha = alpha), radius = r * 0.2f, center = c)

    rotate(degrees = (time * 90f) % 360f, pivot = c) {
      drawLine(color = Color(0xFFE0F7FA).copy(alpha = alpha), start = c, end = Offset(c.x + r * 0.58f, c.y), strokeWidth = 2.4f, cap = StrokeCap.Round)
      drawCircle(color = Color.White.copy(alpha = alpha), radius = r * 0.1f, center = c)
    }
  }

  private fun DrawScope.drawExtractor(c: Offset, r: Float, primary: Color, dark: Color, light: Color, time: Float, alpha: Float) {
    drawCircle(color = outlineDark.copy(alpha = alpha), radius = r * 1.05f, center = c)
    drawCircle(color = gunmetalDark.copy(alpha = alpha), radius = r * 0.98f, center = c)
    drawCircle(color = dark.copy(alpha = alpha), radius = r * 0.9f, center = c, style = Stroke(width = r * 0.2f))
    val podR = r * 0.3f
    listOf(Offset(-r * 0.52f, -r * 0.48f), Offset(r * 0.52f, r * 0.48f)).forEach { off ->
      drawCircle(color = outlineDark.copy(alpha = alpha), radius = podR + 1.5f, center = c + off)
      drawCircle(color = primary.copy(alpha = alpha), radius = podR, center = c + off)
    }
    rotate(degrees = (time * 180f) % 360f, pivot = c) {
      drawRoundRect(color = steelShine.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.6f, c.y - r * 0.15f), size = Size(r * 1.2f, r * 0.3f), cornerRadius = CornerRadius(r * 0.08f))
      drawRoundRect(color = gunmetalLight.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.15f, c.y - r * 0.6f), size = Size(r * 0.3f, r * 1.2f), cornerRadius = CornerRadius(r * 0.08f))
    }
    drawCircle(color = outlineDark.copy(alpha = alpha), radius = r * 0.29f, center = c)
    drawCircle(color = Color(0xFF00E5FF).copy(alpha = 0.9f * alpha), radius = r * 0.19f, center = c)
  }

  private fun DrawScope.drawTurret(c: Offset, r: Float, turretDeg: Float, primary: Color, dark: Color, light: Color, muzzleFlash: Boolean, alpha: Float) {
    drawCircle(color = outlineDark.copy(alpha = alpha), radius = r * 1.02f, center = c)
    drawCircle(color = Color(0xFF141A20).copy(alpha = alpha), radius = r * 0.74f, center = c)
    rotate(degrees = turretDeg, pivot = c) {
      listOf(-r * 0.26f, r * 0.12f).forEach { oy ->
        drawRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x + r * 0.2f, c.y + oy - 1.2f), size = Size(r * 1.1f, r * 0.18f + 2.4f))
        drawRect(color = steelShine.copy(alpha = alpha), topLeft = Offset(c.x + r * 0.22f, c.y + oy), size = Size(r * 1.04f, r * 0.18f))
      }
      if (muzzleFlash) {
        drawCircle(color = Color(0xFFFFD54F), radius = r * 0.32f, center = Offset(c.x + r * 1.35f, c.y))
      }
      drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.58f, c.y - r * 0.55f), size = Size(r * 1.14f, r * 1.1f), cornerRadius = CornerRadius(r * 0.28f))
      drawRoundRect(color = dark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.54f, c.y - r * 0.51f), size = Size(r * 1.06f, r * 1.02f), cornerRadius = CornerRadius(r * 0.24f))
      drawRoundRect(color = primary.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.44f, c.y - r * 0.41f), size = Size(r * 0.86f, r * 0.82f), cornerRadius = CornerRadius(r * 0.2f))
    }
  }

  private fun DrawScope.drawAntiAir(c: Offset, r: Float, turretDeg: Float, primary: Color, dark: Color, light: Color, muzzleFlash: Boolean, alpha: Float) {
    drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.95f, c.y - r * 0.95f), size = Size(r * 1.9f, r * 1.9f), cornerRadius = CornerRadius(r * 0.3f))
    drawCircle(color = gunmetalDark.copy(alpha = alpha), radius = r * 0.7f, center = c)
    rotate(degrees = turretDeg, pivot = c) {
      drawRoundRect(color = dark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.48f, c.y - r * 0.56f), size = Size(r * 1.02f, r * 1.12f), cornerRadius = CornerRadius(r * 0.16f))
      drawRect(color = primary.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.38f, c.y - r * 0.4f), size = Size(r * 0.76f, r * 0.8f))
      val tipColor = if (muzzleFlash) Color(0xFF69F0AE) else Color(0xFFFF5252)
      listOf(-r * 0.36f, -r * 0.13f, r * 0.13f, r * 0.36f).forEach { oy ->
        drawCircle(color = tipColor.copy(alpha = alpha), radius = r * 0.1f, center = Offset(c.x + r * 0.58f, c.y + oy))
      }
    }
  }

  private fun DrawScope.drawLandFactory(c: Offset, r: Float, primary: Color, dark: Color, light: Color, time: Float, alpha: Float) {
    val w = r * 2.0f
    val h = r * 1.8f
    drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - w / 2f - 2f, c.y - h / 2f - 2f), size = Size(w + 4f, h + 4f), cornerRadius = CornerRadius(r * 0.18f))
    drawRoundRect(color = gunmetalDark.copy(alpha = alpha), topLeft = Offset(c.x - w / 2f, c.y - h / 2f), size = Size(w, h), cornerRadius = CornerRadius(r * 0.16f))
    drawRect(color = Color(0xFF0C1218).copy(alpha = alpha), topLeft = Offset(c.x - w * 0.36f, c.y - h * 0.05f), size = Size(w * 0.72f, h * 0.54f))
    drawRoundRect(color = dark.copy(alpha = alpha), topLeft = Offset(c.x - w * 0.48f, c.y - h * 0.48f), size = Size(w * 0.96f, h * 0.48f), cornerRadius = CornerRadius(r * 0.1f))
    drawRect(color = primary.copy(alpha = alpha), topLeft = Offset(c.x - w * 0.4f, c.y - h * 0.39f), size = Size(w * 0.8f, h * 0.3f))
  }

  private fun DrawScope.drawAirFactory(c: Offset, r: Float, primary: Color, dark: Color, light: Color, time: Float, alpha: Float) {
    val s = r * 1.95f
    drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - s / 2f - 2f, c.y - s / 2f - 2f), size = Size(s + 4f, s + 4f), cornerRadius = CornerRadius(r * 0.22f))
    drawRoundRect(color = Color(0xFF18222C).copy(alpha = alpha), topLeft = Offset(c.x - s / 2f, c.y - s / 2f), size = Size(s, s), cornerRadius = CornerRadius(r * 0.2f))
    drawCircle(color = Color(0xFFFFB300).copy(alpha = 0.75f * alpha), radius = r * 0.76f, center = c + Offset(0f, r * 0.15f), style = Stroke(width = r * 0.1f))
    drawRoundRect(color = dark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.85f, c.y - r * 0.86f), size = Size(r * 1.7f, r * 0.6f), cornerRadius = CornerRadius(r * 0.12f))
    drawRect(color = primary.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.72f, c.y - r * 0.76f), size = Size(r * 1.44f, r * 0.36f))
  }

  private fun DrawScope.drawTechCenter(c: Offset, r: Float, primary: Color, dark: Color, light: Color, time: Float, alpha: Float) {
    val s = r * 1.88f
    drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - s / 2f - 2f, c.y - s / 2f - 2f), size = Size(s + 4f, s + 4f), cornerRadius = CornerRadius(r * 0.26f))
    drawRoundRect(color = Color(0xFF101C26).copy(alpha = alpha), topLeft = Offset(c.x - s / 2f, c.y - s / 2f), size = Size(s, s), cornerRadius = CornerRadius(r * 0.22f))
    drawCircle(color = dark.copy(alpha = alpha), radius = r * 0.62f, center = c)
    drawCircle(color = primary.copy(alpha = alpha), radius = r * 0.45f, center = c)
    rotate(degrees = (time * 120f) % 360f, pivot = c) {
      drawRect(color = Color(0xFF00E5FF).copy(alpha = alpha), topLeft = Offset(c.x - r * 0.4f, c.y - r * 0.11f), size = Size(r * 0.8f, r * 0.22f))
      drawCircle(color = Color.White.copy(alpha = alpha), radius = r * 0.15f, center = c)
    }
  }

  private fun DrawScope.drawMissileSilo(c: Offset, r: Float, primary: Color, dark: Color, light: Color, muzzleFlash: Boolean, alpha: Float) {
    val s = r * 1.95f
    drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - s / 2f - 2f, c.y - s / 2f - 2f), size = Size(s + 4f, s + 4f), cornerRadius = CornerRadius(r * 0.22f))
    drawRoundRect(color = Color(0xFF261A14).copy(alpha = alpha), topLeft = Offset(c.x - s / 2f, c.y - s / 2f), size = Size(s, s), cornerRadius = CornerRadius(r * 0.2f))
    drawCircle(color = dark.copy(alpha = alpha), radius = r * 0.74f, center = c)
    drawCircle(color = Color(0xFFFF6D00).copy(alpha = if (muzzleFlash) 1f else 0.5f), radius = r * 0.45f, center = c)
    drawCircle(color = Color(0xFFFFD54F), radius = r * 0.24f, center = c)
  }

  private fun DrawScope.drawBuilder(c: Offset, r: Float, bodyDeg: Float, turretDeg: Float, primary: Color, dark: Color, light: Color, alpha: Float) {
    rotate(degrees = bodyDeg, pivot = c) {
      drawTreads(c, r, r * 0.75f, r * 0.32f, r * 1.6f, alpha)
      drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.7f, c.y - r * 0.55f), size = Size(r * 1.4f, r * 1.1f), cornerRadius = CornerRadius(r * 0.3f))
      drawRoundRect(color = dark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.48f), size = Size(r * 1.24f, r * 0.96f), cornerRadius = CornerRadius(r * 0.25f))
      drawRoundRect(color = primary.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.48f, c.y - r * 0.36f), size = Size(r * 0.96f, r * 0.72f), cornerRadius = CornerRadius(r * 0.2f))
    }
    rotate(degrees = turretDeg, pivot = c) {
      drawCircle(color = outlineDark.copy(alpha = alpha), radius = r * 0.38f, center = c)
      drawCircle(color = gunmetalLight.copy(alpha = alpha), radius = r * 0.3f, center = c)
      drawRect(color = steelShine.copy(alpha = alpha), topLeft = Offset(c.x, c.y - r * 0.08f), size = Size(r * 0.9f, r * 0.16f))
    }
  }

  private fun DrawScope.drawLightTank(c: Offset, r: Float, bodyDeg: Float, turretDeg: Float, primary: Color, dark: Color, light: Color, muzzleFlash: Boolean, alpha: Float) {
    rotate(degrees = bodyDeg, pivot = c) {
      drawTreads(c, r, r * 0.8f, r * 0.35f, r * 1.7f, alpha)
      drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.75f, c.y - r * 0.6f), size = Size(r * 1.5f, r * 1.2f), cornerRadius = CornerRadius(r * 0.35f))
      drawRoundRect(color = dark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.68f, c.y - r * 0.52f), size = Size(r * 1.36f, r * 1.04f), cornerRadius = CornerRadius(r * 0.3f))
      drawRoundRect(color = primary.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.52f, c.y - r * 0.38f), size = Size(r * 1.04f, r * 0.76f), cornerRadius = CornerRadius(r * 0.22f))
    }
    rotate(degrees = turretDeg, pivot = c) {
      if (muzzleFlash) {
        drawCircle(color = Color(0xFFFFD54F), radius = r * 0.3f, center = Offset(c.x + r * 1.2f, c.y))
      }
      drawRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x, c.y - r * 0.09f), size = Size(r * 1.0f, r * 0.18f + 2f))
      drawRect(color = steelShine.copy(alpha = alpha), topLeft = Offset(c.x, c.y - r * 0.08f), size = Size(r * 0.96f, r * 0.16f))
      drawCircle(color = outlineDark.copy(alpha = alpha), radius = r * 0.44f, center = c)
      drawCircle(color = dark.copy(alpha = alpha), radius = r * 0.38f, center = c)
      drawCircle(color = primary.copy(alpha = alpha), radius = r * 0.26f, center = c)
    }
  }

  private fun DrawScope.drawHeavyTank(c: Offset, r: Float, bodyDeg: Float, turretDeg: Float, primary: Color, dark: Color, light: Color, muzzleFlash: Boolean, alpha: Float) {
    rotate(degrees = bodyDeg, pivot = c) {
      drawTreads(c, r, r * 0.92f, r * 0.42f, r * 1.95f, alpha)
      drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.88f, c.y - r * 0.72f), size = Size(r * 1.76f, r * 1.44f), cornerRadius = CornerRadius(r * 0.4f))
      drawRoundRect(color = dark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.8f, c.y - r * 0.64f), size = Size(r * 1.6f, r * 1.28f), cornerRadius = CornerRadius(r * 0.35f))
      drawRoundRect(color = primary.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.48f), size = Size(r * 1.24f, r * 0.96f), cornerRadius = CornerRadius(r * 0.28f))
    }
    rotate(degrees = turretDeg, pivot = c) {
      if (muzzleFlash) {
        drawCircle(color = Color(0xFFFFD54F), radius = r * 0.38f, center = Offset(c.x + r * 1.4f, c.y))
      }
      drawRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x, c.y - r * 0.14f), size = Size(r * 1.2f, r * 0.28f + 2f))
      drawRect(color = steelShine.copy(alpha = alpha), topLeft = Offset(c.x, c.y - r * 0.12f), size = Size(r * 1.14f, r * 0.24f))
      drawCircle(color = outlineDark.copy(alpha = alpha), radius = r * 0.55f, center = c)
      drawCircle(color = dark.copy(alpha = alpha), radius = r * 0.48f, center = c)
      drawCircle(color = primary.copy(alpha = alpha), radius = r * 0.34f, center = c)
    }
  }

  private fun DrawScope.drawArtillery(c: Offset, r: Float, bodyDeg: Float, turretDeg: Float, primary: Color, dark: Color, light: Color, muzzleFlash: Boolean, alpha: Float) {
    rotate(degrees = bodyDeg, pivot = c) {
      drawTreads(c, r, r * 0.76f, r * 0.34f, r * 1.7f, alpha)
      drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.78f, c.y - r * 0.58f), size = Size(r * 1.56f, r * 1.16f), cornerRadius = CornerRadius(r * 0.3f))
      drawRoundRect(color = dark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.7f, c.y - r * 0.5f), size = Size(r * 1.4f, r * 1.0f), cornerRadius = CornerRadius(r * 0.25f))
    }
    rotate(degrees = turretDeg, pivot = c) {
      if (muzzleFlash) {
        drawCircle(color = Color(0xFFFFD54F), radius = r * 0.42f, center = Offset(c.x + r * 1.6f, c.y))
      }
      drawRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.2f, c.y - r * 0.18f), size = Size(r * 1.6f, r * 0.36f + 2f))
      drawRect(color = steelShine.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.15f, c.y - r * 0.14f), size = Size(r * 1.5f, r * 0.28f))
      drawCircle(color = outlineDark.copy(alpha = alpha), radius = r * 0.46f, center = c)
      drawCircle(color = primary.copy(alpha = alpha), radius = r * 0.36f, center = c)
    }
  }

  private fun DrawScope.drawFighter(c: Offset, r: Float, bodyDeg: Float, primary: Color, dark: Color, light: Color, muzzleFlash: Boolean, alpha: Float) {
    rotate(degrees = bodyDeg, pivot = c) {
      val path = Path().apply {
        moveTo(c.x + r * 1.3f, c.y)
        lineTo(c.x - r * 0.9f, c.y - r * 1.1f)
        lineTo(c.x - r * 0.5f, c.y)
        lineTo(c.x - r * 0.9f, c.y + r * 1.1f)
        close()
      }
      drawPath(path, color = outlineDark.copy(alpha = alpha))
      val pathInner = Path().apply {
        moveTo(c.x + r * 1.15f, c.y)
        lineTo(c.x - r * 0.8f, c.y - r * 0.98f)
        lineTo(c.x - r * 0.42f, c.y)
        lineTo(c.x - r * 0.8f, c.y + r * 0.98f)
        close()
      }
      drawPath(pathInner, color = dark.copy(alpha = alpha))
      drawCircle(color = primary.copy(alpha = alpha), radius = r * 0.35f, center = c + Offset(-r * 0.1f, 0f))
      if (muzzleFlash) {
        drawCircle(color = Color(0xFF69F0AE), radius = r * 0.26f, center = Offset(c.x + r * 1.45f, c.y))
      }
    }
  }

  private fun DrawScope.drawEmpMech(c: Offset, r: Float, bodyDeg: Float, turretDeg: Float, primary: Color, dark: Color, light: Color, muzzleFlash: Boolean, alpha: Float) {
    rotate(degrees = bodyDeg, pivot = c) {
      drawCircle(color = outlineDark.copy(alpha = alpha), radius = r * 0.85f, center = c)
      drawCircle(color = dark.copy(alpha = alpha), radius = r * 0.75f, center = c)
    }
    rotate(degrees = turretDeg, pivot = c) {
      if (muzzleFlash) {
        drawCircle(color = Color(0xFF00E5FF), radius = r * 0.35f, center = Offset(c.x + r * 1.2f, c.y))
      }
      drawCircle(color = primary.copy(alpha = alpha), radius = r * 0.45f, center = c)
      drawRect(color = Color(0xFF00E5FF).copy(alpha = alpha), topLeft = Offset(c.x, c.y - r * 0.12f), size = Size(r * 1.05f, r * 0.24f))
    }
  }

  private fun DrawScope.drawShieldTank(c: Offset, r: Float, bodyDeg: Float, turretDeg: Float, primary: Color, dark: Color, light: Color, muzzleFlash: Boolean, alpha: Float) {
    rotate(degrees = bodyDeg, pivot = c) {
      drawTreads(c, r, r * 0.88f, r * 0.38f, r * 1.85f, alpha)
      drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.82f, c.y - r * 0.65f), size = Size(r * 1.64f, r * 1.3f), cornerRadius = CornerRadius(r * 0.38f))
      drawRoundRect(color = dark.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.74f, c.y - r * 0.58f), size = Size(r * 1.48f, r * 1.16f), cornerRadius = CornerRadius(r * 0.32f))
      drawRoundRect(color = primary.copy(alpha = alpha), topLeft = Offset(c.x - r * 0.58f, c.y - r * 0.42f), size = Size(r * 1.16f, r * 0.84f), cornerRadius = CornerRadius(r * 0.25f))
    }
    rotate(degrees = turretDeg, pivot = c) {
      if (muzzleFlash) {
        drawCircle(color = Color(0xFF84FFFF), radius = r * 0.32f, center = Offset(c.x + r * 1.3f, c.y))
      }
      drawRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x, c.y - r * 0.12f), size = Size(r * 1.1f, r * 0.24f + 2f))
      drawRect(color = Color(0xFF00E5FF).copy(alpha = alpha), topLeft = Offset(c.x, c.y - r * 0.1f), size = Size(r * 1.04f, r * 0.2f))
      drawCircle(color = outlineDark.copy(alpha = alpha), radius = r * 0.48f, center = c)
      drawCircle(color = Color(0xFF00B0FF).copy(alpha = alpha), radius = r * 0.38f, center = c)
    }
  }

  private fun DrawScope.drawOilRefinery(c: Offset, r: Float, primary: Color, dark: Color, light: Color, time: Float, alpha: Float) {
    val s = r * 1.85f
    drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - s / 2f - 2f, c.y - s / 2f - 2f), size = Size(s + 4f, s + 4f), cornerRadius = CornerRadius(r * 0.22f))
    drawRoundRect(color = Color(0xFF2A1F14).copy(alpha = alpha), topLeft = Offset(c.x - s / 2f, c.y - s / 2f), size = Size(s, s), cornerRadius = CornerRadius(r * 0.18f))
    drawCircle(color = dark.copy(alpha = alpha), radius = r * 0.65f, center = c)
    drawCircle(color = Color(0xFFFF9100).copy(alpha = 0.85f * alpha), radius = r * 0.45f, center = c)
    drawCircle(color = Color(0xFFFFD54F), radius = r * 0.22f, center = c)
  }

  private fun DrawScope.drawDefenseWall(c: Offset, r: Float, primary: Color, dark: Color, light: Color, alpha: Float) {
    val w = r * 1.8f
    val h = r * 0.9f
    drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - w / 2f - 1.5f, c.y - h / 2f - 1.5f), size = Size(w + 3f, h + 3f), cornerRadius = CornerRadius(r * 0.2f))
    drawRoundRect(color = Color(0xFF42505B).copy(alpha = alpha), topLeft = Offset(c.x - w / 2f, c.y - h / 2f), size = Size(w, h), cornerRadius = CornerRadius(r * 0.15f))
    drawRoundRect(color = primary.copy(alpha = alpha), topLeft = Offset(c.x - w * 0.38f, c.y - h * 0.3f), size = Size(w * 0.76f, h * 0.3f), cornerRadius = CornerRadius(r * 0.1f))
  }

  private fun DrawScope.drawSteelGate(c: Offset, r: Float, primary: Color, dark: Color, light: Color, isGateOpen: Boolean, alpha: Float) {
    val w = r * 1.9f
    val h = r * 0.95f
    drawRoundRect(color = outlineDark.copy(alpha = alpha), topLeft = Offset(c.x - w / 2f - 1.5f, c.y - h / 2f - 1.5f), size = Size(w + 3f, h + 3f), cornerRadius = CornerRadius(r * 0.2f))
    drawRoundRect(color = Color(0xFF232B32).copy(alpha = alpha), topLeft = Offset(c.x - w / 2f, c.y - h / 2f), size = Size(w, h), cornerRadius = CornerRadius(r * 0.15f))
    if (!isGateOpen) {
      drawRect(color = Color(0xFFCFD8DC).copy(alpha = alpha), topLeft = Offset(c.x - w * 0.4f, c.y - h * 0.25f), size = Size(w * 0.8f, h * 0.5f))
    } else {
      drawRect(color = Color(0xFF37474F).copy(alpha = alpha), topLeft = Offset(c.x - w * 0.4f, c.y - h * 0.25f), size = Size(w * 0.15f, h * 0.5f))
      drawRect(color = Color(0xFF37474F).copy(alpha = alpha), topLeft = Offset(c.x + w * 0.25f, c.y - h * 0.25f), size = Size(w * 0.15f, h * 0.5f))
    }
  }
}
