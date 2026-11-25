package com.music.bitchord.data.stats

import android.content.Context
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.durationMillis
import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.playback.QueueBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * What the smart queue knows about your taste, learnt from how you listen (after Glaze).
 *
 * Every track that ends is scored: finished counts for it, an early skip counts against it
 * (heavily when AutoPlay chose it, lightly when you did), and everything fades with a 45-day
 * half-life so old habits give way. Artists collect half of their songs' scores. Navidrome's
 * play counts and stars add what you play on other devices. It all stays on the phone.
 */
object Taste {
    @Serializable
    data class Track(
        val id: String, val title: String, val artist: String,
        val thumbnail: String? = null, val duration: String? = null, val album: String? = null,
        val score: Double = 0.0, val skips: Double = 0.0, val at: Long = 0, val heard: Long = 0,
    )

    @Serializable
    data class Artist(val score: Double = 0.0, val at: Long = 0)

    @Serializable
    data class State(
        val tracks: Map<String, Track> = emptyMap(),
        val artists: Map<String, Artist> = emptyMap(),
        /** Navidrome: plays (plus a bonus for a star) by track key and by artist. */
        val serverTracks: Map<String, Int> = emptyMap(),
        val serverArtists: Map<String, Int> = emptyMap(),
        val serverAt: Long = 0,
    )

    private const val HALF_LIFE_MS = 45L * 86_400_000
    private const val MAX_TRACKS = 4000
    private val json = Json { ignoreUnknownKeys = true }
    // One at a time, in order: a song's pending file must not outlive its finish.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private var file: File? = null

    @Volatile var state = State()
        private set

    fun init(context: Context) {
        val f = File(context.filesDir, "taste.json").also { file = it }
        state = runCatching { json.decodeFromString<State>(f.readText()) }.getOrDefault(State())
        // The song that was playing when the app last closed: scored now, as stopped rather than skipped.
        val p = File(context.filesDir, "taste-pending.json").also { pendingFile = it }
        runCatching { json.decodeFromString<Pending>(p.readText()) }.getOrNull()?.let { pending ->
            val song = pending.track.toSong().copy(queueTier = if (pending.automatic) QueueTier.AUTOPLAY else QueueTier.CONTEXT)
            state = learn(state, song, pending.played, pending.length, pending.at, stopped = true)
            save()
        }
        p.delete()
    }

    /** The song being listened to, kept on disk so closing the app doesn't lose it. */
    @Serializable
    data class Pending(val track: Track, val automatic: Boolean, val played: Long, val length: Long, val at: Long)

    private var pendingFile: File? = null

    /** Writes the current song's listening so far; [ListeningRecorder] calls it on pause and every half minute. */
    @Synchronized
    fun flushPending() {
        val song = current ?: return
        val f = pendingFile ?: return
        val pending = Pending(Track(song.videoId, song.title, song.artist, song.thumbnailUrl, song.durationText, song.albumName),
            song.queueTier == QueueTier.AUTOPLAY, played, length, System.currentTimeMillis())
        val text = json.encodeToString(Pending.serializer(), pending)
        writer.launch { runCatching { File(f.path + ".tmp").apply { writeText(text) }.renameTo(f) } }
    }

    private fun Track.toSong() = Song(videoId = id, title = title, artist = artist, thumbnailUrl = thumbnail,
        durationText = duration, albumName = album)

    private fun save() {
        val f = file ?: return
        val text = json.encodeToString(State.serializer(), state)
        writer.launch { runCatching { File(f.path + ".tmp").apply { writeText(text) }.renameTo(f) } }
    }

    fun key(title: String, artist: String) =
        QueueBuilder.normalisedTitle(title) + "|" + (QueueBuilder.artistSet(artist).firstOrNull() ?: "")

    fun artistKey(artist: String) = QueueBuilder.artistSet(artist).firstOrNull().orEmpty()

    fun decayed(value: Double, at: Long, now: Long) =
        value * 0.5.pow((now - at).coerceAtLeast(0).toDouble() / HALF_LIFE_MS)

    // ── Learning ──────────────────────────────────────────────────────────────

    private var current: Song? = null
    private var played = 0L
    private var length = 0L

    /** One tick of real listening to [song], from [ListeningRecorder]. A new song ends the last one. */
    @Synchronized
    fun listened(song: Song, stepMs: Long, lengthMs: Long) {
        if (song.videoId != current?.videoId) {
            current?.let { finish(it, played, length, System.currentTimeMillis()) }
            current = song
            played = 0L
        }
        played += stepMs
        if (lengthMs > 0) length = lengthMs
    }

    private fun finish(song: Song, listenedMs: Long, lengthMs: Long, now: Long) {
        state = learn(state, song, listenedMs, lengthMs.takeIf { it > 0 } ?: song.durationMillis(), now)
        save()
        pendingFile?.let { f -> writer.launch { f.delete() } }
    }

