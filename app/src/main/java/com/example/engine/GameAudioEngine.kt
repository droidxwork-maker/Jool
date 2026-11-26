package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import com.example.model.GameSettings
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundEffect {
  UI_CLICK,
  UI_CONFIRM,
  SHOOT_LIGHT,
  SHOOT_HEAVY,
  SHOOT_AA,
  EXPLOSION,
  BUILD_COMPLETE,
  HIT_IMPACT,
  MISSILE_LAUNCH,
}

class GameAudioEngine(cacheDir: File) {
  private val soundPool: SoundPool =
    SoundPool.Builder()
      .setMaxStreams(12)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      .build()

  private val soundIds = HashMap<SoundEffect, Int>()
  private var settings = GameSettings()
  private var musicThread: Thread? = null

  @Volatile private var musicRunning = false

  @Volatile private var musicTrack: AudioTrack? = null
  private var lastSfxTimestamp = HashMap<SoundEffect, Long>()

  init {
    try {
      val sfxDir = File(cacheDir, "synth_sfx").apply { mkdirs() }
      soundIds[SoundEffect.UI_CLICK] = loadSynth(sfxDir, "ui_click.wav", generateUiClick())
      soundIds[SoundEffect.UI_CONFIRM] = loadSynth(sfxDir, "ui_confirm.wav", generateUiConfirm())
      soundIds[SoundEffect.SHOOT_LIGHT] = loadSynth(sfxDir, "shoot_light.wav", generateShootLight())
      soundIds[SoundEffect.SHOOT_HEAVY] = loadSynth(sfxDir, "shoot_heavy.wav", generateShootHeavy())
      soundIds[SoundEffect.SHOOT_AA] = loadSynth(sfxDir, "shoot_aa.wav", generateShootAa())
      soundIds[SoundEffect.EXPLOSION] = loadSynth(sfxDir, "explosion.wav", generateExplosion())
      soundIds[SoundEffect.BUILD_COMPLETE] =
        loadSynth(sfxDir, "build_done.wav", generateBuildComplete())
      soundIds[SoundEffect.HIT_IMPACT] = loadSynth(sfxDir, "hit_impact.wav", generateHitImpact())
      soundIds[SoundEffect.MISSILE_LAUNCH] = loadSynth(sfxDir, "missile_launch.wav", generateMissileLaunch())
    } catch (_: Throwable) {
      // Safe fallback in headless unit test environments (e.g. Robolectric)
    }
  }

  fun updateSettings(newSettings: GameSettings) {
    settings = newSettings
    val vol = settings.musicVolume.coerceIn(0f, 1f)
    try {
      musicTrack?.setVolume(vol)
    } catch (_: Throwable) {}
    if (vol > 0.01f && !musicRunning) {
      startAmbientMusic()
    } else if (vol <= 0.01f && musicRunning) {
      stopAmbientMusic()
    }
  }

  fun play(effect: SoundEffect) {
    val id = soundIds[effect] ?: return
    val isUi = effect == SoundEffect.UI_CLICK || effect == SoundEffect.UI_CONFIRM
    val vol = (if (isUi) settings.interfaceVolume else settings.gameVolume).coerceIn(0f, 1f)
    if (vol <= 0.01f) return

    val now = System.currentTimeMillis()
    val minGapMs = if (isUi) 40L else 70L
    val prev = lastSfxTimestamp[effect] ?: 0L
    if (now - prev < minGapMs) return
    lastSfxTimestamp[effect] = now

    try {
      soundPool.play(id, vol, vol, 1, 0, 1f)
    } catch (_: Throwable) {}
  }

