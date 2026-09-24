package com.example.securemate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class SecureMateApplication : Application() {

    lateinit var container: SecureMateAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = SecureMateAppContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = CHANNEL_SECURITY_ALERTS
            val name = "Security Alerts & Monitoring"
            val descriptionText = "Alerts for security score changes, app risk factors, and device hygiene reminders."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_SECURITY_ALERTS = "securemate_security_alerts"
    }
}
