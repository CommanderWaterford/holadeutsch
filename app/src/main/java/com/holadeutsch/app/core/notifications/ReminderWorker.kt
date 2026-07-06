package com.holadeutsch.app.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.holadeutsch.app.HolaDeutschApp
import com.holadeutsch.app.MainActivity
import com.holadeutsch.app.R
import com.holadeutsch.app.data.repo.Stats
import kotlinx.coroutines.flow.first

/**
 * Posts the daily reminder, then re-schedules itself for the next day.
 * Skips the notification when the user has already met today's goal.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? HolaDeutschApp ?: return Result.success()
        val scheduler = app.container.reminderScheduler
        val stats = app.container.statsRepository.stats.first()

        // The user turned the reminder off since this job was queued: let the chain die.
        if (!stats.reminderEnabled) return Result.success()

        if (stats.wordsToday < stats.dailyGoal) {
            notify(stats)
        }

        // Keep the daily cadence going.
        scheduler.schedule(stats.reminderHour, stats.reminderMinute)
        return Result.success()
    }

    private fun notify(stats: Stats) {
        val remaining = (stats.dailyGoal - stats.wordsToday).coerceAtLeast(0)
        val text = if (stats.streak > 0) {
            "No pierdas tu racha de ${stats.streak} " +
                "${if (stats.streak == 1) "día" else "días"}. Te faltan $remaining palabras hoy."
        } else {
            "Dedica unos minutos a tus palabras del día."
        }

        val launch = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("¡Hora de practicar alemán! 🇩🇪")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(ReminderScheduler.NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted; nothing to show.
        }
    }
}
