package com.music.bitchord.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.RepeatMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.border
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.data.sources.GlazeCompanion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Blue = Color(0xFF0A84FF)
private val Green = Color(0xFF30D158)
private val Orange = Color(0xFFFF9F0A)
private val Red = Color(0xFFFF453A)

/**
 * Everything the companion is doing for the user's music server, as its web dashboard shows it:
 * what needs a decision, what is downloading, what was added, what the library already had, and
 * what could not be added and why. It refreshes by itself, and is where a notification opens.
 */
@Composable
fun NavidromeDashboardScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val server = remember { GlazeCompanion.server() }
    val scope = rememberCoroutineScope()
    var jobs by remember { mutableStateOf<List<GlazeCompanion.JobInfo>?>(null) }
    var error by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    // Deleted here: gone from the list at once, before the companion confirms it.
    var deleted by remember { mutableStateOf(emptySet<String>()) }
    // The last deletion, offered back for a few seconds.
    var undo by remember { mutableStateOf<Removal?>(null) }

    LaunchedEffect(server?.id, refresh) {
        if (server == null) return@LaunchedEffect
        while (true) {
            runCatching { GlazeCompanion.jobs(server) }
                .onSuccess { jobs = it; error = false }
                .onFailure { error = true }
            // The optional JioSaavn fallback for songs with no lossless copy (each tried once).
            if (com.music.bitchord.data.settings.AppSettings.serverLossyFallback.value) {
                jobs.orEmpty().filter { it.job.failed }.forEach { GlazeCompanion.fillFromJioSaavn(server, it) }
            }
            delay(5_000)
        }
    }
    LaunchedEffect(undo) { if (undo != null) { delay(5_000); undo = null } }

    fun requestKey(info: GlazeCompanion.JobInfo) =
        "${GlazeCompanion.cleanArtist(info.artist)}|${info.title}|${info.kind}|${info.album}".lowercase()
    val visible = remember(jobs, deleted) { jobs.orEmpty().filter { it.job.id !in deleted } }
    // One entry per request, the newest of any repeats; cancelled ones aren't shown at all
    // (the web dashboard leaves them out too).
    val latest = remember(visible) {
        // By when each attempt was made, not last touched: an old failure can't cover a later success.
        visible.sortedByDescending { it.createdAt }.distinctBy(::requestKey).filter { it.job.status != "cancelled" }
    }
    val ask = latest.filter { it.job.review }
    val active = latest.filter { it.job.active }
    val added = latest.filter { it.job.status == "rescanned" }.take(15)
    val have = latest.filter { it.job.status == "in_library" || it.job.status == "skipped" }.take(15)
    val bad = latest.filter { it.job.failed }.take(15)

    var refreshing by remember { mutableStateOf(false) }
    fun act(block: suspend () -> Unit) = scope.launch { runCatching { block() }; refreshing = false; refresh++ }

    /** Hides [infos] and every older repeat of them, and offers them back. */
    fun remove(infos: List<GlazeCompanion.JobInfo>, label: String) {
        if (server == null || infos.isEmpty()) return
        val keys = infos.map(::requestKey).toSet()
        val ids = visible.filter { requestKey(it) in keys && !it.job.active }.map { it.job.id }.toSet()
        deleted = deleted + ids
        undo = Removal(label, ids)
        scope.launch { runCatching { GlazeCompanion.setHidden(server, ids, true) } }
    }
    fun restore(removal: Removal) {
        if (server == null) return
        undo = null
        deleted = deleted - removal.ids
        scope.launch { runCatching { GlazeCompanion.setHidden(server, removal.ids, false) }; refresh++ }
    }

    // The source's own name (what it was called in Sources, else its host): never a fixed "Navidrome".
    val serverName = server?.displayName ?: stringResource(R.string.dashboard_your_server)
    val clearedLabel = stringResource(R.string.dashboard_cleared)
    val deletedLabel = stringResource(R.string.dashboard_deleted)
    Box(modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item(key = "title") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.navidrome_dashboard),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (server != null) {
                        // Everything that isn't running, gone in one go (and back with Undo).
                        HeaderButton(
                            icon = Icons.Rounded.DeleteSweep,
                            label = stringResource(R.string.dashboard_clear_all),
                            enabled = latest.any { !it.job.active },
                        ) { remove(latest.filter { !it.job.active }, clearedLabel) }
                        Spacer(Modifier.width(8.dp))
                        // Re-checks every failure against the library (an album added since shows as
                        // "Already in your library") and retries the newest ones still missing.
                        RefreshButton(refreshing) {
                            refreshing = true
                            act { GlazeCompanion.refresh(server); delay(2_000) }
                        }
                    }
                }
                Text(
                    text = when {
                        server == null -> stringResource(R.string.navidrome_dashboard_none)
                        error -> stringResource(R.string.navidrome_dashboard_offline)
                        else -> stringResource(R.string.navidrome_dashboard_subtitle, serverName)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp),
                )
                if (server != null) {
                    val lossy by com.music.bitchord.data.settings.AppSettings.serverLossyFallback.collectAsStateWithLifecycle()
                    Spacer(Modifier.height(16.dp))
                    // The companion's own settings, read from it and saved back to it.
                    var singles by remember { mutableStateOf<GlazeCompanion.SinglesSettings?>(null) }
                    LaunchedEffect(server.id) { singles = GlazeCompanion.singlesSettings(server) }
                    fun saveSingles(next: GlazeCompanion.SinglesSettings) {
                        val before = singles
                        singles = next
                        scope.launch { if (runCatching { GlazeCompanion.setSinglesSettings(server, next) }.isFailure) singles = before }
                    }
                    GroupCell(first = true, last = singles == null) {
                        SettingSwitchRow(
                            title = stringResource(R.string.server_lossy_fallback),
                            subtitle = stringResource(R.string.server_lossy_fallback_subtitle),
                            checked = lossy,
                            onCheckedChange = com.music.bitchord.data.settings.AppSettings::setServerLossyFallback,
                        )
                    }
                    singles?.let { current ->
                        GroupCell(first = false, last = false) {
                            GroupDivider()
                            SettingSwitchRow(
                                title = stringResource(R.string.replace_singles),
                                subtitle = stringResource(R.string.replace_singles_subtitle),
                                checked = current.replaceSingles,
                                onCheckedChange = { saveSingles(current.copy(replaceSingles = it)) },
                            )
                        }
                        GroupCell(first = false, last = true) {
                            GroupDivider()
                            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
                                Text(stringResource(R.string.album_from_singles), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                                Text(stringResource(R.string.album_from_singles_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "off" to R.string.album_from_singles_off,
                                        "any" to R.string.album_from_singles_any,
                                        "half" to R.string.album_from_singles_half,
                                        "all" to R.string.album_from_singles_all,
                                    ).forEach { (value, label) ->
                                        val selected = current.albumFromSingles == value
                                        Text(
                                            text = stringResource(label),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                                                .clickable { saveSingles(current.copy(albumFromSingles = value)) }
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Which companion this is, and a link to its newest release when it is behind.
                    var version by remember { mutableStateOf<GlazeCompanion.CompanionVersion?>(null) }
                    LaunchedEffect(server.id) { version = GlazeCompanion.version(server) }
                    version?.let { v ->
                        val uri = androidx.compose.ui.platform.LocalUriHandler.current
                        Text(
                            text = if (v.updateAvailable && v.latest != null) stringResource(R.string.companion_update_available, v.version, v.latest)
                            else if (v.latest != null) stringResource(R.string.companion_up_to_date, v.version)
                            else stringResource(R.string.companion_version, v.version),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (v.updateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (v.updateAvailable) Modifier.clickable { uri.openUri("https://github.com/ILostXD/BitChord_Plus_Companion/releases/latest") } else Modifier)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            if (server == null) return@LazyColumn
            val delete: (GlazeCompanion.JobInfo) -> Unit = { info -> remove(listOf(info), deletedLabel.format(info.title)) }

            group("ask", R.string.dashboard_needs_decision, ask, null, delete) { info ->
                JobRow(info, Orange, info.job.summary?.let { "$it. " + stringResource(R.string.dashboard_download_anyway) }.orEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { act { GlazeCompanion.decide(server, info.job.id, false) } }) { Text(stringResource(R.string.review_keep)) }
                        TextButton(onClick = { act { GlazeCompanion.decide(server, info.job.id, true) } }) { Text(stringResource(R.string.review_download)) }
                    }
                }
            }
            group("active", R.string.dashboard_in_progress, active, R.string.dashboard_nothing_downloading, null) { info ->
                JobRow(info, Blue, "${kindText(info)} · ${statusText(info.job.status)}", pulse = true)
            }
            group("bad", R.string.dashboard_failed, bad, null, delete) { info ->
                JobRow(info, Red, failureText(info), trailing = {
                    if (info.canApprove) {
                        TextButton(onClick = { act { GlazeCompanion.approve(server, info.job.id) } }) { Text(stringResource(R.string.dashboard_import_anyway)) }
                    }
                    IconButton(onClick = { act { GlazeCompanion.retry(server, info.job.id) } }) {
                        Icon(Icons.Rounded.Replay, contentDescription = stringResource(R.string.dashboard_try_again), tint = MaterialTheme.colorScheme.onBackground)
                    }
                })
            }
            group("added", R.string.dashboard_added, added, null, delete) { info -> JobRow(info, Green, kindText(info)) }
            group("have", R.string.dashboard_already_have, have, null, delete) { info ->
                JobRow(info, Green, info.job.summary ?: stringResource(R.string.server_download_done))
            }
        }

        // "… was deleted · Undo", just above the player and the tab bar. It rises from the bottom
        // and leaves the same way.
        AnimatedVisibility(
            visible = undo != null,
            enter = slideInVertically(tween(250, easing = EaseOutStrong)) { it } + fadeIn(tween(250, easing = EaseOutStrong)),
            exit = slideOutVertically(tween(200, easing = EaseOutStrong)) { it } + fadeOut(tween(200, easing = EaseOutStrong)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (contentPadding.calculateBottomPadding() - 24.dp).coerceAtLeast(16.dp), start = 16.dp, end = 16.dp),
        ) {
            // Holds the last value through the exit, so the text doesn't blank as it leaves.
            var shown by remember { mutableStateOf<Removal?>(null) }
            undo?.let { shown = it }
            shown?.let { removal -> UndoPill(removal.label) { restore(removal) } }
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        com.music.bitchord.ui.components.IosSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f))
}

/** What a delete took away, to put back with Undo. */
private class Removal(val label: String, val ids: Set<String>)

/** A strong ease-out (0.23, 1, 0.32, 1): the motion lands fast and settles. */
private val EaseOutStrong = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
private fun HeaderButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else 0.35f))
    }
}

