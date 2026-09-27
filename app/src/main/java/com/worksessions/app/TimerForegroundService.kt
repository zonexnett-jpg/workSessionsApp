package com.worksessions.app

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

/**
 * خدمة تعمل في المقدمة (Foreground Service) طوال مدة الجلسة، بحيث لا يوقفها
 * النظام حتى عند إغلاق الشاشة أو تصغير التطبيق. تعرض إشعارًا حيًا بالوقت
 * المتبقي، وتضبط منبّهًا دقيقًا (AlarmManager) يضمن ظهور إشعار الانتهاء
 * وتشغيل الصوت/الاهتزاز في لحظته تمامًا حتى لو تعطلت العملية نفسها.
 */
class TimerForegroundService : Service() {

    companion object {
        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_END = "extra_end"
        const val EXTRA_CAT_NAME = "extra_cat_name"
        const val EXTRA_CAT_COLOR = "extra_cat_color"
        const val EXTRA_DURATION = "extra_duration"
        const val NOTIF_ID = 42
    }

    private val handler = Handler(Looper.getMainLooper())
    private var endEpoch = 0L
    private var paused = false
    private var catName = ""

    private val ticker = object : Runnable {
        override fun run() {
            if (paused) return
            updateNotification()
            if (System.currentTimeMillis() >= endEpoch) return
            handler.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannels(this)
        when (intent?.action) {
            ACTION_START -> {
                endEpoch = intent.getLongExtra(EXTRA_END, System.currentTimeMillis())
                catName = intent.getStringExtra(EXTRA_CAT_NAME) ?: ""
                paused = false
                startForeground(NOTIF_ID, buildNotification("جارٍ العمل"))
                handler.removeCallbacks(ticker)
                handler.post(ticker)
                scheduleAlarm()
            }
            ACTION_PAUSE -> {
                paused = true
                cancelAlarm()
                startForeground(NOTIF_ID, buildNotification("متوقف مؤقتًا"))
            }
            ACTION_RESUME -> {
                endEpoch = intent.getLongExtra(EXTRA_END, endEpoch)
                paused = false
                handler.removeCallbacks(ticker)
                handler.post(ticker)
                scheduleAlarm()
            }
            ACTION_STOP -> {
                cancelAlarm()
                handler.removeCallbacks(ticker)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun scheduleAlarm() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = TimerAlarmReceiver.buildPendingIntent(this, catName)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endEpoch, pi)
        } catch (e: SecurityException) {
            // بعض الأجهزة تمنع المنبهات الدقيقة دون إذن خاص؛ الإشعار الحي يبقى يعمل كخط دفاع أول
        }
    }

    private fun cancelAlarm() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(TimerAlarmReceiver.buildPendingIntent(this, catName))
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(if (paused) "متوقف مؤقتًا" else "جارٍ العمل"))
    }

    private fun buildNotification(status: String): Notification {
        val remain = ((endEpoch - System.currentTimeMillis()).coerceAtLeast(0)) / 1000
        val mm = remain / 60
        val ss = remain % 60
        val time = String.format("%02d:%02d", mm, ss)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(this, 0, openIntent, flags)

        val title = if (catName.isNotEmpty()) "$catName — $status" else status
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_PROGRESS)
            .setContentTitle(title)
            .setContentText("الوقت المتبقي: $time")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
