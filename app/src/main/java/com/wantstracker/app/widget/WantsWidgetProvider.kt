package com.wantstracker.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.wantstracker.app.MainActivity
import com.wantstracker.app.R
import com.wantstracker.app.WantsApp
import com.wantstracker.app.domain.QuarterKey
import com.wantstracker.app.domain.Status
import com.wantstracker.app.domain.plannedKey
import com.wantstracker.app.domain.formatInr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WantsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context, appWidgetManager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private suspend fun render(context: Context, mgr: AppWidgetManager, ids: IntArray) {
            val app = context.applicationContext as WantsApp
            val wants = app.db.wantDao().getAll()
            val inProgress = wants.filter { it.status == Status.IN_PROGRESS }
            val notStarted = wants.filter { it.status == Status.NOT_STARTED }
            val bought = wants.count { it.status == Status.BOUGHT }
            val failed = wants.count { it.status == Status.FAILED }
            val thisQuarter = QuarterKey.now()
            val plannedNow = wants.count { it.status.isOpen && it.plannedKey == thisQuarter }

            val top = inProgress.minWithOrNull(compareBy({ it.priority.ordinal }, { it.plannedKey ?: QuarterKey(9999, 4) }))
                ?: notStarted.minWithOrNull(compareBy({ it.priority.ordinal }, { it.plannedKey ?: QuarterKey(9999, 4) }))

            val title = top?.name ?: "No wants yet"
            val sub = "${inProgress.size} in progress · ${notStarted.size} not started"
            val pendingValue = (inProgress + notStarted).sumOf { it.price }
            val stats = "🟢 $bought  🔴 $failed  ·  ${thisQuarter.label}: $plannedNow planned" +
                if (pendingValue > 0) "  ·  ${formatInr(pendingValue)} to go" else ""

            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_wants)
                views.setTextViewText(R.id.w_title, title)
                views.setTextViewText(R.id.w_sub, sub)
                views.setTextViewText(R.id.w_stats, stats)
                views.setOnClickPendingIntent(R.id.widget_root, open)
                mgr.updateAppWidget(id, views)
            }
        }

        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, WantsWidgetProvider::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch { render(context, mgr, ids) }
        }
    }
}
