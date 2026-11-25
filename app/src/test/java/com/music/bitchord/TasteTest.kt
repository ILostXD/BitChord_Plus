package com.music.bitchord

import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.stats.Taste
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TasteTest {
    private val day = 86_400_000L
    private val now = 1_000 * day

    private fun song(id: String, title: String, artist: String, autoplay: Boolean = true) =
        Song(videoId = id, title = title, artist = artist, thumbnailUrl = null, durationText = "3:30",
            queueTier = if (autoplay) QueueTier.AUTOPLAY else QueueTier.CONTEXT)

    @Test
    fun `finishing counts for a song and skipping an AutoPlay pick counts against it`() {
        var state = Taste.State()
        state = Taste.learn(state, song("a", "Liked", "One"), 210_000, 210_000, now)
        state = Taste.learn(state, song("b", "Skipped", "Two"), 8_000, 210_000, now)
        assertTrue(Taste.weight(state, song("a", "Liked", "One"), now) > 1.0)
        assertTrue(Taste.weight(state, song("b", "Skipped", "Two"), now) < 1.0)
    }

    @Test
    fun `a skip of a song you chose yourself weighs less`() {
        val auto = Taste.learn(Taste.State(), song("b", "S", "Two"), 8_000, 210_000, now)
        val chosen = Taste.learn(Taste.State(), song("b", "S", "Two", autoplay = false), 8_000, 210_000, now)
        assertTrue(Taste.weight(auto, song("b", "S", "Two"), now) < Taste.weight(chosen, song("b", "S", "Two"), now))
    }

    @Test
    fun `old listening fades`() {
        val state = Taste.learn(Taste.State(), song("a", "Liked", "One"), 210_000, 210_000, now)
        assertTrue(Taste.weight(state, song("a", "Liked", "One"), now + 400 * day) < Taste.weight(state, song("a", "Liked", "One"), now))
    }

    @Test
    fun `songs skipped twice or just heard are left out, and favourites come back`() {
        var state = Taste.State()
        repeat(2) { state = Taste.learn(state, song("x", "Twice skipped", "Two"), 5_000, 210_000, now - day) }
        state = Taste.learn(state, song("j", "Just heard", "Three"), 210_000, 210_000, now - 60_000)
        repeat(3) { state = Taste.learn(state, song("fav", "Old favourite", "One"), 210_000, 210_000, now - 10 * day) }
        val radio = listOf(song("x", "Twice skipped", "Two"), song("j", "Just heard", "Three")) +
            (1..12).map { song("r$it", "Radio $it", "One") }
        val queue = Taste.smartQueue(radio, state, now, Random(1))
        val ids = queue.map { it.videoId }
        assertFalse("x" in ids)
        assertFalse("j" in ids)
        assertTrue("fav" in ids)
        assertEquals(13, queue.size)
    }
}
