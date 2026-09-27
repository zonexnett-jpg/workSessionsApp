package com.worksessions.app

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface

/**
 * الجسر الذي تستدعيه صفحة الويب (JavaScript) عبر window.Android.*
 * كل استدعاء يرسل أمرًا لخدمة المؤقت الخلفية (Foreground Service)
 * التي تستمر بالعمل حتى لو غادر المستخدم التطبيق أو أُطفئت الشاشة.
 */
class WebAppBridge(private val ctx: Context) {

    @JavascriptInterface
    fun onSessionStart(endEpochMillis: Long, categoryName: String, categoryColor: String, durationSeconds: Int) {
        val i = Intent(ctx, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_START
            putExtra(TimerForegroundService.EXTRA_END, endEpochMillis)
            putExtra(TimerForegroundService.EXTRA_CAT_NAME, categoryName)
            putExtra(TimerForegroundService.EXTRA_CAT_COLOR, categoryColor)
            putExtra(TimerForegroundService.EXTRA_DURATION, durationSeconds)
        }
        ctx.startForegroundService(i)
    }

    @JavascriptInterface
    fun onPauseToggle(paused: Boolean, newEnd: Long) {
        val i = Intent(ctx, TimerForegroundService::class.java).apply {
            action = if (paused) TimerForegroundService.ACTION_PAUSE else TimerForegroundService.ACTION_RESUME
            putExtra(TimerForegroundService.EXTRA_END, newEnd)
        }
        ctx.startService(i)
    }

    @JavascriptInterface
    fun onSessionEnd() {
        val i = Intent(ctx, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_STOP
        }
        ctx.startService(i)
    }
}
