package com.music.bitchord.data.sources

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
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

    /** Song → (server config id, track id) on the server, or null when it hasn't got it. */
    private val copies = java.util.concurrent.ConcurrentHashMap<String, Pair<String, String>?>()

    /** Where [song] is on your server: itself for a server track, else the server's copy of it. */
    suspend fun serverCopy(song: Song): Pair<String, String>? {
        // A source track belongs to its own source: a server's, or a module's (which has no plays to count).
        SourceRegistry.parseTrackKey(song.videoId)?.let { return it.takeIf { ServerLibrary.source(it.first) != null } }
        if (song.localUri != null) return null
        if (copies.containsKey(song.videoId)) return copies[song.videoId]
        val config = main() ?: return null
        val id = runCatching { ServerLibrary.source(config.id)?.match(song.title, GlazeCompanion.cleanArtist(song.artist)) }
            .getOrElse { return null }
        return id?.let { config.id to it }.also { copies[song.videoId] = it }
    }

    /** Whether Navidrome passes [song]'s play on to Last.fm and ListenBrainz, so the app shouldn't. */
    suspend fun forwardedByServer(song: Song): Boolean =
        AppSettings.navidromeForwardsScrobbles.value && serverCopy(song) != null

    /**
     * [song] counted as a play. A server track scrobbles to its own server; a YouTube Music
     * song scrobbles to your server's copy of it, if it has one.
     */
    fun played(song: Song, startedAtMs: Long) {
        scope.launch {
            runCatching {
                val (config, id) = serverCopy(song) ?: return@runCatching
                ServerLibrary.source(config)?.scrobble(id, startedAtMs)
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
