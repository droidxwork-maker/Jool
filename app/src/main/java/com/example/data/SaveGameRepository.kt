package com.example.data

import android.content.Context
import com.example.model.GameSaveData
import com.example.model.SavedGameSummary
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveGameRepository(private val context: Context) {
  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val adapter = moshi.adapter(GameSaveData::class.java)

  private val savesDir: File
    get() = File(context.filesDir, "saves").apply { mkdirs() }

  suspend fun listSaves(): List<SavedGameSummary> =
    withContext(Dispatchers.IO) {
      val files = savesDir.listFiles { f -> f.isFile && f.extension == "json" } ?: return@withContext emptyList()
      files
        .mapNotNull { file ->
          runCatching {
            val data = adapter.fromJson(file.readText()) ?: return@runCatching null
            SavedGameSummary(
              saveId = data.saveId,
              saveName = data.saveName,
              mapId = data.mapId,
              isAutosave = data.isAutosave,
              savedAtMillis = data.savedAtMillis,
              elapsedSeconds = data.elapsedSeconds,
            )
          }.getOrNull()
        }
        .sortedByDescending { it.savedAtMillis }
    }

  suspend fun saveGame(saveData: GameSaveData): Boolean =
    withContext(Dispatchers.IO) {
      runCatching {
        val safeId = saveData.saveId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(savesDir, "$safeId.json")
        file.writeText(adapter.toJson(saveData))
        true
      }.getOrDefault(false)
    }

  suspend fun loadGame(saveId: String): GameSaveData? =
    withContext(Dispatchers.IO) {
      runCatching {
        val safeId = saveId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(savesDir, "$safeId.json")
        if (!file.exists()) return@runCatching null
        adapter.fromJson(file.readText())
      }.getOrNull()
    }

  suspend fun deleteSave(saveId: String): Boolean =
    withContext(Dispatchers.IO) {
      runCatching {
        val safeId = saveId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(savesDir, "$safeId.json")
        if (file.exists()) file.delete() else false
      }.getOrDefault(false)
    }
}
