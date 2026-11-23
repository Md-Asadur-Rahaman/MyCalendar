package com.mycalendar.app

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var geoCallback: GeolocationPermissions.Callback? = null
    private var geoOrigin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        webView = WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.setGeolocationEnabled(true)
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(CalendarBridge(this), "AndroidCalendar")
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false)
                } else {
                    geoOrigin = origin
                    geoCallback = callback
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        REQUEST_LOCATION
                    )
                }
            }
        }
        setContentView(webView)
        webView.loadUrl("file:///android_asset/MyCalendar_Enhanced.html")
        requestNotificationPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            geoCallback?.invoke(geoOrigin ?: "", granted, false)
            geoCallback = null
            geoOrigin = null
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EventAlarmReceiver.CHANNEL_ID,
                "Calendar reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "MyCalendar event and reminder notifications"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    inner class CalendarBridge(private val context: Context) {
        @JavascriptInterface
        fun scheduleNotification(id: String, title: String, timestamp: Long) {
            if (timestamp <= System.currentTimeMillis()) return
            val eventId = id.toIntOrNull() ?: id.hashCode()
            val intent = Intent(context, EventAlarmReceiver::class.java).apply {
                action = EventAlarmReceiver.ACTION_EVENT
                putExtra(EventAlarmReceiver.EXTRA_ID, eventId)
                putExtra(EventAlarmReceiver.EXTRA_TITLE, title.take(160))
            }
            val pending = PendingIntent.getBroadcast(
                context, eventId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pending)
                } else {
                    alarms.setExact(AlarmManager.RTC_WAKEUP, timestamp, pending)
                }
            } catch (_: SecurityException) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pending)
            }
            context.getSharedPreferences(EventAlarmReceiver.PREFS, Context.MODE_PRIVATE)
                .edit().putString(eventId.toString(), "$timestamp|${title.replace("|", " ")}").apply()
        }

        @JavascriptInterface
        fun cancelNotification(id: String) {
            val eventId = id.toIntOrNull() ?: id.hashCode()
            val intent = Intent(context, EventAlarmReceiver::class.java).apply {
                action = EventAlarmReceiver.ACTION_EVENT
            }
            val pending = PendingIntent.getBroadcast(
                context, eventId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
            context.getSharedPreferences(EventAlarmReceiver.PREFS, Context.MODE_PRIVATE)
                .edit().remove(eventId.toString()).apply()
        }

        @JavascriptInterface
        fun requestNotificationPermission() {
            runOnUiThread { this@MainActivity.requestNotificationPermission() }
        }
    }

    companion object {
        private const val REQUEST_LOCATION = 301
        private const val REQUEST_NOTIFICATIONS = 302
    }
}
