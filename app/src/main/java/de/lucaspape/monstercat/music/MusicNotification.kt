package de.lucaspape.monstercat.music

import android.app.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager

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
    createNotificationChannel()

    val playerNotificationManager = PlayerNotificationManager(contextReference!!.get()!!, musicChannelID, musicNotificationID, object:
        PlayerNotificationManager.MediaDescriptionAdapter{
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
    })

    playerNotificationManager.setPlayer(mediaPlayer)

    playerNotificationManager.setRewindIncrementMs(0)
    playerNotificationManager.setFastForwardIncrementMs(0)

    playerNotificationManager.setMediaSessionToken(mediaSession!!.sessionToken)
}

private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val context = contextReference!!.get()!!

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