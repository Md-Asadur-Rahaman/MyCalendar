package com.mycalendar.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class EventAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_EVENT) return
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Calendar reminder"
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = openIntent?.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("MyCalendar")
            .setContentText(title)
            .setStyle(Notification.BigTextStyle().bigText(title))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(id.toString()).apply()
    }

    companion object {
        const val CHANNEL_ID = "mycalendar_reminders"
        const val ACTION_EVENT = "com.mycalendar.app.EVENT_REMINDER"
        const val EXTRA_ID = "event_id"
        const val EXTRA_TITLE = "event_title"
        const val PREFS = "mycalendar_scheduled_events"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences(EventAlarmReceiver.PREFS, Context.MODE_PRIVATE)
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        prefs.all.forEach { (key, value) ->
            val parts = (value as? String)?.split("|", limit = 2) ?: return@forEach
            val time = parts.getOrNull(0)?.toLongOrNull() ?: return@forEach
            if (time <= System.currentTimeMillis()) return@forEach
            val id = key.toIntOrNull() ?: return@forEach
            val alarmIntent = Intent(context, EventAlarmReceiver::class.java).apply {
                action = EventAlarmReceiver.ACTION_EVENT
                putExtra(EventAlarmReceiver.EXTRA_ID, id)
                putExtra(EventAlarmReceiver.EXTRA_TITLE, parts.getOrNull(1) ?: "Calendar reminder")
            }
            val pending = PendingIntent.getBroadcast(
                context, id, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
                } else {
                    alarms.setExact(AlarmManager.RTC_WAKEUP, time, pending)
                }
            } catch (_: SecurityException) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
            }
        }
    }
}
