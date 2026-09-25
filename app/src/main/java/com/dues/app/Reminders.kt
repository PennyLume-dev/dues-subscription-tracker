package com.dues.app

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dues.app.data.AppDb
import com.dues.app.data.daysPhrase
import com.dues.app.data.isTrial
import com.dues.app.data.nextPayment
import com.dues.app.data.priceOn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

private const val CHANNEL = "renewals"

fun createChannel(ctx: Context) {
    ctx.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(NotificationChannel(CHANNEL, "Renewal reminders", NotificationManager.IMPORTANCE_DEFAULT))
}

/** Daily check at ~9:00. ponytail: WorkManager may drift by minutes; exact alarms aren't worth the permission. */
fun scheduleDaily(ctx: Context) {
    val now = LocalDateTime.now()
    var at = now.toLocalDate().atTime(9, 0)
    if (!at.isAfter(now)) at = at.plusDays(1)
    WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
        "daily-reminders",
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, at).toMillis(), TimeUnit.MILLISECONDS)
            .build(),
    )
}

fun sendTest(ctx: Context) {
    WorkManager.getInstance(ctx).enqueue(
        OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .setInputData(workDataOf("test" to true))
            .build()
    )
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (inputData.getBoolean("test", false)) {
            notify(ctx, 0, "Reminders are working", "Dues will nudge you like this before a renewal.")
            return Result.success()
        }
        val prefs = Prefs(ctx)
        if (!prefs.notifications) return Result.success()

        val dao = AppDb.get(ctx).dao()
        val changes = dao.changesOnce()
        val today = LocalDate.now()
        dao.subsOnce().forEach { s ->
            if (s.notifyDaysBefore < 0) return@forEach
            val next = s.nextPayment(today) ?: return@forEach
            if (ChronoUnit.DAYS.between(today, next) != s.notifyDaysBefore.toLong()) return@forEach
            val price = "%.2f %s".format(priceOn(s, changes, next), prefs.currency)
            val (title, text) = if (s.isTrial(today))
                "${s.name} free trial ends ${daysPhrase(today, next)}" to "Cancel before ${next.format(DATE)} to avoid a $price charge."
            else
                "${s.name} renews ${daysPhrase(today, next)}" to "$price on ${next.format(DATE)}."
            notify(ctx, s.id.toInt(), title, text)
        }
        return Result.success()
    }
}

@SuppressLint("MissingPermission") // checked via areNotificationsEnabled
private fun notify(ctx: Context, id: Int, title: String, text: String) {
    val nm = NotificationManagerCompat.from(ctx)
    if (!nm.areNotificationsEnabled()) return
    val open = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )
    nm.notify(
        id,
        NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build(),
    )
}
