package com.biblefirst

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/** Setup and status screen. */
class MainActivity : Activity() {

    private lateinit var root: LinearLayout

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 32.dp(), 20.dp(), 32.dp())
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        Reminder.schedule(this)
        render()
    }

    private fun accessibilityOn(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains("$packageName/")
    }

    private fun render() {
        root.removeAllViews()

        root.addView(text("Bible First", 28f, bold = true))

        val status = when {
            !Prefs.enabled(this) -> "Lock is switched off"
            Prefs.doneToday(this) -> "Reading done today - phone is free"
            Prefs.overrideActive(this) -> "Emergency override is active"
            else -> "Phone is LOCKED until you mark today's reading done"
        }
        root.addView(text(status, 16f).apply { setPadding(0, 8.dp(), 0, 16.dp()) })

        // Step 1
        root.addView(text("1. Allow Bible First to detect open apps", 18f, bold = true))
        if (accessibilityOn()) {
            root.addView(text("Done - accessibility service is on.", 14f))
        } else {
            root.addView(text(
                "Open Accessibility settings, find Bible First (it may be under Installed apps), and switch it on.",
                14f
            ))
            root.addView(button("Open Accessibility settings") {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
        }

        // Step 2
        root.addView(text("2. Your Bible app(s)", 18f, bold = true).apply { setPadding(0, 24.dp(), 0, 0) })
        root.addView(text("Ticked apps stay usable while locked.", 14f))
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
        val chosen = Prefs.bibleApps(this).toMutableSet()
        for ((pkg, name) in apps) {
            root.addView(CheckBox(this).apply {
                this.text = name
                isChecked = pkg in chosen
                setOnCheckedChangeListener { _, on ->
                    if (on) chosen += pkg else chosen -= pkg
                    Prefs.setBibleApps(this@MainActivity, chosen)
                }
            })
        }

        // Step 3
        root.addView(text("3. Lock starts each morning at", 18f, bold = true).apply { setPadding(0, 24.dp(), 0, 0) })
        val hourRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val hourText = text(hourLabel(Prefs.startHour(this)), 18f).apply { setPadding(16.dp(), 0, 16.dp(), 0) }
        hourRow.addView(button("-") {
            Prefs.setStartHour(this, Prefs.startHour(this) - 1)
            hourText.text = hourLabel(Prefs.startHour(this))
            Reminder.schedule(this)
        })
        hourRow.addView(hourText)
        hourRow.addView(button("+") {
            Prefs.setStartHour(this, Prefs.startHour(this) + 1)
            hourText.text = hourLabel(Prefs.startHour(this))
            Reminder.schedule(this)
        })
        root.addView(hourRow)

        // Gentle reminder before the lock starts
        root.addView(text("Reminder before the lock", 16f, bold = true).apply { setPadding(0, 16.dp(), 0, 0) })
        val remRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val remText = text(reminderLabel(Prefs.reminderMinutes(this)), 16f)
            .apply { setPadding(16.dp(), 0, 16.dp(), 0) }
        fun changeReminder(delta: Int) {
            Prefs.setReminderMinutes(this, Prefs.reminderMinutes(this) + delta)
            remText.text = reminderLabel(Prefs.reminderMinutes(this))
            requestNotificationPermission()
            Reminder.schedule(this)
        }
        remRow.addView(button("-") { changeReminder(-15) })
        remRow.addView(remText)
        remRow.addView(button("+") { changeReminder(15) })
        root.addView(remRow)

        // Step 4
        root.addView(text("4. Turn it on", 18f, bold = true).apply { setPadding(0, 24.dp(), 0, 0) })
        root.addView(Switch(this).apply {
            this.text = "Morning lock"
            isChecked = Prefs.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, on ->
                Prefs.setEnabled(this@MainActivity, on)
                if (on) requestNotificationPermission()
                Reminder.schedule(this@MainActivity)
                render()
            }
        })

        // Daily actions
        root.addView(text("Today", 18f, bold = true).apply { setPadding(0, 24.dp(), 0, 0) })
        root.addView(button("I've finished my reading") {
            Prefs.markDone(this)
            render()
        })
        root.addView(button("Reset today (for testing)") {
            Prefs.clearDone(this)
            render()
        })
        root.addView(text("Emergency overrides used: ${Prefs.overrideCount(this)}", 13f).apply {
            setPadding(0, 12.dp(), 0, 0)
        })
    }

    private fun hourLabel(h: Int) = "%02d:00".format(h)

    private fun reminderLabel(m: Int) = if (m == 0) "Off" else "$m min before"

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun text(s: String, sp: Float, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = sp
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { onClick() }
    }
}
