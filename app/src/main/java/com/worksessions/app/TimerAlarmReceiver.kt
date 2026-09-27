package com.worksessions.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * يُطلق تمامًا في لحظة انتهاء الجلسة عبر AlarmManager، مستقلًا عن حالة
 * تطبيق الويب (حتى لو كان التطبيق مغلقًا تمامًا)، ليضمن وصول إشعار
 * الانتهاء مع الصوت والاهتزاز دون انقطاع.
 */
class TimerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIF_ID_DONE = 43

        fun buildPendingIntent(ctx: Context, catName: String): PendingIntent {
            val i = Intent(ctx, TimerAlarmReceiver::class.java).apply {
                putExtra("cat", catName)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
            return PendingIntent.getBroadcast(ctx, 0, i, flags)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.ensureChannels(context)
        val cat = intent.getStringExtra("cat") ?: ""

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(context, 1, openIntent, flags)

        val soundUri = NotificationHelper.doneSoundUri(context)
        val notif = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_DONE)
            .setContentTitle("انتهت الجلسة ✓")
            .setContentText(if (cat.isNotEmpty()) "$cat — أحسنت!" else "أحسنت!")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri) // احتياطي للأجهزة الأقدم من أندرويد 8 التي لا تعتمد على قناة الإشعار
            .setVibrate(longArrayOf(0, 1000, 300, 1000, 300, 1000))
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_DONE, notif)

        context.stopService(Intent(context, TimerForegroundService::class.java))
    }
}
