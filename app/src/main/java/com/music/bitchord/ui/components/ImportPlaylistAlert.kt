package com.music.bitchord.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.data.model.PlaylistPrivacy
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.sources.ServerLibrary
import com.music.bitchord.data.sources.SourceConfig
import com.music.bitchord.data.sources.SubsonicSource
import com.music.bitchord.data.spotify.SpotifyImportTrack
import com.music.bitchord.data.spotify.SpotifyImporter
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * "Import Playlist": first picks where from — Spotify, or any connected server that
 * has playlists (Navidrome, Jellyfin…) — then, for a server, which playlist. A server
 * playlist is matched song by song on YouTube Music and saved as a YouTube Music
 * playlist, the same way a Spotify one is, so sources never mix inside a playlist.
 */
@Composable
fun ImportPlaylistAlert(
    hazeState: HazeState,
    servers: List<SourceConfig>,
    onSpotify: () -> Unit,
    /** Called once the songs are matched, as for a Spotify import. */
    onImported: (title: String, privacy: PlaylistPrivacy, songs: List<Song>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf<SourceConfig?>(null) }
    var playlists by remember { mutableStateOf<List<SubsonicSource.Entry>?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var missed by remember { mutableStateOf<List<SpotifyImportTrack>?>(null) }

    LaunchedEffect(server?.id) {
        val chosen = server ?: return@LaunchedEffect
        playlists = null
        runCatching { ServerLibrary.source(chosen.id)!!.playlists() }
            .onSuccess { playlists = it; if (it.isEmpty()) status = context.getString(R.string.import_no_playlists) }
            .onFailure { playlists = emptyList(); failed = true; status = context.getString(R.string.import_server_failed, it.message.orEmpty()) }
    }

    fun import(entry: SubsonicSource.Entry) {
        val source = ServerLibrary.source(server?.id ?: return) ?: return
        working = true
        failed = false
        status = context.getString(R.string.spotify_import_fetching)
        scope.launch {
            try {
                val listing = source.playlist(entry.id)
                val tracks = listing.songs.map { SpotifyImportTrack(it.title, it.artist) }
                val (songs, unmatched) = SpotifyImporter.resolveToSongs(tracks, null) { done, total ->
                    status = context.getString(R.string.spotify_import_matching, done, total)
                }
                if (songs.isEmpty()) error(context.getString(R.string.spotify_import_none))
                onImported(entry.title, PlaylistPrivacy.PRIVATE, songs)
                if (unmatched.isEmpty()) onDismiss() else missed = unmatched
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = context.getString(R.string.spotify_import_failed, e.message.orEmpty())
                failed = true
            }
            working = false
        }
    }

    AlertScaffold(hazeState = hazeState, onDismiss = { if (!working) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 19.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.import_playlist_title),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.W600),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            val chosen = server
            Text(
                text = when {
                    missed != null -> stringResource(R.string.spotify_import_unmatched, missed!!.size)
                    status != null -> status!!
                    chosen == null -> stringResource(R.string.import_choose_source)
                    else -> stringResource(R.string.import_choose_playlist, chosen.displayName)
                },
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (!working && missed == null) {
                if (chosen == null) {
                    ImportRow(stringResource(R.string.spotify)) { onSpotify() }
                    servers.forEach { s -> ImportRow(s.displayName) { server = s } }
                } else {
                    playlists.orEmpty().forEach { p -> ImportRow(p.title, p.subtitle) { import(p) } }
                }
            }
        }
        AlertRule()
        AlertAction(
            label = stringResource(if (missed != null) R.string.done else R.string.cancel),
            emphasised = missed != null,
            onClick = onDismiss,
            enabled = !working,
        )
    }
}

@Composable
private fun ImportRow(title: String, subtitle: String = "", onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
