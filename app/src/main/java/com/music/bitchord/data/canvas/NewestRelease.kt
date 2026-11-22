package com.music.bitchord.data.canvas

import com.music.bitchord.data.Http
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * An artist's newest release of any kind (album, EP or single, their own or a joint one).
 * A YouTube artist page only gives years and may not list collabs at all, so Deezer, which
 * dates every release to the day and lists them newest first, says which one it is; the
 * YouTube copy is then the page's own card, or found by a search.
 */
object NewestRelease {
    private data class Cached(val item: ShelfItem?, val at: Long)

    private val cache = ConcurrentHashMap<String, Cached>()
    private const val TTL_MS = 6 * 60 * 60 * 1000L

    /** The newest release, as a YouTube Music card; null when it can't be told in a few seconds. */
    suspend fun find(artist: String, pageItems: List<ShelfItem>): ShelfItem? {
        val cacheKey = key(artist)
        cache[cacheKey]?.takeIf { System.currentTimeMillis() - it.at < TTL_MS }?.let { return it.item }
        val found = withTimeoutOrNull(6_000) { runCatching { lookup(artist, pageItems) }.getOrNull() } ?: return null
        cache[cacheKey] = Cached(found, System.currentTimeMillis())
        return found
    }

    private suspend fun lookup(artist: String, pageItems: List<ShelfItem>): ShelfItem? {
        val search = get("https://api.deezer.com/search/artist?limit=5&q=" + URLEncoder.encode(artist, "UTF-8")) ?: return null
        val artists = search.optJSONArray("data") ?: return null
        val id = (0 until artists.length()).map { artists.getJSONObject(it) }
            .filter { key(it.optString("name")) == key(artist) }.maxByOrNull { it.optLong("nb_fan") }?.optLong("id") ?: return null
        val albums = get("https://api.deezer.com/artist/$id/albums?limit=100")?.optJSONArray("data") ?: return null
        val today = java.time.LocalDate.now().toString()
        val newest = (0 until albums.length()).map { albums.getJSONObject(it) }
            .filter { it.optString("release_date").let { d -> d.isNotEmpty() && d <= today } }
            .maxByOrNull { it.optString("release_date") } ?: return null
        val title = newest.optString("title")
        pageItems.firstOrNull { it.browseId?.startsWith("MPRE") == true && core(it.title) == core(title) }?.let { return it }
        // Not on the artist's page (often a joint release): find its YouTube Music copy.
        return YtMusicRepository.search("${core(title)} $artist", SearchFilter.ALBUMS).getOrNull().orEmpty()
            .filterIsInstance<SearchResult.Browse>().map { it.item }
            .firstOrNull { it.type == BrowseType.ALBUM && core(it.title) == core(title) && key(it.subtitle).contains(key(artist)) }
            ?.let { ShelfItem(title = it.title, subtitle = it.subtitle, thumbnailUrl = it.thumbnailUrl, videoId = null, browseId = it.browseId) }
    }

    private suspend fun get(url: String): JSONObject? = withContext(Dispatchers.IO) {
        Http.client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()?.let(::JSONObject)
        }
    }

    /** The title without anything in brackets ("Let's Rock (feat. Kodak Black) [from GTAVI: The Album]" -> "let s rock"). */
    private fun core(title: String): String = key(title.replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), ""))

    /** A title as both catalogues can agree on it: no " - Single"/" - EP", no (feat. …), letters and digits only. */
    fun key(title: String): String =
        title.lowercase()
            .replace(Regex("""\s+-\s+(single|ep)$"""), "")
            .replace(Regex("""\s*[(\[](feat|ft|with)\.?[^)\]]*[)\]]"""), "")
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
}
