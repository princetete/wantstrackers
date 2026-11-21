package com.wantstracker.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.wantstracker.app.MainActivity
import com.wantstracker.app.R
import com.wantstracker.app.WantsApp
import com.wantstracker.app.domain.DAY_MS
import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status
import com.wantstracker.app.domain.isOverdue
import com.wantstracker.app.domain.plannedKey
import java.util.concurrent.TimeUnit

object Notifier {
    private const val CHANNEL = "wants_reminders"

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = ctx.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun notify(ctx: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

object Reminders {
    fun scheduleDaily(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "daily_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyCheckWorker>(24, TimeUnit.HOURS).build()
        )
    }

    fun scheduleCooling(ctx: Context, wantId: Long, until: Long) {
        val delay = (until - System.currentTimeMillis()).coerceAtLeast(0L)
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "cool_$wantId",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CoolingWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("id" to wantId))
                .build()
        )
    }

    fun cancelCooling(ctx: Context, wantId: Long) {
        WorkManager.getInstance(ctx).cancelUniqueWork("cool_$wantId")
    }
}

class DailyCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WantsApp
        val prefs = app.prefs
        if (!prefs.remindersEnabled.value) return Result.success()
        val wants = app.db.wantDao().getAll()
        val nowKey = QuarterKey.now()

        if (prefs.lastQuarterNotified.isEmpty()) {
            prefs.lastQuarterNotified = nowKey.label
        } else if (prefs.lastQuarterNotified != nowKey.label) {
            prefs.lastQuarterNotified = nowKey.label
            val planned = wants.count { it.plannedKey == nowKey && it.status.isOpen }
            val overdue = wants.count { it.isOverdue() }
            if (planned > 0 || overdue > 0) {
                val parts = buildList {
                    if (planned > 0) add("$planned planned for ${nowKey.label}")
                    if (overdue > 0) add("$overdue overdue")
                }
                Notifier.notify(applicationContext, 1, "New quarter, new wants", parts.joinToString(" · "))
            }
        }

        val now = System.currentTimeMillis()
        val stale = wants.count { it.status == Status.IN_PROGRESS && now - it.lastStatusChangeAt > 30 * DAY_MS }
        if (stale > 0 && now - prefs.lastStaleNudge > 7 * DAY_MS) {
            prefs.lastStaleNudge = now
            Notifier.notify(
                applicationContext, 2, "Still on it?",
                "$stale want${if (stale == 1) " has" else "s have"} been in progress for 30+ days. Continue, pause or let go."
            )
        }
        return Result.success()
    }
}

class CoolingWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WantsApp
        if (!app.prefs.remindersEnabled.value) return Result.success()
        val id = inputData.getLong("id", -1L)
        val w = app.db.wantDao().get(id) ?: return Result.success()
        val until = w.coolingUntil ?: return Result.success()
        if (!w.status.isOpen || until > System.currentTimeMillis() + 60_000L) return Result.success()
        Notifier.notify(
            applicationContext, (1000 + id % 100000).toInt(),
            "Cooling-off over",
            "\"${w.name}\" — still want it? Time to decide."
        )
        return Result.success()
    }
}
