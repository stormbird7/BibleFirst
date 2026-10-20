package com.biblefirst

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony

/** Works out which apps may be used while the phone is locked. */
object AllowList {

    // Calls, texts, maps, alarms, keyboards and system pieces. Includes Samsung packages.
    private val known = setOf(
        // system
        "com.android.systemui",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        // calls
        "com.android.server.telecom", "com.android.phone", "com.android.incallui",
        "com.android.dialer", "com.google.android.dialer",
        "com.samsung.android.dialer", "com.samsung.android.incallui",
        "com.samsung.android.app.telephonyui",
        // texts
        "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms",
        // maps
        "com.google.android.apps.maps",
        // alarms (so your morning alarm can still be turned off)
        "com.google.android.deskclock", "com.android.deskclock", "com.sec.android.app.clockpackage",
        // keyboards
        "com.samsung.android.honeyboard", "com.google.android.inputmethod.latin",
        // PROTOTYPE SAFETY NET: lets you reach Accessibility settings to switch the service off.
        // Remove this line later if you want the lock to be stricter.
        "com.android.settings"
    )

    fun build(c: Context): Set<String> {
        val pm = c.packageManager
        val result = HashSet<String>(known)
        result += c.packageName
        result += Prefs.bibleApps(c)

        fun addAll(intent: Intent) {
            pm.queryIntentActivities(intent, 0).forEach { result += it.activityInfo.packageName }
        }

        // Home screen(s) must stay reachable.
        addAll(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        // Whatever the user has set as phone, texts and maps.
        addAll(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
        addAll(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=cafe")))
        addAll(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))
        Telephony.Sms.getDefaultSmsPackage(c)?.let { result += it }

        return result
    }

    /** Catch-all for third-party keyboards. */
    fun looksLikeKeyboard(pkg: String): Boolean =
        pkg.contains("inputmethod") || pkg.contains("keyboard") || pkg.contains("honeyboard")
}
