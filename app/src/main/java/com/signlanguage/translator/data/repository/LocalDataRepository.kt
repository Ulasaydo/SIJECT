package com.signlanguage.translator.data.repository

import android.content.Context
import com.signlanguage.translator.data.model.WordHistory
import org.json.JSONArray
import org.json.JSONObject

class LocalDataRepository(context: Context) {
    private val preferences = context.getSharedPreferences("siject_local", Context.MODE_PRIVATE)

    fun saveLastPrediction(history: WordHistory) {
        val historyItems = getWordHistory().toMutableList()
        historyItems.add(0, history)
        preferences.edit()
            .putString("last_word", history.word)
            .putFloat("last_confidence", history.confidence)
            .putLong("last_timestamp", history.timestampMillis)
            .putString(KEY_WORD_HISTORY, encodeHistory(historyItems.take(MAX_HISTORY_ITEMS)))
            .apply()
    }

    fun getWordHistory(): List<WordHistory> {
        val rawHistory = preferences.getString(KEY_WORD_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawHistory)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                WordHistory(
                    word = item.getString("word"),
                    confidence = item.getDouble("confidence").toFloat(),
                    timestampMillis = item.getLong("timestampMillis")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun clearHistory() {
        preferences.edit()
            .remove("last_word")
            .remove("last_confidence")
            .remove("last_timestamp")
            .remove(KEY_WORD_HISTORY)
            .apply()
    }

    private fun encodeHistory(history: List<WordHistory>): String {
        val array = JSONArray()
        history.forEach { item ->
            array.put(
                JSONObject()
                    .put("word", item.word)
                    .put("confidence", item.confidence)
                    .put("timestampMillis", item.timestampMillis)
            )
        }
        return array.toString()
    }

    companion object {
        private const val KEY_WORD_HISTORY = "word_history"
        private const val MAX_HISTORY_ITEMS = 50
    }
}