    /** [state] after [song] ended having played for [listenedMs] of [lengthMs]. */
    fun learn(state: State, song: Song, listenedMs: Long, lengthMs: Long, now: Long, stopped: Boolean = false): State {
        if (listenedMs <= 0) return state
        val automatic = song.queueTier == QueueTier.AUTOPLAY
        // Closing the app isn't a skip.
        val earlySkip = !stopped && listenedMs < 30_000 && lengthMs > 60_000
        val completed = lengthMs > 0 && listenedMs >= lengthMs * 8 / 10
        val qualified = listenedMs >= min(if (lengthMs > 0) lengthMs / 2 else 30_000, 4 * 60_000L)
        val skip = if (earlySkip) (if (automatic) 1.0 else 0.2) else 0.0
        val finish = if (completed) 1.0 else if (qualified) 0.35 else 0.0
        val deliberate = if (!automatic && listenedMs >= 10_000) 0.15 else 0.0
        val delta = finish * 0.45 + deliberate - skip * 1.2

        val key = key(song.title, song.artist)
        val old = state.tracks[key]
        val track = Track(
            id = song.videoId, title = song.title, artist = song.artist,
            thumbnail = song.thumbnailUrl, duration = song.durationText, album = song.albumName,
            score = (old?.let { decayed(it.score, it.at, now) } ?: 0.0) + delta,
            skips = (old?.let { decayed(it.skips, it.at, now) } ?: 0.0) + skip,
            at = now, heard = now,
        )
        val tracks = (state.tracks + (key to track)).let { all ->
            if (all.size <= MAX_TRACKS) all else all.entries.sortedByDescending { it.value.at }.take(MAX_TRACKS).associate { it.toPair() }
        }
        val artistKey = artistKey(song.artist)
        val artists = if (artistKey.isEmpty()) state.artists else {
            val a = state.artists[artistKey]
            state.artists + (artistKey to Artist((a?.let { decayed(it.score, it.at, now) } ?: 0.0) + delta * 0.5, now))
        }
        return state.copy(tracks = tracks, artists = artists)
    }

    /** Navidrome's view of your listening: plays and stars, from every device that plays from it. */
    fun setServer(counts: List<Triple<String, String, Int>>, now: Long) {
        val tracks = HashMap<String, Int>()
        val artists = HashMap<String, Int>()
        counts.forEach { (title, artist, plays) ->
            if (plays <= 0) return@forEach
            tracks.merge(key(title, artist), plays, Int::plus)
            artistKey(artist).takeIf { it.isNotEmpty() }?.let { artists.merge(it, plays, Int::plus) }
        }
        state = state.copy(serverTracks = tracks, serverArtists = artists, serverAt = now)
        save()
    }

    // ── Choosing ──────────────────────────────────────────────────────────────

    /** How much more (or less) likely [song] is to be picked: 1 is neutral. */
    fun weight(state: State, song: Song, now: Long): Double {
        val key = key(song.title, song.artist)
        var w = 1.0
        state.tracks[key]?.let { w *= exp(decayed(it.score, it.at, now).coerceIn(-1.5, 0.8)) }
        state.artists[artistKey(song.artist)]?.let { w *= exp(0.5 * decayed(it.score, it.at, now).coerceIn(-1.0, 1.0)) }
        state.serverTracks[key]?.let { w *= 1 + min(0.15 * ln(1.0 + it), 0.6) }
        state.serverArtists[artistKey(song.artist)]?.let { w *= 1 + min(0.08 * ln(1.0 + it), 0.4) }
        return w.coerceIn(0.2, 3.0)
    }

    /** Skipped twice lately when AutoPlay offered it, or heard in the last three hours. */
    fun unwanted(state: State, song: Song, now: Long): Boolean {
        val t = state.tracks[key(song.title, song.artist)] ?: return false
        return decayed(t.skips, t.at, now) >= 1.6 || now - t.heard < 3 * 3_600_000L
    }

    /**
     * YouTube Music's radio [candidates], in an order that fits you. Its own order still counts
     * (it is how related each song is), songs you skip drop out, and every few songs one of
     * your favourites by an artist in this radio comes back, if you haven't heard it in two days.
     */
    fun smartQueue(candidates: List<Song>, state: State = this.state, now: Long = System.currentTimeMillis(), random: Random = Random.Default): List<Song> {
        if (candidates.size < 3) return candidates
        val kept = candidates.filterNot { unwanted(state, it, now) }.takeIf { it.size >= candidates.size / 2 } ?: candidates
        val ordered = kept.withIndex().sortedBy { (i, song) ->
            // Exponential race: a weighted shuffle without replacement, leaning on the radio's order.
            -ln(random.nextDouble().coerceAtLeast(Double.MIN_VALUE)) / (weight(state, song, now) / (1 + i * 0.04))
        }.map { it.value }.toMutableList()

        val artists = candidates.map { artistKey(it.artist) }.filter { it.isNotEmpty() }.toSet()
        val present = candidates.map { key(it.title, it.artist) }.toSet()
        val favourites = state.tracks.entries
            .filter { (k, t) -> k !in present && artistKey(t.artist) in artists && now - t.heard > 2 * 86_400_000L }
            .map { it.value to decayed(it.value.score, it.value.at, now) }
            .filter { it.second > 0.5 }
            .sortedByDescending { it.second }
            .take(ordered.size / 6)
            .shuffled(random)
        favourites.forEachIndexed { n, (t, _) ->
            ordered.add((3 + n * 6).coerceAtMost(ordered.size), t.toSong())
        }
        return ordered
    }
}
