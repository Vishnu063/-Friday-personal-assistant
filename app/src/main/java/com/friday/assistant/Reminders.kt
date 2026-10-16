package com.friday.assistant

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object Reminders {
    private fun pending(context: Context, id: String) = PendingIntent.getBroadcast(context, 0,
        Intent(context, ReminderReceiver::class.java).setData(android.net.Uri.parse("friday://reminder/$id")).putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun schedule(context: Context, id: String, due: Long) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending(context, id))
    }
    fun cancel(context: Context, id: String) = (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(context, id))
}
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = Store(context)
        val entry = try { store.find(intent.getStringExtra("id").orEmpty())?.takeIf { it.kind == "task" && !it.done } } finally { store.close() } ?: return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("reminders", "Friday reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        manager.notify(entry.id.hashCode(), NotificationCompat.Builder(context, "reminders").setSmallIcon(R.drawable.ic_friday)
            .setContentTitle("A little nudge from Friday").setContentText(entry.text).setStyle(NotificationCompat.BigTextStyle().bigText(entry.text))
            .setContentIntent(open).setAutoCancel(true).build())
    }
}
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = Store(context)
        try { store.pendingTasks().forEach { Reminders.schedule(context, it.id, it.due) } } finally { store.close() }
    }
}
