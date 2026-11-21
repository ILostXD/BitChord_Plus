package com.music.bitchord.data.sources

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UserPlaylist
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The user's own Navidrome / Subsonic library as pages of the app: its albums,
 * artists and playlists open as ordinary release pages under `server:` browse ids,
 * and its playlists sit beside the account's in the add-to-playlist sheet.
 *
 * Nothing is cached here but the playlist list: every page reads the server, so
 * an edit made in Navidrome's own UI (or Glaze, or Feishin) shows on the next visit,
 * and an edit made here is the server's at once — that is the two-way sync.
 */
object ServerLibrary {
    /** Prefix of a server page's browse id: `server:<config id>:<kind>:<server id>`. */
    const val PREFIX = "server:"

    /** Prefix of the Library row that opens one server's library: `app:server:<config id>`. */
    const val ROOT_PREFIX = "app:server:"

    enum class Kind(val key: String) { ALBUM("album"), ARTIST("artist"), PLAYLIST("playlist") }

    data class PageRef(val configId: String, val kind: Kind, val id: String)

    fun pageId(configId: String, kind: Kind, id: String) = "$PREFIX$configId:${kind.key}:$id"

    fun parse(browseId: String?): PageRef? {
        if (browseId == null || !browseId.startsWith(PREFIX)) return null
        val parts = browseId.removePrefix(PREFIX).split(":", limit = 3)
        if (parts.size != 3) return null
        val kind = Kind.entries.firstOrNull { it.key == parts[1] } ?: return null
        return PageRef(parts[0], kind, parts[2])
    }

    fun isPlaylist(browseId: String?) = parse(browseId)?.kind == Kind.PLAYLIST

    /** Whether [url] is a cover served by a music server (getCoverArt). */
    fun isServerCover(url: String?) = url?.contains("/rest/getCoverArt") == true

    /** Every configured server, enabled or not: its library is still the user's to browse. */
    fun servers(): List<SourceConfig> = SourceRegistry.configs.value.filter { it.kind == SourceKind.SUBSONIC }

    fun source(configId: String): SubsonicSource? =
        SourceRegistry.instance(configId) as? SubsonicSource
            ?: SourceRegistry.config(configId)?.takeIf { it.kind == SourceKind.SUBSONIC }?.let(::SubsonicSource)

    /** Every server's playlists, for the add-to-playlist sheet. Filled by [refreshPlaylists]. */
    val playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())

    suspend fun refreshPlaylists() {
        playlists.value = servers().flatMap { config ->
            val server = source(config.id) ?: return@flatMap emptyList()
            runCatching { server.playlists() }.getOrDefault(emptyList()).map {
                UserPlaylist(
                    playlistId = pageId(config.id, Kind.PLAYLIST, it.id),
                    title = it.title,
                    subtitle = server.displayName,
                    thumbnailUrl = it.coverUrl,
                )
            }
        }
    }

    /**
     * [song]'s track id on server [configId]: its own when it already plays from
     * there, else the server's copy found by title and artist. Null when the server
     * doesn't have it.
     */
    suspend fun trackIdOn(configId: String, song: Song): String? {
        SourceRegistry.parseTrackKey(song.videoId)?.let { (config, id) -> if (config == configId) return id }
        return source(configId)?.match(song.title, song.artist)
    }

    /** The server and track id behind a track that plays from a server, if [song] is one. */
    fun serverTrack(song: Song): Pair<SubsonicSource, String>? {
        val (config, id) = SourceRegistry.parseTrackKey(song.videoId) ?: return null
        return source(config)?.let { it to id }
    }
}