/**
 * Spins at a constant speed while the companion re-checks, then eases out to the next full
 * turn instead of snapping back; a press also gives a light tick.
 */
@Composable
private fun RefreshButton(refreshing: Boolean, onClick: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(refreshing) {
        if (refreshing) {
            while (true) rotation.animateTo(rotation.value + 360f, tween(700, easing = LinearEasing))
        } else if (rotation.value % 360f != 0f) {
            rotation.animateTo((rotation.value / 360f).toInt() * 360f + 360f, tween(450, easing = EaseOutStrong))
            rotation.snapTo(0f)
        }
    }
    IconButton(
        onClick = {
            if (!refreshing) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
    ) {
        Icon(
            Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.dashboard_refresh),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.graphicsLayer { rotationZ = rotation.value },
        )
    }
}

@Composable
private fun UndoPill(label: String, onUndo: () -> Unit) {
    // The page's own light or dark, not Material's inverse colours.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (dark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
            .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
            .padding(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onUndo) {
            Text(stringResource(R.string.dashboard_undo), color = Blue, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A section as one inset rounded group, rows split by hairlines, with its count in the header. */
private fun LazyListScope.group(
    key: String,
    title: Int,
    rows: List<GlazeCompanion.JobInfo>,
    empty: Int?,
    /** Swipe left to delete; null where rows can't be (running downloads). */
    onDelete: ((GlazeCompanion.JobInfo) -> Unit)?,
    row: @Composable (GlazeCompanion.JobInfo) -> Unit,
) {
    if (rows.isEmpty() && empty == null) return
    item(key = "h-$key") {
        Text(
            text = stringResource(title).uppercase() + if (rows.isNotEmpty()) "  ${rows.size}" else "",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 32.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        )
    }
    if (rows.isEmpty()) {
        item(key = "e-$key") {
            GroupCell(first = true, last = true) {
                Text(
                    text = stringResource(empty!!),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
        return
    }
    rows.forEachIndexed { index, info ->
        item(key = "$key-${info.job.id}") {
            GroupCell(first = index == 0, last = index == rows.lastIndex) {
                if (index > 0) {
                    HorizontalDivider(
                        Modifier.padding(start = 80.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f),
                    )
                }
                if (onDelete == null) row(info) else SwipeToDelete(onDelete = { onDelete(info) }) { row(info) }
            }
        }
    }
}

/** A row that slides away to the left over a red "delete" strip; the list's Undo brings it back. */
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val current by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value -> (value == SwipeToDismissBoxValue.EndToStart).also { if (it) current() } },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(Red).padding(end = 22.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.White)
            }
        },
    ) {
        // Opaque, so the strip only shows where the row has moved off it.
        Box(Modifier.background(MaterialTheme.colorScheme.background).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))) { content() }
    }
}

