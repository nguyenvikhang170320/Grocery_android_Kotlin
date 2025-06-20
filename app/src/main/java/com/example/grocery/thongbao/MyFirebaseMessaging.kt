package com.example.grocery.thongbao

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.grocery.R
import com.example.grocery.activities.OrderDetailsSellerActivity
import com.example.grocery.activities.OrderDetailsUsersActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class MyFirebaseMessaging : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser ?: return

        val data = remoteMessage.data
        Log.d("FCM", "onMessageReceived: $data")

        val notificationType = data["notificationType"] ?: return
        val buyerUid = data["buyerUid"]
        val sellerUid = data["sellerUid"]
        val orderId = data["orderId"]
        val title = data["notificationTitle"] ?: "Thông báo"
        val message = data["notificationMessage"] ?: "Bạn có thông báo mới"

        val currentUid = firebaseUser.uid

        if (orderId.isNullOrBlank() || buyerUid.isNullOrBlank() || sellerUid.isNullOrBlank()) {
            Log.w("FCM", "Thiếu dữ liệu cần thiết trong notification")
            return
        }

        when (notificationType) {
            "NewOrder" -> {
                if (currentUid == sellerUid) {
                    val intent = Intent(this, OrderDetailsSellerActivity::class.java).apply {
                        putExtra("orderId", orderId)
                        putExtra("orderBy", buyerUid)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    showNotification(title, message, intent)
                }
            }

            "OrderStatusChanged" -> {
                if (currentUid == buyerUid) {
                    val intent = Intent(this, OrderDetailsUsersActivity::class.java).apply {
                        putExtra("orderId", orderId)
                        putExtra("orderTo", sellerUid)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    showNotification(title, message, intent)
                }
            }

            else -> Log.d("FCM", "Loại thông báo không xác định: $notificationType")
        }
    }


    private fun showNotification(title: String, message: String, intent: Intent) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = Random().nextInt(3000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // tránh bị ghi đè khi nhiều noti
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )


        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.icon)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setColor(Color.GREEN)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(notificationId, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(manager: NotificationManager) {
        val channelName = "Thông báo đơn hàng"
        val channelDescription = "Thông báo về đơn hàng mới và trạng thái đơn hàng"
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        val update = hashMapOf<String, Any>("fcmToken" to token)
        ref.child(uid).updateChildren(update)
        Log.d("FCM", "New FCM token: $token")
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ORDER_NOTIFICATION_CHANNEL"
    }
}
