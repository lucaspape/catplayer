package de.lucaspape.monstercat.music

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v4.media.session.MediaSessionCompat
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.palette.graphics.Palette
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import de.lucaspape.monstercat.R

//notification var
const val musicChannelID = "Music Notification"
const val musicNotificationID = 1

private const val NOTIFICATION_PREVIOUS = "de.lucaspape.monstercat.previous"
private const val NOTIFICATION_DELETE = "de.lucaspape.monstercat.delete"
private const val NOTIFICATION_PAUSE = "de.lucaspape.monstercat.pause"
private const val NOTIFICATION_PLAY = "de.lucaspape.monstercat.play"
private const val NOTIFICATION_NEXT = "de.lucaspape.monstercat.next"

/**
 * Show notification
 */
internal fun createSongNotification(
    title: String,
    version: String,
    artist: String,
    coverLocation: String,
    playing: Boolean
) {
    val playerNotificationManager = PlayerNotificationManager(contextReference!!.get()!!, musicChannelID, musicNotificationID, object:
        PlayerNotificationManager.MediaDescriptionAdapter{
        override fun getCurrentLargeIcon(
            player: Player?,
            callback: PlayerNotificationManager.BitmapCallback?
        ): Bitmap? {
            return BitmapFactory.decodeFile(coverLocation)
        }

        override fun getCurrentContentTitle(player: Player?): String {
            return title
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

/**
 * Get the dominant color of a bitmap image (use for notification background color)
 */
fun getDominantColor(bitmap: Bitmap): Int {
    val swatchesTemp = Palette.from(bitmap).generate().swatches
    val swatches = ArrayList<Palette.Swatch>(swatchesTemp)

    swatches.sortWith(Comparator { swatch1, swatch2 -> swatch2.population - swatch1.population })

    return if (swatches.size > 0) swatches[0].rgb else Color.WHITE
}