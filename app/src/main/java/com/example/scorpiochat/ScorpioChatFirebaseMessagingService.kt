package com.example.scorpiochat


import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.example.scorpiochat.ui.activities.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


private const val notificationDescription = "New message notification"
private const val channelId = "Channel ID"

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class ScorpioChatFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        createNotificationChannel()

        val userName = remoteMessage.data["title"]
        val messageText = remoteMessage.data["message"]
        val senderId = remoteMessage.data["senderId"]
        val time = remoteMessage.data["time"]?.takeLast(9)?.toInt()
        val actionWithMessage = remoteMessage.data["actionWithMessage"]

        if (userName != null && messageText != null && senderId != null) {
            when (actionWithMessage) {
                editThisMessage -> {
                    val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notifications: Array<StatusBarNotification> = notificationManager.activeNotifications
                    for (notification in notifications) {
                        if (notification.id == time) {
                            sendNotification(messageText, userName, senderId, time)
                        }
                    }
                }

                deleteThisMessage -> {
                    NotificationManagerCompat.from(this).cancel(time!!)
                }

                else -> {
                    sendNotification(messageText, userName, senderId, time)
                }
            }
        }
    }


    private fun sendNotification(messageText: String, userName: String, senderId: String, time: Int?) {
        if (!SharedPreferencesManager.getIfUserIsMuted(applicationContext, senderId)) {
            val pendingIntent = NavDeepLinkBuilder(applicationContext)
                .setComponentName(MainActivity::class.java)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.conversationFragment)
                .setArguments(bundleOf("userId" to senderId))
                .createPendingIntent()

            val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(userName)
                .setContentText(messageText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            with(NotificationManagerCompat.from(applicationContext)) {
                notify(time!!, builder.build())
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val name = notificationDescription
            val notificationDescription = notificationDescription
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = notificationDescription
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
