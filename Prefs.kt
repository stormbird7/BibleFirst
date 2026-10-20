package com.biblefirst

import android.content.Context
import java.time.LocalDateTime

/** All saved settings and the "is the phone locked right now?" rule. */
object Prefs {
    private fun sp(c: Context) =
        c.getSharedPreferences("biblefirst", Context.MODE_PRIVATE)

    fun enabled(c: Context) = sp(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()

    /** Hour of day (0-12) when a new "reading day" begins and the lock turns on. */
    fun startHour(c: Context) = sp(c).getInt("start_hour", 4)
    fun setStartHour(c: Context, h: Int) =
        sp(c).edit().putInt("start_hour", h.coerceIn(0, 12)).apply()

    /** Minutes before the lock starts to send a reminder notification (0 = off). */
    fun reminderMinutes(c: Context) = sp(c).getInt("reminder_minutes", 30)
    fun setReminderMinutes(c: Context, m: Int) =
        sp(c).edit().putInt("reminder_minutes", m.coerceIn(0, 120)).apply()

    fun bibleApps(c: Context): Set<String> =
        sp(c).getStringSet("bible_apps", emptySet()) ?: emptySet()

    fun setBibleApps(c: Context, s: Set<String>) =
        sp(c).edit().putStringSet("bible_apps", HashSet(s)).apply()

    /** The current "reading day" as a string. Rolls over at the start hour, not midnight. */
    private fun today(c: Context): String =
        LocalDateTime.now().minusHours(startHour(c).toLong()).toLocalDate().toString()

    fun doneToday(c: Context) = sp(c).getString("done_day", null) == today(c)
    fun markDone(c: Context) = sp(c).edit().putString("done_day", today(c)).apply()
    fun clearDone(c: Context) = sp(c).edit().remove("done_day").apply()

    fun overrideActive(c: Context) =
        System.currentTimeMillis() < sp(c).getLong("override_until", 0L)

    fun overrideCount(c: Context) = sp(c).getInt("override_count", 0)

    fun grantOverride(c: Context, minutes: Int) {
        sp(c).edit()
            .putLong("override_until", System.currentTimeMillis() + minutes * 60_000L)
            .putInt("override_count", overrideCount(c) + 1)
            .apply()
    }

    fun isLocked(c: Context) = enabled(c) && !doneToday(c) && !overrideActive(c)
}
