package com.biblefirst

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** The screen shown over any blocked app until today's reading is marked complete. */
class LockActivity : Activity() {

    private var timer: CountDownTimer? = null

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (!Prefs.isLocked(this)) finish()
    }

    @Deprecated("Back goes to the home screen, not to the blocked app")
    override fun onBackPressed() {
        leaveToHome()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24.dp(), 48.dp(), 24.dp(), 24.dp())
        }

        root.addView(label("Reading first", 30f, bold = true))
        root.addView(label("Do today's Bible reading, then your phone is yours again.", 16f).apply {
            setPadding(0, 8.dp(), 0, 24.dp())
        })

        // Bible apps chosen in settings
        val pm = packageManager
        val apps = Prefs.bibleApps(this)
        if (apps.isEmpty()) {
            root.addView(label("No Bible app chosen yet. Pick one in Bible First settings.", 15f))
            root.addView(button("Open Bible First settings") {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            })
        } else {
            for (pkg in apps) {
                val intent = pm.getLaunchIntentForPackage(pkg) ?: continue
                val name = try {
                    pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
                } catch (e: Exception) { pkg }
                root.addView(button("Open $name") {
                    startActivity(intent)
                })
            }
        }

        root.addView(button("I've finished my reading") { confirmDone() }.apply {
            setBackgroundColor(Color.parseColor("#2E7D32"))
            setTextColor(Color.WHITE)
        })

        root.addView(label("Always available", 14f).apply { setPadding(0, 28.dp(), 0, 4.dp()) })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(smallButton("Phone") { launch(Intent(Intent.ACTION_DIAL)) })
        row.addView(smallButton("Texts") {
            launch(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING))
        })
        row.addView(smallButton("Maps") {
            launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")))
        })
        root.addView(row)

        // Emergency override
        val overrideBtn = button("Emergency override") {}
        overrideBtn.setOnClickListener { startOverrideCountdown(overrideBtn) }
        overrideBtn.setBackgroundColor(Color.parseColor("#B71C1C"))
        overrideBtn.setTextColor(Color.WHITE)
        root.addView(overrideBtn.apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = 32.dp()
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun confirmDone() {
        AlertDialog.Builder(this)
            .setTitle("Be honest")
            .setMessage("Have you really finished today's reading?")
            .setPositiveButton("Yes, I have") { _, _ ->
                Prefs.markDone(this)
                leaveToHome()
            }
            .setNegativeButton("Not yet", null)
            .show()
    }

    /** 60-second wait, then 10 minutes of normal phone use. */
    private fun startOverrideCountdown(btn: Button) {
        timer?.cancel()
        btn.isEnabled = false
        timer = object : CountDownTimer(60_000, 1_000) {
            override fun onTick(msLeft: Long) {
                btn.text = "Unlocking in ${msLeft / 1000}s... (leave this screen to cancel)"
            }

            override fun onFinish() {
                Prefs.grantOverride(this@LockActivity, 10)
                finish()
            }
        }.start()
    }

    override fun onPause() {
        // Leaving the screen cancels a pending override.
        timer?.cancel()
        super.onPause()
    }

    private fun launch(i: Intent) {
        try { startActivity(i) } catch (e: ActivityNotFoundException) { /* no such app */ }
    }

    private fun leaveToHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private fun label(text: String, sp: Float, bold: Boolean = false) = TextView(this).apply {
        this.text = text
        textSize = sp
        gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 12.dp() }
        setOnClickListener { onClick() }
    }

    private fun smallButton(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        setOnClickListener { onClick() }
    }
}