/** One cell of a group: only the group's first and last cells round their outer corners. */
@Composable
private fun GroupCell(first: Boolean, last: Boolean, content: @Composable () -> Unit) {
    val radius = 16.dp
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (first) radius else 0.dp, if (first) radius else 0.dp, if (last) radius else 0.dp, if (last) radius else 0.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)),
    ) { content() }
}

@Composable
private fun JobRow(
    info: GlazeCompanion.JobInfo,
    color: Color,
    subtitle: String,
    pulse: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f, targetValue = if (pulse) 0.3f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dot",
    )
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 11.dp, bottom = 11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp)) {
                val art = RoundedCornerShape(8.dp)
                Box(
                    Modifier.size(48.dp).clip(art).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (info.kind == "album") Icons.Rounded.Album else Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        modifier = Modifier.size(22.dp),
                    )
                    info.cover?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
                }
                // The status, ringed in the page colour so it reads on any artwork.
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp)
                        .alpha(dotAlpha)
                        .clip(CircleShape)
                        .background(color),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = GlazeCompanion.cleanArtist(info.artist),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing?.invoke()
        }
        if (below != null) {
            Spacer(Modifier.height(4.dp))
            below()
        }
    }
}

@Composable
private fun kindText(info: GlazeCompanion.JobInfo) =
    stringResource(if (info.kind == "album") R.string.dashboard_album else R.string.dashboard_song)

@Composable
private fun statusText(status: String) = stringResource(
    when (status) {
        "searching" -> R.string.server_download_searching
        "downloading" -> R.string.server_download_downloading
        else -> R.string.server_download_importing
    },
)

@Composable
private fun failureText(info: GlazeCompanion.JobInfo) = when (info.job.error) {
    null -> stringResource(R.string.navidrome_reason_unknown)
    "no_suitable_slskd_match" -> stringResource(R.string.navidrome_reason_no_match)
    "slskd_search_timeout" -> stringResource(R.string.dashboard_reason_timeout)
    "metadata_review_required" -> stringResource(R.string.dashboard_reason_tags)
    else -> info.job.error.substringBefore(':').replace('_', ' ')
}
