package com.worksessions.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build

object NotificationHelper {
    const val CHANNEL_PROGRESS = "progress"
    const val CHANNEL_DONE = "done_v3" // تغيير المعرّف لإجبار أندرويد على تطبيق إعدادات الصوت الجديدة

    /** رابط الصوت المخصّص المرفق مع التطبيق (res/raw/timer_done.wav) */
    fun doneSoundUri(ctx: Context): Uri =
        Uri.parse("android.resource://${ctx.packageName}/${R.raw.timer_done}")

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (nm.getNotificationChannel(CHANNEL_PROGRESS) == null) {
            val ch = NotificationChannel(
                CHANNEL_PROGRESS, "جلسة قيد التنفيذ", NotificationManager.IMPORTANCE_LOW
            )
            ch.setSound(null, null)
            ch.setShowBadge(false)
            ch.enableVibration(false)
            nm.createNotificationChannel(ch)
        }

        if (nm.getNotificationChannel(CHANNEL_DONE) == null) {
            val ch = NotificationChannel(
                CHANNEL_DONE, "انتهاء الجلسة", NotificationManager.IMPORTANCE_HIGH
            )
            // صوت مخصّص مرفق مع التطبيق (نغمة صاعدة ~3 ثوانٍ) بدل صوت النظام الافتراضي
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ch.setSound(doneSoundUri(ctx), attrs)
            ch.enableVibration(true)
            ch.vibrationPattern = longArrayOf(0, 1000, 300, 1000, 300, 1000)
            nm.createNotificationChannel(ch)
        }
    }
}
