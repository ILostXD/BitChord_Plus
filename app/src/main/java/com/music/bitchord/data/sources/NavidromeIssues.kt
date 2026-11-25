package com.music.bitchord.data.sources

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.music.bitchord.MainActivity
import com.music.bitchord.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Tells the user, with an Android notification, about a download request that needs them:
 * the companion paused to ask ("Download it anyway?"), or one that could not be added.
 * Nothing is shown inside the app; the dashboard is where they are looked at.
 *
 * Run once a minute while the app is open and every 15 minutes in the background.
 */
object NavidromeIssues {
    private const val CHANNEL = "navidrome_issues"
    private const val PREFS = "navidrome_issues"
    private const val SEEN = "seen"
    private const val STARTED = "started"
    const val EXTRA_OPEN_DASHBOARD = "open_navidrome_dashboard"
    const val ACTION_DOWNLOAD = "com.music.bitchord.navidrome.DOWNLOAD"
    const val ACTION_KEEP = "com.music.bitchord.navidrome.KEEP"
    /** Set when a notification was tapped, so the app opens the dashboard. */
    val openDashboard = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun consume(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_DASHBOARD, false) == true) openDashboard.value = true
        intent?.getStringArrayExtra(EXTRA_PAGE)?.let { openPage.value = it.toList(); intent.removeExtra(EXTRA_PAGE) }
    }

    const val EXTRA_JOB = "job"
    const val EXTRA_NOTIFICATION = "notification"

    /** Looks for new issues and notifies once for each. A repeat of the same request is one issue. */
    suspend fun check(context: Context) {
        val server = GlazeCompanion.server() ?: return
        runCatching { checkPreSaves(context, server) }
        val jobs = runCatching { GlazeCompanion.jobs(server) }.getOrNull() ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = prefs.getStringSet(SEEN, emptySet()).orEmpty().toMutableSet()
        val first = !prefs.getBoolean(STARTED, false)
        val newest = jobs.sortedByDescending { it.createdAt }.distinctBy { "${it.artist}|${it.title}|${it.kind}".lowercase() }
        // The optional lossy fallback: songs with no lossless copy come from JioSaavn instead,
        // and those that do aren't reported as failures.
        val filled = if (com.music.bitchord.data.settings.AppSettings.serverLossyFallback.value) {
            newest.filter { it.job.failed }.filter { GlazeCompanion.fillFromJioSaavn(server, it) }.map { it.job.id }.toSet()
        } else {
            emptySet()
        }
        val issues = newest.filter { (it.job.review || it.job.failed) && it.job.id !in filled }
        val fresh = issues.filter { "${it.job.id}:${it.job.status}" !in seen }
        issues.forEach { seen += "${it.job.id}:${it.job.status}" }
        // Keep the memory bounded: only what the companion still lists.
        val alive = jobs.map { "${it.job.id}:${it.job.status}" }.toSet()
        prefs.edit().putStringSet(SEEN, seen.filter { it in alive }.toSet()).putBoolean(STARTED, true).apply()
        // The first look only learns what is already there: no flood for old failures.
        if (first) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.navidrome_issues_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val reviews = fresh.filter { it.job.review }
        val failures = fresh.filter { it.job.failed }
        reviews.take(5).forEach { notifyReview(context, manager, it) }
        when {
            failures.size > 3 -> notifyFailures(context, manager, context.getString(R.string.navidrome_failed_many, failures.size), null)
            else -> failures.forEach { notifyFailures(context, manager, context.getString(R.string.navidrome_failed_one, it.title), reason(context, it.job.error)) }
        }
    }

    /** A page to open from a notification: (page id, title, artist, cover). */
    val openPage = kotlinx.coroutines.flow.MutableStateFlow<List<String?>?>(null)
    private const val EXTRA_PAGE = "open_page"
    private const val LANDED = "landed"

    /** Notifies once when a Pre-Saved album has been added to the library. */
    private suspend fun checkPreSaves(context: Context, server: SourceConfig) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val landed = GlazeCompanion.releases(server).filter { it.saved && (it.status == "rescanned" || it.status == "in_library") }
        val known = prefs.getStringSet(LANDED, null)
        prefs.edit().putStringSet(LANDED, landed.map { it.id }.toSet()).apply()
        // The first look only learns what is already there.
        val fresh = if (known == null) emptyList() else landed.filter { it.id !in known }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (fresh.isEmpty() || !manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(PRESAVE_CHANNEL, context.getString(R.string.presave_landed_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val source = ServerLibrary.source(server.id)
        fresh.take(3).forEach { release ->
            val id = release.id.hashCode()
            val album = source?.let { runCatching { it.findAlbum(release.title, release.artist) }.getOrNull() }
            val tap = if (album == null) openDashboard(context, id) else PendingIntent.getActivity(
                context, id,
                Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_PAGE, arrayOf(ServerLibrary.pageId(server.id, ServerLibrary.Kind.ALBUM, album.id), album.title, album.subtitle, album.coverUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            manager.notify(
                id,
                NotificationCompat.Builder(context, PRESAVE_CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification_logo)
                    .setContentTitle(context.getString(R.string.presave_landed_title, release.title))
                    .setContentText(context.getString(R.string.presave_landed_text, release.title, release.artist))
                    .setContentIntent(tap)
                    .setAutoCancel(true)
                    .build(),
            )
        }
    }

    private const val PRESAVE_CHANNEL = "presave_landed"

    private fun reason(context: Context, error: String?): String = when (error) {
        null -> context.getString(R.string.navidrome_reason_unknown)
        "no_suitable_slskd_match" -> context.getString(R.string.navidrome_reason_no_match)
        else -> error.replace('_', ' ')
    }

    private fun openDashboard(context: Context, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_DASHBOARD, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(context: Context, kind: String, info: GlazeCompanion.JobInfo, id: Int): PendingIntent = PendingIntent.getBroadcast(
        context, id + kind.hashCode(),
        Intent(context, NavidromeReviewReceiver::class.java).setAction(kind)
            .putExtra(EXTRA_JOB, info.job.id).putExtra(EXTRA_NOTIFICATION, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notifyReview(context: Context, manager: NotificationManager, info: GlazeCompanion.JobInfo) {
        val id = info.job.id.hashCode()
        val text = info.job.summary ?: info.artist
        manager.notify(
            id,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_logo)
                .setContentTitle(context.getString(R.string.review_notification_title, info.title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText("${info.artist}\n$text"))
                .setContentIntent(openDashboard(context, id))
                .addAction(0, context.getString(R.string.review_keep), action(context, ACTION_KEEP, info, id))
                .addAction(0, context.getString(R.string.review_download), action(context, ACTION_DOWNLOAD, info, id))
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun notifyFailures(context: Context, manager: NotificationManager, title: String, text: String?) {
        val id = (title + text).hashCode()
        manager.notify(
            id,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_logo)
                .setContentTitle(title)
                .apply { if (text != null) setContentText(text) }
                .setContentIntent(openDashboard(context, id))
                .setAutoCancel(true)
                .build(),
        )
    }

    /** Checks every 15 minutes even when the app is closed. */
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "navidrome-issues", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<Worker>(15, TimeUnit.MINUTES).build(),
        )
    }

    class Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            check(applicationContext)
            return Result.success()
        }
    }
}

/** The "Download" / "Keep mine" buttons on a notification. */
class NavidromeReviewReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val job = intent.getStringExtra(NavidromeIssues.EXTRA_JOB) ?: return
        val server = GlazeCompanion.server() ?: return
        val download = intent.action == NavidromeIssues.ACTION_DOWNLOAD
        context.getSystemService(NotificationManager::class.java)?.cancel(intent.getIntExtra(NavidromeIssues.EXTRA_NOTIFICATION, 0))
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { GlazeCompanion.decide(server, job, download) }
            } finally {
                pending.finish()
            }
        }
    }
}
