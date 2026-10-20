package com.biblefirst

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** A gentle heads-up notification a little before the morning lock starts. */
object Reminder {
    private const val CHANNEL = "reminder"
    const val ACTION = "com.biblefirst.REMINDER"

    private fun pending(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 0,
            Intent(c, ReminderReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Sets (or cancels) the next reminder. Safe to call as often as you like. */
    fun schedule(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val minutes = Prefs.reminderMinutes(c)
        if (!Prefs.enabled(c) || minutes <= 0) {
            am.cancel(pending(c))
            return
        }
        val now = LocalDateTime.now()
        var at = LocalDate.now().atTime(Prefs.startHour(c), 0).minusMinutes(minutes.toLong())
        if (!at.isAfter(now)) at = at.plusDays(1)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact on purpose: no special exact-alarm permission, may arrive a few minutes late.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending(c))
    }

    fun show(c: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Morning reminder", NotificationManager.IMPORTANCE_DEFAULT)
        )

        // Tapping opens the first chosen Bible app, or Bible First if none is chosen.
        val open = Prefs.bibleApps(c)
            .firstNotNullOfOrNull { c.packageManager.getLaunchIntentForPackage(it) }
            ?: Intent(c, MainActivity::class.java)
        val tap = PendingIntent.getActivity(c, 1, open, PendingIntent.FLAG_IMMUTABLE)

        val minutes = Prefs.reminderMinutes(c)
        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Good morning")
            .setContentText("Your phone locks in $minutes minutes. A good moment to open your Bible app.")
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        nm.notify(1, n)
    }
}