  fun startAmbientMusic() {
    if (musicRunning || settings.musicVolume <= 0.01f) return
    musicRunning = true
    musicThread =
      Thread {
        try {
          val sampleRate = 22050
          val loopSeconds = 4
          val totalSamples = sampleRate * loopSeconds
          val pcm = ShortArray(totalSamples)
          val bassNotes = floatArrayOf(55.0f, 55.0f, 65.41f, 49.0f)
          for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val beatIdx = (t / 1.0f).toInt().coerceIn(0, 3)
            val localT = t % 1.0f
            val freq = bassNotes[beatIdx]
            val pulseEnv = exp(-localT * 3.5f)
            val pad =
              sin(2.0 * PI * freq * t) * 0.25 +
                sin(2.0 * PI * (freq * 1.5) * t) * 0.12 +
                sin(2.0 * PI * (freq * 2.0) * t) * 0.15 * pulseEnv
            // Subtle tactical sonar tick on beat 0 and 2
            val ping =
              if ((beatIdx == 0 || beatIdx == 2) && localT < 0.25f) {
                sin(2.0 * PI * 440.0 * localT) * exp(-localT * 14f) * 0.08
              } else 0.0
            pcm[i] = ((pad + ping) * 5500.0).toInt().coerceIn(-32767, 32767).toShort()
          }

          val minBuf =
            AudioTrack.getMinBufferSize(
              sampleRate,
              AudioFormat.CHANNEL_OUT_MONO,
              AudioFormat.ENCODING_PCM_16BIT,
            )
          val track =
            AudioTrack.Builder()
              .setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_GAME)
                  .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                  .build()
              )
              .setAudioFormat(
                AudioFormat.Builder()
                  .setSampleRate(sampleRate)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .build()
              )
              .setBufferSizeInBytes(maxOf(minBuf, totalSamples * 2))
              .setTransferMode(AudioTrack.MODE_STATIC)
              .build()

