package de.lucaspape.monstercat.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.activities.MainActivity

class PushNotificationService :FirebaseMessagingService(){
    var nId = 0

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)

        val title = p0.data["title"]
        val msg = p0.data["msg"]
        val url = p0.data["url"]

        title?.let {
            msg?.let {
                sendNotification(title, msg, url)
            }
        }
    }

    private fun sendNotification(title:String, msg:String, url:String?){
        val pendingIntent = if(url != null){
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }else{
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.pushNotificationChannelId))
            .setContentTitle(title)
            .setContentText(msg)
            .setContentIntent(pendingIntent)
            .setContentInfo(title)
            .setSmallIcon(R.drawable.ic_icon)
            .setAutoCancel(true)
            .setOngoing(false)

        createNotificationChannel(this)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nId++

        notificationManager.notify(nId, notificationBuilder.build())
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.getString(R.string.pushNotificationChannelId)
            val channelDescription = context.getString(R.string.pushNotificationDescription)
            val importance = NotificationManager.IMPORTANCE_LOW

            val notificationChannel = NotificationChannel(
                context.getString(R.string.pushNotificationChannelId),
                channelName,
                importance
            )

            notificationChannel.description = channelDescription

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}