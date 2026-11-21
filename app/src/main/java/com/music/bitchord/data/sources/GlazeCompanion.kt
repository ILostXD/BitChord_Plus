package com.music.bitchord.data.sources

import com.music.bitchord.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Glaze Companion, beside the user's Navidrome: it downloads a release through
 * slskd or Qobuz, checks it is the right edition against the Deezer / MusicBrainz
 * tracklist, moves it into the library and rescans — so it shows up as one of the
 * server's own albums. See GlazeCompanion's API.md, "Automatic acquisition".
 *
 * Authenticated with the server login BitChord already keeps for that Navidrome
 * (the salted token, sent as headers — never in the URL), so no companion admin
 * token ever reaches the phone.
 */
object GlazeCompanion {
    /** A download job. [status]: searching, downloading, moved, rescanned, needs_review or failed. */
    data class Job(val id: String, val status: String, val error: String? = null, val summary: String? = null) {
        /** In the library: downloaded, or it already was (or the user chose to keep what they have). */
        val finished get() = status == "rescanned" || status == "in_library" || status == "skipped"

        /** Waiting for the user: the library has something close, and the companion asks before adding more. */
        val review get() = status == "needs_review" && error == "library_review"
        val failed get() = status == "failed" || (status == "needs_review" && !review)
        val active get() = !finished && !failed && !review
    }

    /** A job as the companion lists it: what was asked for, and how it went. */
    data class JobInfo(
        val job: Job,
        val artist: String,
        val title: String,
        val kind: String,
        val album: String = "",
        val canApprove: Boolean = false,
        /** ISO time of the last change, for ordering. */
        val updatedAt: String = "",
    )

    /** The first music server with a companion address set in Sources. */
    fun server(): SourceConfig? = ServerLibrary.servers().firstOrNull { it.companionUrl.isNotBlank() }

    /** Asks for [title] by [artist] (`kind` album or track) to be downloaded into the library. */
    suspend fun submit(
        server: SourceConfig,
        artist: String,
        title: String,
        kind: String = "album",
        album: String? = null,
        durationSec: Int = 0,
    ): Job =
        call(server, "/api/v1/acquisition/jobs", buildJsonObject {
            put("artist", artist)
            put("title", title)
            put("kind", kind)
            // Lets the companion tell this cut of a song from the one the library has.
            if (durationSec > 0) put("duration_sec", durationSec)
            // A track keeps the release it was picked from, so the right edition is fetched.
            if (kind == "track" && !album.isNullOrBlank()) put("album", album)
        }).toJob()

    /** Retries a failed job with its original request. */
    suspend fun retry(server: SourceConfig, jobId: String): Job =
        call(server, "/api/v1/acquisition/jobs", buildJsonObject { put("retry_id", jobId) }).toJob()

    /** Imports a download whose tags did not match, after the user has seen the review. */
    suspend fun approve(server: SourceConfig, jobId: String): Job =
        call(server, "/api/v1/acquisition/jobs", buildJsonObject {
            put("retry_id", jobId)
            put("approve_metadata", true)
        }).toJob()

    /** Answers a job that is waiting on the user: download it anyway, or keep what the library has. */
    suspend fun decide(server: SourceConfig, jobId: String, download: Boolean): Job =
        call(server, "/api/v1/acquisition/jobs/$jobId/decision", buildJsonObject { put("action", if (download) "download" else "skip") }).toJob()

    suspend fun job(server: SourceConfig, jobId: String): Job =
        call(server, "/api/v1/acquisition/jobs/$jobId", null).toJob()

    /** An upcoming album from a starred artist, or one Pre-Saved. Downloaded on release day when [saved]. */
    data class Release(
        val id: String,
        val title: String,
        val artist: String,
        /** ISO date. The time of day isn't announced, so the release is counted to local midnight. */
        val releaseDate: String,
        val cover: String?,
        val saved: Boolean,
        val status: String?,
        val trackCount: Int = 0,
        val tracks: List<ReleaseTrack> = emptyList(),
    )

    data class ReleaseTrack(val id: String, val title: String, val artist: String, val durationSec: Int?, val released: Boolean)

    /** The page id an upcoming release opens under. */
    const val RELEASE_PREFIX = "release:"

    /** Upcoming releases for the Home shelf and the release page; filled by [refreshUpcoming]. */
    val upcoming = kotlinx.coroutines.flow.MutableStateFlow<List<Release>>(emptyList())

    /**
     * The artists in the YouTube Music library (its Artists and Subscriptions shelves).
     * Those that are also on the music server have their upcoming albums listed too,
     * without having to be starred there first.
     */
    @Volatile var libraryArtists: List<String> = emptyList()