          track.write(pcm, 0, pcm.size)
          track.setLoopPoints(0, totalSamples, -1)
          track.setVolume(settings.musicVolume.coerceIn(0f, 1f))
          track.play()
          musicTrack = track
        } catch (_: Throwable) {
          musicRunning = false
        }
      }
        .apply {
          isDaemon = true
          start()
        }
  }

  fun stopAmbientMusic() {
    musicRunning = false
    try {
      musicTrack?.pause()
      musicTrack?.flush()
      musicTrack?.release()
    } catch (_: Throwable) {}
    musicTrack = null
  }

  fun release() {
    stopAmbientMusic()
    try {
      soundPool.release()
    } catch (_: Throwable) {}
  }

  private fun loadSynth(dir: File, name: String, pcm: ShortArray): Int {
    val file = File(dir, name)
    file.writeBytes(encodeWav(pcm, 22050))
    return soundPool.load(file.absolutePath, 1)
  }

  private fun generateUiClick(): ShortArray {
    val sr = 22050
    val len = (sr * 0.055f).toInt()
    return ShortArray(len) { i ->
      val t = i.toFloat() / sr
      val env = exp(-t * 85f)
      val s = sin(2.0 * PI * 920.0 * t) + 0.5 * sin(2.0 * PI * 1480.0 * t)
      (s * env * 14000.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  private fun generateUiConfirm(): ShortArray {
    val sr = 22050
    val len = (sr * 0.14f).toInt()
    return ShortArray(len) { i ->
      val t = i.toFloat() / sr
      val freq = if (t < 0.06f) 580.0 else 860.0
      val env = exp(-t * 24f)
      (sin(2.0 * PI * freq * t) * env * 15000.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  private fun generateShootLight(): ShortArray {
    val sr = 22050
    val len = (sr * 0.11f).toInt()
    val rng = Random(11)
    return ShortArray(len) { i ->
      val t = i.toFloat() / sr
      val freq = 520.0 - t * 2400.0
      val env = exp(-t * 36f)
      val noise = (rng.nextFloat() * 2f - 1f) * 0.35f
      ((sin(2.0 * PI * freq.coerceAtLeast(90.0) * t) + noise) * env * 15000.0)
        .toInt()
        .coerceIn(-32767, 32767)
        .toShort()
    }
  }

  private fun generateShootHeavy(): ShortArray {
    val sr = 22050
    val len = (sr * 0.24f).toInt()
    val rng = Random(22)
    return ShortArray(len) { i ->
      val t = i.toFloat() / sr
      val freq = 180.0 - t * 420.0
      val env = exp(-t * 15f)
      val noise = (rng.nextFloat() * 2f - 1f) * 0.6f
      ((sin(2.0 * PI * freq.coerceAtLeast(45.0) * t) + noise) * env * 18000.0)
        .toInt()
        .coerceIn(-32767, 32767)
        .toShort()
    }
  }

  private fun generateShootAa(): ShortArray {
    val sr = 22050
    val len = (sr * 0.14f).toInt()
    val rng = Random(33)
    return ShortArray(len) { i ->
      val t = i.toFloat() / sr
      val env = exp(-t * 25f)
      val chirp = sin(2.0 * PI * (950.0 + t * 1800.0) * t) * 0.5
      val hiss = (rng.nextFloat() * 2f - 1f) * 0.5
      ((chirp + hiss) * env * 14000.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  private fun generateExplosion(): ShortArray {
    val sr = 22050
    val len = (sr * 0.36f).toInt()
    val rng = Random(44)
    var lp = 0f
    return ShortArray(len) { i ->
      val t = i.toFloat() / sr
      val env = exp(-t * 9.5f)
      val white = rng.nextFloat() * 2f - 1f
      lp = lp * 0.72f + white * 0.28f
      val boom = sin(2.0 * PI * 72.0 * t) * 0.45
      ((lp * 1.4 + boom) * env * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  private fun generateBuildComplete(): ShortArray {
    val sr = 22050
    val len = (sr * 0.22f).toInt()
    return ShortArray(len) { i ->
      val t = i.toFloat() / sr
      val freq = if (t < 0.09f) 523.25 else 783.99
      val env = exp(-t * 14f)
      (sin(2.0 * PI * freq * t) * env * 14000.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  private fun generateHitImpact(): ShortArray {
    val sr = 22050
    val len = (sr * 0.12f).toInt()
    val rng = Random(88)
    return ShortArray(len) { i ->
      val t = i.toFloat() / sr
      val env = exp(-t * 22f)
      val noise = rng.nextFloat() * 2f - 1f
      val metallic = sin(2.0 * PI * 850.0 * t) * 0.5
      ((noise * 0.8 + metallic) * env * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  private fun generateMissileLaunch(): ShortArray {
    val sr = 22050
    val len = (sr * 0.4f).toInt()
    val rng = Random(99)
    return ShortArray(len) { i ->
      val t = i.toFloat() / sr
      val env = exp(-t * 6.5f)
      val whoosh = rng.nextFloat() * 2f - 1f
      val pitch = sin(2.0 * PI * (120.0 + t * 450.0) * t) * 0.6
      ((whoosh * 0.5 + pitch) * env * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
    }
  }

  private fun encodeWav(samples: ShortArray, sampleRate: Int): ByteArray {
    val dataSize = samples.size * 2
    val out = ByteArrayOutputStream(44 + dataSize)
    fun writeStr(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
    fun writeIntLE(v: Int) {
      out.write(v and 0xFF)
      out.write((v shr 8) and 0xFF)
      out.write((v shr 16) and 0xFF)
      out.write((v shr 24) and 0xFF)
    }
    fun writeShortLE(v: Int) {
      out.write(v and 0xFF)
      out.write((v shr 8) and 0xFF)
    }

    writeStr("RIFF")
    writeIntLE(36 + dataSize)
    writeStr("WAVE")
    writeStr("fmt ")
    writeIntLE(16)
    writeShortLE(1) // PCM
    writeShortLE(1) // Mono
    writeIntLE(sampleRate)
    writeIntLE(sampleRate * 2)
    writeShortLE(2)
    writeShortLE(16)
    writeStr("data")
    writeIntLE(dataSize)
    for (s in samples) {
      writeShortLE(s.toInt())
    }
    return out.toByteArray()
  }
}
