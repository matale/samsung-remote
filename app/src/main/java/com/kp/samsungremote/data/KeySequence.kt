package com.kp.samsungremote.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A saved list of keys sent one after another, with [delayMs] of pause between two presses. */
data class KeySequence(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val keys: List<String>,
    val delayMs: Long = DEFAULT_DELAY_MS,
) {
    companion object {
        const val DEFAULT_DELAY_MS = 500L
        const val MIN_DELAY_MS = 100L
        const val MAX_DELAY_MS = 5_000L

        fun encode(sequences: List<KeySequence>): String = JSONArray().apply {
            sequences.forEach { seq ->
                put(
                    JSONObject()
                        .put("id", seq.id)
                        .put("name", seq.name)
                        .put("delayMs", seq.delayMs)
                        .put("keys", JSONArray(seq.keys)),
                )
            }
        }.toString()

        /** Reads what [encode] wrote. Broken entries are skipped instead of losing the whole list. */
        fun decode(raw: String?): List<KeySequence> {
            if (raw.isNullOrBlank()) {
                return emptyList()
            }
            val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
            return (0 until array.length()).mapNotNull { i ->
                runCatching {
                    val obj = array.getJSONObject(i)
                    val keys = obj.getJSONArray("keys")
                    KeySequence(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        keys = (0 until keys.length()).map { keys.getString(it) },
                        delayMs = obj.optLong("delayMs", DEFAULT_DELAY_MS).coerceIn(MIN_DELAY_MS, MAX_DELAY_MS),
                    )
                }.getOrNull()
            }
        }
    }
}
