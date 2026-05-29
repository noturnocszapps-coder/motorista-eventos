package com.example.data.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppNotification(val title: String, val body: String, val timestamp: Long = System.currentTimeMillis())

object RoxouNotificationManager {
    private const val TAG = "RoxouNotification"
    private const val CHANNEL_ID = "roxou_alerts_channel"
    private const val CHANNEL_NAME = "Alertas Reserva Roxou"
    private const val CHANNEL_DESC = "Notificações urgentes sobre suas reservas e mensagens de motoristas"

    private val _inAppPushFlow = MutableSharedFlow<InAppNotification>(extraBufferCapacity = 10)
    val inAppPushFlow: SharedFlow<InAppNotification> = _inAppPushFlow.asSharedFlow()

    // 1. INICIALIZAR CANAL DE NOTIFICAÇÃO NATIVO (Android 8.0+)
    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.i(TAG, "Native Notification Channel initialized.")
        }
    }

    // 2. DISPARAR NOTIFICAÇÃO NATIVA (E EM CASUÍSTICA IN-APP)
    fun sendPushNotification(context: Context, title: String, body: String) {
        Log.i(TAG, "Push Triggered: $title - $body")

        // Trigger reactive in-app banner
        _inAppPushFlow.tryEmit(InAppNotification(title, body))

        // Create notification builder
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Generate adaptive system tray icon
        val iconRes = android.R.drawable.stat_notify_chat

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(100, 200, 300, 400))
            .setContentIntent(pendingIntent)

        // Native post with permission check (Mandatory Android 13+)
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Notice: POST_NOTIFICATIONS runtime permission not granted. Native tray notification skipped.")
                    return
                }
            }
            
            val notificationId = System.currentTimeMillis().toInt()
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
            Log.i(TAG, "Native System Tray Notification posted.")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Cannot post native notification due to missing permission: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification: ${e.message}")
        }
    }
}
