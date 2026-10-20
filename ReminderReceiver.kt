package com.biblefirst

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires the reminder, and re-arms it after a reboot. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Reminder.ACTION && Prefs.enabled(c) && Prefs.reminderMinutes(c) > 0) {
            Reminder.show(c)
        }
        // Always set up tomorrow's reminder (also covers BOOT_COMPLETED).
        Reminder.schedule(c)
    }
}
