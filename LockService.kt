package com.biblefirst

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the front. While the phone is locked, anything that
 * isn't on the allow-list is covered by the reading screen (LockActivity).
 */
class LockService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var allow: Set<String> = emptySet()
    private var allowBuiltAt = 0L
    private var lastPkg: String? = null

    // Re-check every few seconds so an expired override or a new morning
    // takes effect even if the user stays inside the same app.
    private val recheck = object : Runnable {
        override fun run() {
            lastPkg?.let { enforce(it) }
            handler.postDelayed(this, 5_000)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.removeCallbacks(recheck)
        handler.postDelayed(recheck, 5_000)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        lastPkg = pkg
        enforce(pkg)
    }

    private fun enforce(pkg: String) {
        if (!Prefs.isLocked(this)) return
        if (System.currentTimeMillis() - allowBuiltAt > 30_000) {
            allow = AllowList.build(this)
            allowBuiltAt = System.currentTimeMillis()
        }
        if (pkg in allow || AllowList.looksLikeKeyboard(pkg)) return

        startActivity(
            Intent(this, LockActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        )
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(recheck)
        super.onDestroy()
    }
}