    /** Library artist name → server artist id ("" when the server hasn't got them). */
    private val serverArtistIds = java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun refreshUpcoming() {
        val server = server() ?: run { upcoming.value = emptyList(); return }
        // The companion's own list: starred artists and every Pre-Save.
        val own = runCatching { releases(server) }.getOrElse { return }
        val source = ServerLibrary.source(server.id)
        val ids = libraryArtists.distinct().mapNotNull { name ->
            serverArtistIds[name] ?: source?.let { runCatching { it.findArtist(name)?.id.orEmpty() }.getOrNull() }
                ?.also { serverArtistIds[name] = it }
        }.filter { it.isNotEmpty() }.distinct()
        // A few at a time: the companion checks the catalogue for each one it hasn't lately.
        val gate = kotlinx.coroutines.sync.Semaphore(4)
        val fromLibrary = kotlinx.coroutines.coroutineScope {
            ids.map { id ->
                async { gate.withPermit { runCatching { releases(server, id) }.getOrDefault(emptyList()) } }
            }.flatMap { it.await() }
        }
        // Only what is still to come (or comes today), and not what the library already has.
        val today = java.time.LocalDate.now().toString()
        val due = (own + fromLibrary).distinctBy { it.id }.filter { it.releaseDate.take(10) >= today }.sortedBy { it.releaseDate }
        upcoming.value = due.filterNot { release ->
            source != null && runCatching { source.findAlbum(release.title, release.artist) != null }.getOrDefault(false)
        }
    }

    /** One release with its tracklist. */
    suspend fun release(server: SourceConfig, releaseId: String): Release? =
        (send(server, "/api/v1/releases/$releaseId", null, "GET") as? JsonObject)?.toRelease()

    /** Replaces [release] in [upcoming], after a Pre-Save changed it. */
    fun updated(release: Release) {
        upcoming.value = upcoming.value.map { if (it.id == release.id) release.copy(tracks = it.tracks) else it }
    }

    /** Upcoming albums by the server's starred artists, plus everything Pre-Saved. */
    suspend fun releases(server: SourceConfig, artistId: String? = null): List<Release> =
        (send(server, "/api/v1/releases" + (artistId?.let { "?artist_id=" + java.net.URLEncoder.encode(it, "UTF-8") } ?: ""), null, "GET") as? JsonArray)
            .orEmpty().mapNotNull { (it as? JsonObject)?.toRelease() }

    /** Pre-Saves [releaseId] (or undoes it): the companion downloads it into the library once it is out. */
    suspend fun setPreSaved(server: SourceConfig, releaseId: String, saved: Boolean): Release? =
        (send(server, "/api/v1/releases/$releaseId/presave", buildJsonObject { put("saved", saved) }, "PUT") as? JsonObject)?.toRelease()

    private fun JsonObject.toRelease(): Release? = Release(
        id = text("id") ?: return null,
        title = text("title") ?: return null,
        artist = text("artist").orEmpty(),
        releaseDate = text("releaseDate").orEmpty(),
        cover = text("cover")?.takeIf { it.isNotBlank() },
        saved = (this["saved"] as? JsonPrimitive)?.booleanOrNull == true,
        status = text("status")?.takeIf { it.isNotBlank() },
        trackCount = (this["trackCount"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0,
        tracks = (this["tracks"] as? JsonArray).orEmpty().mapNotNull { element ->
            val t = element as? JsonObject ?: return@mapNotNull null
            ReleaseTrack(
                id = t.text("id") ?: return@mapNotNull null,
                title = t.text("title") ?: return@mapNotNull null,
                artist = t.text("artist").orEmpty(),
                durationSec = t.text("duration")?.toIntOrNull(),
                released = (t["released"] as? JsonPrimitive)?.booleanOrNull == true,
            )
        },
    )

    private suspend fun call(server: SourceConfig, path: String, body: JsonObject?): JsonObject =
        send(server, path, body, if (body == null) "GET" else "POST") as? JsonObject
            ?: throw IllegalStateException("Not a companion response")

    private suspend fun send(server: SourceConfig, path: String, body: JsonObject?, method: String): JsonElement = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(server.companionUrl.trim().trimEnd('/') + path)
            .header("X-Jam-Navidrome-User", server.username)
            .header("X-Jam-Navidrome-Salt", server.authSalt)
            .header("X-Jam-Navidrome-Token", server.authToken)
            .method(method, body?.toString()?.toRequestBody("application/json".toMediaType()))
            .build()
        Http.client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val json = runCatching { Json.parseToJsonElement(text) }.getOrNull()
            if (!response.isSuccessful) {
                throw IllegalStateException((json as? JsonObject)?.text("error") ?: "HTTP ${response.code}")
            }
            json ?: throw IllegalStateException("Not a companion response")
        }
    }

    /** Every job the companion knows, newest first. */
    suspend fun jobs(server: SourceConfig): List<JobInfo> =
        (send(server, "/api/v1/acquisition/jobs", null, "GET") as? JsonArray).orEmpty().mapNotNull { element ->
            (element as? JsonObject)?.let {
                runCatching {
                    JobInfo(
                        it.toJob(), it.text("Artist").orEmpty(), it.text("Title").orEmpty(), it.text("Kind").orEmpty(),
                        it.text("AlbumTitle").orEmpty(), (it["CanApproveMetadata"] as? JsonPrimitive)?.booleanOrNull == true,
                        it.text("UpdatedAt").orEmpty(),
                    )
                }.getOrNull()
            }
        }

    private fun JsonObject.toJob() = Job(
        id = text("ID") ?: text("id") ?: throw IllegalStateException("No job id"),
        status = text("Status") ?: text("status") ?: "searching",
        error = (text("Error") ?: text("error"))?.takeIf { it.isNotBlank() },
        summary = text("ReviewSummary")?.takeIf { it.isNotBlank() },
    )

    private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
}
