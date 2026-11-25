package com.music.bitchord.data.sources

import com.music.bitchord.data.Http
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "BitChord"

/**
 * A Navidrome / Subsonic / OpenSubsonic server (Gonic, Airsonic, Ampache, Jellyfin's
 * Subsonic plugin…) used as a stream source: a YouTube Music track is matched against
 * the user's own library and played from their own file, lossless when the file is.
 *
 * No addon in between — the app speaks the Subsonic REST API itself. Auth is the
 * salted-token scheme (`t = md5(password + salt)`), so only the token and salt are
 * stored, never the password; see [SourceConfig.authToken].
 */
class SubsonicSource(
    override val config: SourceConfig,
) : MusicSource, SourceRegistry.ConfigBacked {

    override val configId: String get() = config.id
    override val kind: SourceKind get() = SourceKind.SUBSONIC
    override val displayName: String
        get() = config.label.ifBlank { config.baseUrl.toHttpUrlOrNull()?.host ?: SourceKind.SUBSONIC.label }

    /** Search rows by Subsonic song id, so [stream] knows the file's format without asking again. */
    private val rows = ConcurrentHashMap<String, JsonObject>()

    override suspend fun health(): SourceHealth = runCatching {
        request("ping")
        SourceHealth.Ok()
    }.getOrElse { it.toHealth() }

    /**
     * Which server this is: OpenSubsonic servers name themselves in `type`
     * (`navidrome`, `gonic`, `ampache`…); Jellyfin's plugin doesn't, so its own
     * public info endpoint is asked. Blank when neither says.
     */
    suspend fun detectServerType(): String {
        runCatching { request("ping").text("type") }.getOrNull()
            ?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() }?.let { return it }
        return runCatching {
            withContext(Dispatchers.IO) {
                val url = config.baseUrl.trimEnd('/') + "/System/Info/Public"
                Http.client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    val product = response.body?.string()?.let { body ->
                        (json.parseToJsonElement(body).jsonObject["ProductName"] as? JsonPrimitive)?.contentOrNull
                    }
                    if (product?.contains("jellyfin", ignoreCase = true) == true) "jellyfin" else ""
                }
            }
        }.getOrDefault("")
    }

    override suspend fun search(
        query: String,
        limit: Int,
        waitForAll: Boolean,
        request: StreamRequest?,
        target: TrackMatcher.Target?,
    ): List<Song> {
        val body = runCatching {
            request("search3", "query" to query, "songCount" to limit.toString(), "artistCount" to "0", "albumCount" to "0")
        }.getOrElse {
            TrackLog.w(TAG, "  ✗ $displayName search failed: ${it.message}")
            return emptyList()
        }
        val songs = (body["searchResult3"] as? JsonObject)?.get("song") as? JsonArray ?: return emptyList()
        return songs.mapNotNull { (it as? JsonObject)?.toSong() }.take(limit)
    }

    override suspend fun stream(trackId: String, request: StreamRequest): SourceStream? {
        val row = rows[trackId] ?: runCatching {
            request("getSong", "id" to trackId)["song"] as? JsonObject
        }.getOrNull()?.also { rows[trackId] = it }
        val suffix = row?.text("suffix")?.lowercase(Locale.ROOT)
        val bitRate = row?.int("bitRate")
        val original = StreamFormat(
            codec = suffix,
            kbps = bitRate,
            sampleRateHz = row?.int("samplingRate"),
            bitDepth = row?.int("bitDepth"),
        )
        // Over a capped connection, a file above the cap is transcoded by the server.
        val transcode = request is StreamRequest.Capped && (bitRate == null || bitRate > request.maxKbps)
        val url = if (transcode) {
            val cap = (request as StreamRequest.Capped).maxKbps
            url("stream", "id" to trackId, "format" to "mp3", "maxBitRate" to cap.toString())
        } else {
            url("stream", "id" to trackId, "format" to "raw")
        }
        val format = if (transcode) {
            StreamFormat(codec = "mp3", kbps = (request as StreamRequest.Capped).maxKbps, kbpsInferred = true)
        } else original
        TrackLog.d(TAG, "  ✓ $displayName stream $trackId ${format.codec ?: "?"} ${format.kbps ?: "?"}kbps")
        return SourceStream(
            url = url,
            format = format,
            belowRequest = request is StreamRequest.Lossless && format.isLossless == false,
            durationSec = row?.int("duration"),
        )
    }

    // ── Library: what the server itself holds, for the server library page ──

    /** One album, artist or playlist card. [starred] is the server's favourite flag. */
    data class Entry(
        val id: String,
        val title: String,
        val subtitle: String,
        val coverUrl: String?,
        val year: Int? = null,
        val artistId: String? = null,
        val starred: Boolean = false,
    )

    /** An album, artist or playlist with its tracks (an artist: its albums in [albums]). */
    data class Listing(
        val entry: Entry,
        val songs: List<Song> = emptyList(),
        /** Server track ids of [songs], in order: playlist edits address tracks by them. */
        val songIds: List<String> = emptyList(),
        /** Which of [songs] are starred on the server, by [Song.videoId]. */
        val starredSongs: Set<String> = emptySet(),
        val albums: List<Entry> = emptyList(),
        val artists: List<Entry> = emptyList(),
    )

    suspend fun artists(): List<Entry> =
        (request("getArtists")["artists"] as? JsonObject)?.array("index")
            ?.flatMap { (it as? JsonObject)?.array("artist").orEmpty() }
            ?.mapNotNull { (it as? JsonObject)?.toArtist() }.orEmpty()

    /** Every album, fetched 500 at a time, in [order] (`alphabeticalByName`, `newest`…). */
    suspend fun albums(order: String = "alphabeticalByName"): List<Entry> {
        val all = mutableListOf<Entry>()
        while (true) {
            val page = (request("getAlbumList2", "type" to order, "size" to "500", "offset" to all.size.toString())
                ["albumList2"] as? JsonObject)?.array("album").orEmpty().mapNotNull { (it as? JsonObject)?.toAlbum() }
            all += page
            if (page.size < 500) return all
        }
    }

    suspend fun playlists(): List<Entry> =
        (request("getPlaylists")["playlists"] as? JsonObject)?.array("playlist")
            ?.mapNotNull { (it as? JsonObject)?.toPlaylist() }.orEmpty()

    /** The server's favourites: songs, albums and artists. */
    suspend fun starred(): Listing {
        val body = request("getStarred2")["starred2"] as? JsonObject
        val songs = body?.array("song")?.mapNotNull { (it as? JsonObject)?.toSong() }.orEmpty()
        return Listing(
            entry = Entry("starred", "", "", null),
            songs = songs,
            starredSongs = songs.mapTo(HashSet()) { it.videoId },
            albums = body?.array("album")?.mapNotNull { (it as? JsonObject)?.toAlbum() }.orEmpty(),
            artists = body?.array("artist")?.mapNotNull { (it as? JsonObject)?.toArtist() }.orEmpty(),
        )
    }

    suspend fun album(id: String): Listing =
        listing(request("getAlbum", "id" to id)["album"] as? JsonObject, "song") { toAlbum() }

    suspend fun playlist(id: String): Listing =
        listing(request("getPlaylist", "id" to id)["playlist"] as? JsonObject, "entry") { toPlaylist() }

    private fun listing(body: JsonObject?, key: String, entry: JsonObject.() -> Entry?): Listing {
        val rows = body?.array(key).orEmpty().mapNotNull { it as? JsonObject }
        val songs = rows.mapNotNull { row -> row.toSong()?.let { row to it } }
        return Listing(
            entry = body?.entry() ?: error("Not found on the server"),
            songs = songs.map { it.second },
            songIds = songs.map { it.first.text("id").orEmpty() },
            starredSongs = songs.filter { it.first.isStarred() }.mapTo(HashSet()) { it.second.videoId },
        )
    }

    suspend fun artist(id: String): Listing {
        val artist = request("getArtist", "id" to id)["artist"] as? JsonObject ?: error("Not found on the server")
        return Listing(
            artist.toArtist() ?: error("Not found on the server"),
            albums = artist.array("album").orEmpty().mapNotNull { (it as? JsonObject)?.toAlbum() }
                .sortedByDescending { it.year ?: 0 },
        )
    }

    /** Stars or unstars a song (`id`), album (`albumId`) or artist (`artistId`). */
    suspend fun setStarred(param: String, id: String, starred: Boolean) {
        request(if (starred) "star" else "unstar", param to id)
    }

    /** A new playlist holding [songIds]; returns its id. */
    suspend fun createPlaylist(name: String, songIds: List<String>): String? =
        (request("createPlaylist", "name" to name, *songIds.map { "songId" to it }.toTypedArray())
            ["playlist"] as? JsonObject)?.text("id")

    /** Replaces [playlistId]'s tracks with [songIds], in order: Subsonic's only way to reorder. */
    suspend fun replacePlaylist(playlistId: String, songIds: List<String>) {
        request("createPlaylist", "playlistId" to playlistId, *songIds.map { "songId" to it }.toTypedArray())
    }

    suspend fun addToPlaylist(playlistId: String, songIds: List<String>) {
        request("updatePlaylist", "playlistId" to playlistId, *songIds.map { "songIdToAdd" to it }.toTypedArray())
    }

    suspend fun removeFromPlaylist(playlistId: String, index: Int) {
        request("updatePlaylist", "playlistId" to playlistId, "songIndexToRemove" to index.toString())
    }

    suspend fun renamePlaylist(playlistId: String, name: String) {
        request("updatePlaylist", "playlistId" to playlistId, "name" to name)
    }

    suspend fun deletePlaylist(playlistId: String) {
        request("deletePlaylist", "id" to playlistId)
    }

    /**
     * This server's copy of [title] by [artist], if it has one: a search hit with the
     * same title (ignoring case and bracketed extras like "(Remastered)"), preferring
     * one whose artist matches too.
     */
    suspend fun match(title: String, artist: String): String? {
        val hits = (request("search3", "query" to "$title $artist".trim(), "songCount" to "10", "artistCount" to "0", "albumCount" to "0")
            ["searchResult3"] as? JsonObject)?.array("song").orEmpty().mapNotNull { it as? JsonObject }
        return matchIn(hits.map { Triple(it.text("id").orEmpty(), it.text("title").orEmpty(), it.text("artist").orEmpty()) }, title, artist)
    }

    /** Tells the server [trackId] was played (at [timeMs]): its play count, "recently played" and any scrobbling it does. */
    suspend fun scrobble(trackId: String, timeMs: Long) {
        request("scrobble", "id" to trackId, "time" to timeMs.toString(), "submission" to "true")
    }

    /**
     * Every song the server has played (title, artist, plays), a star counting as five plays.
     * An empty search3 query lists the whole library, a page at a time.
     */
    suspend fun playCounts(maxSongs: Int = 20_000): List<Triple<String, String, Int>> {
        val out = ArrayList<Triple<String, String, Int>>()
        var offset = 0
        while (offset < maxSongs) {
            val page = (request("search3", "query" to "", "songCount" to "500", "songOffset" to offset.toString(), "artistCount" to "0", "albumCount" to "0")
                ["searchResult3"] as? JsonObject)?.array("song").orEmpty().mapNotNull { it as? JsonObject }
            page.forEach { song ->
                val plays = (song.int("playCount") ?: 0) + if (song.isStarred()) 5 else 0
                if (plays > 0) out += Triple(song.text("title").orEmpty(), song.text("artist").orEmpty(), plays)
            }
            if (page.size < 500) break
            offset += 500
        }
        return out
    }

    /** This server's artist called [name] (ignoring case), if it has one. */
    suspend fun findArtist(name: String): Entry? =
        (request("search3", "query" to name, "songCount" to "0", "albumCount" to "0", "artistCount" to "10")
            ["searchResult3"] as? JsonObject)?.array("artist").orEmpty()
            .mapNotNull { (it as? JsonObject)?.toArtist() }
            .firstOrNull { it.title.trim().equals(name.trim(), ignoreCase = true) }

    /** This server's copy of the album [title] by [artist], matched as [match] matches songs. */
    suspend fun findAlbum(title: String, artist: String): Entry? {
        val hits = (request("search3", "query" to title, "songCount" to "0", "artistCount" to "0", "albumCount" to "20")
            ["searchResult3"] as? JsonObject)?.array("album").orEmpty().mapNotNull { (it as? JsonObject)?.toAlbum() }
        matchIn(hits.map { Triple(it.id, it.title, it.subtitle) }, title, artist)?.let { id -> return hits.firstOrNull { it.id == id } }
        // A title search can't find "?" (a search drops symbols): look through the artist's albums instead.
        if (title.any { it.isLetterOrDigit() }) return null
        val albums = findArtist(artist.split(",", "&", " feat").first().trim())?.let { artist(it.id).albums }.orEmpty()
        val id = matchIn(albums.map { Triple(it.id, it.title, it.subtitle) }, title, artist) ?: return null
        return albums.firstOrNull { it.id == id }
    }

    private fun cover(id: String?) = id?.let { url("getCoverArt", "id" to it, "size" to "600") }

    private fun JsonObject.isStarred() = this["starred"] != null

    private fun JsonObject.toArtist(): Entry? = Entry(
        id = text("id") ?: return null,
        title = text("name") ?: return null,
        subtitle = "",
        coverUrl = text("artistImageUrl")?.takeIf { it.startsWith("http") } ?: cover(text("coverArt")),
        starred = isStarred(),
    )

    private fun JsonObject.toAlbum(): Entry? = Entry(
        id = text("id") ?: return null,
        title = text("name") ?: text("title") ?: return null,
        subtitle = text("artist").orEmpty(),
        coverUrl = cover(text("coverArt")),
        year = int("year"),
        artistId = text("artistId"),
        starred = isStarred(),
    )

    private fun JsonObject.toPlaylist(): Entry? = Entry(
        id = text("id") ?: return null,
        title = text("name") ?: return null,
        subtitle = text("owner").orEmpty(),
        coverUrl = cover(text("coverArt")),
    )

    private fun JsonObject.array(key: String) = this[key] as? JsonArray

    private fun JsonObject.toSong(): Song? {
        val id = text("id") ?: return null
        val title = text("title") ?: return null
        rows[id] = this
        val suffix = text("suffix")?.lowercase(Locale.ROOT)
        val lossless = suffix in LOSSLESS_SUFFIXES
        return Song(
            videoId = SourceRegistry.trackKey(config.id, id),
            title = title,
            artist = text("artist").orEmpty(),
            albumName = text("album"),
            // Song menus' "Go to album / artist" open the server's own pages.
            albumId = text("albumId")?.let { ServerLibrary.pageId(config.id, ServerLibrary.Kind.ALBUM, it) },
            artistId = text("artistId")?.let { ServerLibrary.pageId(config.id, ServerLibrary.Kind.ARTIST, it) },
            thumbnailUrl = cover(text("coverArt")),
            durationText = int("duration")?.let { "${it / 60}:${"%02d".format(Locale.ROOT, it % 60)}" },
            sourceQuality = if (lossless) ModuleSource.LOSSLESS else "HIGH",
            sourceHiRes = lossless && ((int("bitDepth") ?: 0) > 16 || (int("samplingRate") ?: 0) > 48_000),
        )
    }

    /** A signed REST URL. The token in it is the stored one, never the password. */
    private fun url(endpoint: String, vararg params: Pair<String, String>): String {
        val base = config.baseUrl.trimEnd('/').toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid server address")
        return base.newBuilder()
            .addPathSegment("rest").addPathSegment(endpoint)
            .addQueryParameter("u", config.username)
            .addQueryParameter("t", config.authToken)
            .addQueryParameter("s", config.authSalt)
            .addQueryParameter("v", API_VERSION)
            .addQueryParameter("c", CLIENT)
            .addQueryParameter("f", "json")
            .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
            .build().toString()
    }

    /** The `subsonic-response` body of a successful call; throws on transport or API failure. */
    private suspend fun request(endpoint: String, vararg params: Pair<String, String>): JsonObject =
        withContext(Dispatchers.IO) {
            val text = Http.client.newCall(Request.Builder().url(url(endpoint, *params)).build()).execute().use {
                if (!it.isSuccessful) throw SubsonicException("HTTP ${it.code}", rejected = it.code == 401 || it.code == 403)
                it.body?.string().orEmpty()
            }
            val body = runCatching { json.parseToJsonElement(text).jsonObject["subsonic-response"]?.jsonObject }
                .getOrNull() ?: throw SubsonicException("Not a Subsonic server", rejected = true)
            if (body.text("status") != "ok") {
                val error = body["error"] as? JsonObject
                // 40/41 = wrong credentials or token auth unsupported (needs the server's plain-password mode).
                throw SubsonicException(error?.text("message") ?: "Server refused the request", rejected = true)
            }
            body
        }

    private fun Throwable.toHealth(): SourceHealth = when {
        this is SubsonicException && rejected -> SourceHealth.Rejected(message ?: "Rejected")
        else -> SourceHealth.Unreachable(message ?: "Unreachable")
    }

    fun release() = rows.clear()

    private class SubsonicException(message: String, val rejected: Boolean) : Exception(message)

    companion object {
        private const val API_VERSION = "1.16.1"
        private const val CLIENT = "BitChord"
        private val LOSSLESS_SUFFIXES = setOf("flac", "alac", "wav", "aiff", "aif", "ape", "wv", "dsf", "dff")
        private val json = Json { ignoreUnknownKeys = true }

        /** A fresh salt and its token for [password]; the password itself is never stored. */
        fun tokenFor(password: String): Pair<String, String> {
            val salt = ByteArray(12).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
            val token = MessageDigest.getInstance("MD5").digest((password + salt).toByteArray())
                .joinToString("") { "%02x".format(it) }
            return token to salt
        }

        /** The id among [hits] (id, title, artist) that is [title] by [artist]; see [match]. */
        internal fun matchIn(hits: List<Triple<String, String, String>>, title: String, artist: String): String? {
            fun norm(s: String) = s.lowercase(Locale.ROOT).replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "").trim()
            val lead = norm(artist).split(",", "&", " feat").first().trim()
            fun pick(same: (String) -> Boolean): Triple<String, String, String>? {
                val sameTitle = hits.filter { same(it.second) }
                return sameTitle.firstOrNull { lead.isNotEmpty() && norm(it.third).contains(lead) } ?: sameTitle.firstOrNull()
            }
            // The exact title first, so "Song (Remix)" never stands in for "Song" when both are there.
            return (pick { it.trim().equals(title.trim(), ignoreCase = true) } ?: pick { norm(it) == norm(title) })
                ?.first?.takeIf { it.isNotEmpty() }
        }

        private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
        private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.intOrNull
    }
}
