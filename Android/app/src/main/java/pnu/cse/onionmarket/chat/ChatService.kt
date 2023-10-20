package pnu.cse.onionmarket.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import pnu.cse.onionmarket.MainActivity
import pnu.cse.onionmarket.R

class ChatService: FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val name = "채팅 알림"
        val descriptionText = "채팅 알림 입니다."
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(getString(R.string.default_notification_channel_id),name,importance)
        mChannel.description = descriptionText

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
        val data = message.data
        val title = data["title"] ?: ""
        val body = data["body"] ?: ""
        val chatRoomId = data["chatRoomId"]
        val otherUserId = data["otherUserId"]

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("chatRoomId",chatRoomId)
        intent.putExtra("otherUserId",otherUserId)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}