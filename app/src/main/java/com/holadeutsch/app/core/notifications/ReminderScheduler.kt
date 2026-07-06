package com.holadeutsch.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily practice reminder with WorkManager.
 *
 * A [ReminderWorker] is enqueued as a unique one-time job timed to the user's chosen
 * hour/minute; when it runs it re-schedules itself for the next day, giving a precise
 * time-of-day cadence that survives reboots and Doze without exact-alarm permissions.
 */
class ReminderScheduler(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorio diario",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Te recuerda practicar tu alemán cada día."
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /** (Re)schedules the reminder for the next occurrence of [hour]:[minute]. */
    fun schedule(hour: Int, minute: Int) {
        ensureChannel()
        val now = ZonedDateTime.now()
        var next = now.withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delaySeconds = Duration.between(now, next).seconds

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Convenience used by settings: schedule when enabled, cancel when off. */
    fun apply(enabled: Boolean, hour: Int, minute: Int) {
        ensureChannel()
        if (enabled) schedule(hour, minute) else cancel()
    }

    companion object {
        const val CHANNEL_ID = "daily_reminder"
        const val WORK_NAME = "daily_reminder_work"
        const val NOTIFICATION_ID = 1001
    }
}
