package com.friday.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Keeps an explicitly started microphone conversation visible while Friday is backgrounded. */
class VoiceSessionService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        val stopIntent = Intent(this, MainActivity::class.java)
            .setAction(ACTION_STOP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val stopPending = PendingIntent.getActivity(
            this,
            REQUEST_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Friday voice session is active")
            .setContentText("Friday may listen and reply while you use another app.")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause, "Stop", stopPending).build())
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Friday voice session", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Shows when Friday is actively listening in a background conversation."
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.friday.assistant.action.VOICE_START"
        const val ACTION_STOP = "com.friday.assistant.action.VOICE_STOP"
        const val ACTION_STOPPED = "com.friday.assistant.action.VOICE_STOPPED"
        private const val CHANNEL_ID = "friday_voice_session"
        private const val NOTIFICATION_ID = 73
        private const val REQUEST_STOP = 74
    }
}
