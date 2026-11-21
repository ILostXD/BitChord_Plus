package com.music.bitchord

import com.music.bitchord.data.sources.ServerLibrary
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.sources.SubsonicSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.MessageDigest

class SubsonicSourceTest {
    @Test
    fun tokenIsMd5OfPasswordPlusSalt() {
        val (token, salt) = SubsonicSource.tokenFor("sesame")
        val expected = MessageDigest.getInstance("MD5").digest(("sesame$salt").toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, token)
        assertEquals(24, salt.length)
        assertNotEquals(salt, SubsonicSource.tokenFor("sesame").second)
    }

    @Test
    fun baseUrlDropsRestAndSlashAndDefaultsToHttps() {
        assertEquals("https://music.example.com", SourceRegistry.subsonicBase("music.example.com/rest/"))
        assertEquals("http://192.168.1.5:4533", SourceRegistry.subsonicBase(" http://192.168.1.5:4533/ "))
    }

    @Test
    fun serverPageIdsRoundTrip() {
        val id = ServerLibrary.pageId("cfg-1", ServerLibrary.Kind.PLAYLIST, "ab:cd")
        assertEquals(ServerLibrary.PageRef("cfg-1", ServerLibrary.Kind.PLAYLIST, "ab:cd"), ServerLibrary.parse(id))
        assertEquals(null, ServerLibrary.parse("VLPL123"))
        assertEquals(id, com.music.bitchord.data.model.UserPlaylist(id, "", "", null).browseId)
    }

    @Test
    fun matchPrefersSameTitleAndArtist() {
        val hits = listOf(
            Triple("1", "SICKO MODE (Remix)", "Travis Scott"),
            Triple("2", "Sicko Mode", "Someone Else"),
            Triple("3", "SICKO MODE", "Travis Scott, Drake"),
        )
        assertEquals("3", SubsonicSource.matchIn(hits, "SICKO MODE", "Travis Scott & Drake"))
        assertEquals("2", SubsonicSource.matchIn(hits.take(2), "Sicko Mode", "Nobody"))
        assertEquals(null, SubsonicSource.matchIn(hits, "Butterfly Effect", "Travis Scott"))
    }
}
