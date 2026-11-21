package com.music.bitchord.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * Everything the companion is doing for the user's Navidrome, as the web dashboard shows it:
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

    LaunchedEffect(server?.id, refresh) {
        if (server == null) return@LaunchedEffect
        while (true) {
            runCatching { GlazeCompanion.jobs(server) }
                .onSuccess { jobs = it; error = false }
                .onFailure { error = true }
            delay(5_000)
        }
    }
    // One entry per request: the newest of any repeats.
    val latest = remember(jobs) {
        jobs.orEmpty().sortedByDescending { it.updatedAt }.distinctBy { "${it.artist}|${it.title}|${it.kind}|${it.album}".lowercase() }
    }
    val ask = latest.filter { it.job.review }
    val active = latest.filter { it.job.active }
    val added = latest.filter { it.job.status == "rescanned" }.take(15)
    val have = latest.filter { it.job.status == "in_library" || it.job.status == "skipped" }.take(15)
    val bad = latest.filter { it.job.failed }.take(15)

    fun act(block: suspend () -> Unit) = scope.launch { runCatching { block() }; refresh++ }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "title") {
            Text(
                text = stringResource(R.string.navidrome_dashboard),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp),
            )
            Text(
                text = stringResource(
                    when {
                        server == null -> R.string.navidrome_dashboard_none
                        error -> R.string.navidrome_dashboard_offline
                        else -> R.string.navidrome_dashboard_subtitle
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )
        }
        if (server == null) return@LazyColumn

        section("ask", R.string.dashboard_needs_decision, ask, null) { info ->
            JobCard(
                info = info, color = Orange,
                subtitle = info.job.summary?.let { "$it. " + stringResource(R.string.dashboard_download_anyway) }.orEmpty(),
            ) {
                FilledTonalButton(onClick = { act { GlazeCompanion.decide(server, info.job.id, false) } }) { Text(stringResource(R.string.review_keep)) }
                Spacer(Modifier.size(8.dp))
                FilledTonalButton(onClick = { act { GlazeCompanion.decide(server, info.job.id, true) } }) { Text(stringResource(R.string.review_download)) }
            }
        }
        section("active", R.string.dashboard_in_progress, active, R.string.dashboard_nothing_downloading) { info ->
            JobCard(info = info, color = Blue, pulse = true, subtitle = statusText(info.job.status))
        }
        section("added", R.string.dashboard_added, added, null) { info -> JobCard(info = info, color = Green, subtitle = kindText(info)) }
        section("have", R.string.dashboard_already_have, have, null) { info ->
            JobCard(info = info, color = Green, subtitle = info.job.summary ?: stringResource(R.string.server_download_done))
        }
        section("bad", R.string.dashboard_failed, bad, null) { info ->
            JobCard(info = info, color = Red, subtitle = failureText(info)) {
                if (info.canApprove) {
                    FilledTonalButton(onClick = { act { GlazeCompanion.approve(server, info.job.id) } }) { Text(stringResource(R.string.dashboard_import_anyway)) }
                    Spacer(Modifier.size(8.dp))
                }
                FilledTonalButton(onClick = { act { GlazeCompanion.retry(server, info.job.id) } }) { Text(stringResource(R.string.dashboard_try_again)) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    key: String,
    title: Int,
    rows: List<GlazeCompanion.JobInfo>,
    empty: Int?,
    row: @Composable (GlazeCompanion.JobInfo) -> Unit,
) {
    if (rows.isEmpty() && empty == null) return
    item(key = "h-$key") {
        Text(
            text = stringResource(title).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
        )
    }
    if (rows.isEmpty()) {
        item(key = "e-$key") {
            Text(
                text = stringResource(empty!!),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
    items(rows, key = { "$key-${it.job.id}" }) { info ->
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { row(info) }
    }
}

@Composable
private fun JobCard(
    info: GlazeCompanion.JobInfo,
    color: Color,
    subtitle: String,
    pulse: Boolean = false,
    actions: (@Composable () -> Unit)? = null,
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f, targetValue = if (pulse) 0.3f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dot",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(2.dp))
            Spacer(
                Modifier.size(10.dp).alpha(dotAlpha).clip(CircleShape).background(color),
            )
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${info.artist} – ${info.title}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (actions != null) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { actions() }
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
    else -> info.job.error.replace('_', ' ')
}
