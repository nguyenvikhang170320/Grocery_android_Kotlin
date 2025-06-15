package com.example.grocery.thongbao

import android.annotation.SuppressLint
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
import com.example.grocery.activities.OrderDetailsSellerActivity
import com.example.grocery.activities.OrderDetailsUsersActivity
import com.example.grocery.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

/*
* Mọi dữ liệu hiện thông báo message order seller hiện ở đây các bạn có thể tham khảo
* Có liên quan đến activity OderDetailsSeller dòng 131
* Các bạn có thể sửa code bên trong này lại chút và bên activity liên quan là có thể hiện được thông báo
* Do cloud message firebase đã nâng cấp nên đoạn code cũ này có 1 phần không được hỗ trợ nữa, nên không thể hiện thông báo như ban đầu được
* Các bạn có thể tham khảo thêm cloud message firebase trên youtube */
class MyFirebaseMessaging : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        //all notifications will be received here
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser

        //get data from notification
        val notificationType = remoteMessage.data["notificationType"]!!
        if (notificationType == "NewOrder") {
            val buyerUid = remoteMessage.data["buyerUid"]
            val sellerUid = remoteMessage.data["sellerUid"]
            val orderId = remoteMessage.data["orderId"]
            val notificationTitle = remoteMessage.data["notificationTitle"]
            val notificationDescription = remoteMessage.data["notificationMessage"]
            if (firebaseUser != null && firebaseAuth.uid == sellerUid) {
                //user is signed in and is same user to which notification is sent
                showNotification(
                    orderId,
                    sellerUid,
                    buyerUid,
                    notificationTitle,
                    notificationDescription,
                    notificationType
                )
            }
        }
        if (notificationType == "OrderStatusChanged") {
            val buyerUid = remoteMessage.data["buyerUid"]
            val sellerUid = remoteMessage.data["sellerUid"]
            val orderId = remoteMessage.data["orderId"]
            val notificationTitle = remoteMessage.data["notificationTitle"]
            val notificationDescription = remoteMessage.data["notificationMessage"]
            if (firebaseUser != null && firebaseAuth.uid == buyerUid) {
                //user is signed in and is same user to which notification is sent
                showNotification(
                    orderId,
                    sellerUid,
                    buyerUid,
                    notificationTitle,
                    notificationDescription,
                    notificationType
                )
            }
        }
    }

    private fun showNotification(
        orderId: String?,
        sellerUid: String?,
        buyerUid: String?,
        notificationTitle: String?,
        notificationDescription: String?,
        notificationType: String
    ) {
        //notification
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        //id for notification, random
        val notificationID = Random().nextInt(3000)

        //check if android version is Oreo/O or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationChannel(notificationManager)
        }

        //handle notification click, start order activity
        var intent: Intent? = null
        if (notificationType == "NewOrder") {
            //open OrderDetailsSellerActivity
            intent = Intent(this, OrderDetailsSellerActivity::class.java)
            intent.putExtra("orderId", orderId)
            intent.putExtra("orderBy", buyerUid)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        } else if (notificationType == "OrderStatusChanged") {
            //open OrderDetailsUsersActivity
            intent = Intent(this, OrderDetailsUsersActivity::class.java)
            intent.putExtra("orderId", orderId)
            intent.putExtra("orderTo", sellerUid)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        @SuppressLint("UnspecifiedImmutableFlag") val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        //Large icon
        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.icon)

        //sound of notification
        val notificationSounUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        notificationBuilder.setSmallIcon(R.drawable.icon)
            .setLargeIcon(largeIcon)
            .setContentTitle(notificationTitle)
            .setContentText(notificationDescription)
            .setSound(notificationSounUri)
            .setAutoCancel(true) //cancel/dismiss when clicked
            .setContentIntent(pendingIntent) //add intent

        //show notification
        notificationManager.notify(notificationID, notificationBuilder.build())
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupNotificationChannel(notificationManager: NotificationManager?) {
        val channelName: CharSequence = "Some Sample Text"
        val channelDescription = "Channel Description here"
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.description = channelDescription
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.enableVibration(true)
        notificationManager?.createNotificationChannel(notificationChannel)
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        Log.d("SERVICE_TAG", "onNewToken: ")
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID =
            "MY_NOTIFICATION_CHANNEL_ID" //required for android O and above
    }
}
