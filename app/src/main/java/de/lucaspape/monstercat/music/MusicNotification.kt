package de.lucaspape.monstercat.music

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.NotificationListener

//notification var
const val musicChannelID = "Music Notification"
const val musicNotificationID = 1

/**
 * Show notification
 */
internal fun createSongNotification(
    title: String,
    version: String,
    artist: String,
    coverLocation: String
) {
    val notificationServiceIntent =
        Intent(contextReference!!.get()!!, MusicNotificationService::class.java)

    notificationServiceIntent.putExtra("title", title)
    notificationServiceIntent.putExtra("version", version)
    notificationServiceIntent.putExtra("artist", artist)
    notificationServiceIntent.putExtra("coverLocation", coverLocation)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        contextReference!!.get()!!.startForegroundService(notificationServiceIntent)
    } else {
        contextReference!!.get()!!.startService(notificationServiceIntent)
    }
}

private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        contextReference!!.get()?.let { context ->
            val channelName = "Music Notification"
            val channelDescription = "Handy dandy description"
            val importance = NotificationManager.IMPORTANCE_LOW

            val notificationChannel = NotificationChannel(musicChannelID, channelName, importance)

            notificationChannel.description = channelDescription

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}

class MusicNotificationService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent!!.getStringExtra("title")!!
        val version = intent.getStringExtra("version")!!
        val artist = intent.getStringExtra("artist")!!
        val coverLocation = intent.getStringExtra("coverLocation")!!

        createNotificationChannel()

        val playerNotificationManager = PlayerNotificationManager(
            contextReference!!.get()!!,
            musicChannelID,
            musicNotificationID,
            object :
                PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentLargeIcon(
                    player: Player?,
                    callback: PlayerNotificationManager.BitmapCallback?
                ): Bitmap? {
                    return BitmapFactory.decodeFile(coverLocation)
                }

                override fun getCurrentContentTitle(player: Player?): String {
                    return "$title $version"
                }

                override fun getCurrentContentText(player: Player?): String? {
                    return artist
                }

                override fun createCurrentContentIntent(player: Player?): PendingIntent? {
                    return null
                }
            },object : NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification?,
                    ongoing: Boolean
                ) {
                    startForeground(
                        notificationId,
                        notification
                    )
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopSelf()
                }
            })

        playerNotificationManager.setPlayer(mediaPlayer)

        playerNotificationManager.setRewindIncrementMs(0)
        playerNotificationManager.setFastForwardIncrementMs(0)

        playerNotificationManager.setMediaSessionToken(mediaSession!!.sessionToken)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

}
