package com.music.bitchord

import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.screens.discNumbers
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscNumbersTest {
    private fun song(id: String, title: String) = Song(videoId = id, title = title, artist = "A", thumbnailUrl = null, fromAutoplay = false)

    @Test
    fun positionalWhenListsMatch() {
        val songs = listOf(song("a", "One"), song("b", "Two"), song("c", "Three"))
        val apple = listOf("One" to 1, "Two" to 1, "Three" to 2)
        assertEquals(mapOf("a" to 1, "b" to 1, "c" to 2), discNumbers(songs, apple))
    }

    @Test
    fun byTitleWhenListsDifferAndEmptyForOneDisc() {
        val songs = listOf(song("a", "One"), song("c", "Three (feat. B)"))
        val apple = listOf("One" to 1, "Two" to 1, "Three" to 2)
        assertEquals(mapOf("a" to 1, "c" to 2), discNumbers(songs, apple))
        assertEquals(emptyMap<String, Int>(), discNumbers(songs, listOf("One" to 1)))
    }
}
