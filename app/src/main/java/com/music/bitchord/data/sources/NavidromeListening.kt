package com.music.bitchord.data.sources

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.stats.Taste
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Listening shared with your Navidrome, both ways: what you play here counts there (a
 * scrobble, as other Subsonic apps send), and what it has counted from every device
 * feeds the smart queue.
 */
object NavidromeListening {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshing = AtomicBoolean(false)

    /** The server your listening belongs to: the one with the companion, else the first. */
    private fun main(): SourceConfig? = GlazeCompanion.server() ?: ServerLibrary.servers().firstOrNull()

    /**
     * [song] counted as a play. A server track scrobbles to its own server; a YouTube Music
     * song scrobbles to your server's copy of it, if it has one.
     */
    fun played(song: Song, startedAtMs: Long) {
        scope.launch {
            runCatching {
                val key = SourceRegistry.parseTrackKey(song.videoId)
                if (key != null) {
                    ServerLibrary.source(key.first)?.scrobble(key.second, startedAtMs)
                } else if (song.localUri == null) {
                    val source = main()?.let { ServerLibrary.source(it.id) } ?: return@runCatching
                    val id = source.match(song.title, GlazeCompanion.cleanArtist(song.artist)) ?: return@runCatching
                    source.scrobble(id, startedAtMs)
                }
            }
        }
    }

    /** Reads the server's play counts into [Taste], at most once a day. */
    fun refreshTaste() {
        if (System.currentTimeMillis() - Taste.state.serverAt < 24 * 3_600_000L) return
        val source = main()?.let { ServerLibrary.source(it.id) } ?: return
        if (!refreshing.compareAndSet(false, true)) return
        scope.launch {
            try {
                runCatching { source.playCounts() }.onSuccess { Taste.setServer(it, System.currentTimeMillis()) }
            } finally {
                refreshing.set(false)
            }
        }
    }
}
