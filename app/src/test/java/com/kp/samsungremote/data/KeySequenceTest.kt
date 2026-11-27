package com.kp.samsungremote.data

import com.kp.samsungremote.tv.TvKey
import org.junit.Assert.assertEquals
import org.junit.Test

class KeySequenceTest {
    @Test
    fun `encode and decode keep order, keys and pause`() {
        val list = listOf(
            KeySequence(id = "a", name = "Netflix", keys = listOf(TvKey.HOME, TvKey.LEFT, TvKey.LEFT, TvKey.ENTER), delayMs = 700),
            KeySequence(id = "b", name = "HDMI 2", keys = listOf(TvKey.HDMI2)),
        )
        assertEquals(list, KeySequence.decode(KeySequence.encode(list)))
    }

    @Test
    fun `missing or broken data gives an empty list`() {
        assertEquals(emptyList<KeySequence>(), KeySequence.decode(null))
        assertEquals(emptyList<KeySequence>(), KeySequence.decode(""))
        assertEquals(emptyList<KeySequence>(), KeySequence.decode("not json"))
    }

    @Test
    fun `a broken entry is skipped and the rest kept`() {
        val raw = """[{"name":"no id","keys":[]},{"id":"x","name":"Ok","keys":["KEY_HOME"]}]"""
        assertEquals(listOf(KeySequence(id = "x", name = "Ok", keys = listOf(TvKey.HOME))), KeySequence.decode(raw))
    }

    @Test
    fun `pause is clamped to the allowed range`() {
        val raw = """[{"id":"x","name":"Fast","keys":["KEY_UP"],"delayMs":5},{"id":"y","name":"Slow","keys":["KEY_UP"],"delayMs":999999}]"""
        val decoded = KeySequence.decode(raw)
        assertEquals(KeySequence.MIN_DELAY_MS, decoded[0].delayMs)
        assertEquals(KeySequence.MAX_DELAY_MS, decoded[1].delayMs)
    }
}
