package com.holadeutsch.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily practice reminder with WorkManager.
 *
 * A [ReminderWorker] is enqueued as unique periodic work, initially delayed until the
 * user's chosen hour/minute. WorkManager owns the recurring schedule, so the worker does
 * not need to replace itself while it is running.
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
        enqueue(hour, minute, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)
    }

    /** Restores missing work on app startup without postponing an already queued reminder. */
    fun ensureScheduled(hour: Int, minute: Int) {
        enqueue(hour, minute, ExistingPeriodicWorkPolicy.KEEP)
    }

    private fun enqueue(hour: Int, minute: Int, policy: ExistingPeriodicWorkPolicy) {
        ensureChannel()
        val now = ZonedDateTime.now()
        var next = now.withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delaySeconds = Duration.between(now, next).seconds

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).apply {
            // Remove the self-rescheduling one-time job used by older app versions.
            cancelUniqueWork(LEGACY_WORK_NAME)
            enqueueUniquePeriodicWork(
                WORK_NAME,
                policy,
                request
            )
        }
    }

    fun cancel() {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(WORK_NAME)
            cancelUniqueWork(LEGACY_WORK_NAME)
        }
    }

    /** Convenience used by settings: schedule when enabled, cancel when off. */
    fun apply(enabled: Boolean, hour: Int, minute: Int) {
        ensureChannel()
        if (enabled) schedule(hour, minute) else cancel()
    }

    companion object {
        const val CHANNEL_ID = "daily_reminder"
        const val WORK_NAME = "daily_reminder_periodic_work"
        private const val LEGACY_WORK_NAME = "daily_reminder_work"
        const val NOTIFICATION_ID = 1001
    }
}
