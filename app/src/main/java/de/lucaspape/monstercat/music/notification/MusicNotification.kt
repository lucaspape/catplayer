package de.lucaspape.monstercat.music.notification

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.MonstercatPlayer.Companion.contextReference
import de.lucaspape.monstercat.music.MonstercatPlayer.Companion.mediaPlayer
import de.lucaspape.monstercat.music.MonstercatPlayer.Companion.mediaSession
import de.lucaspape.monstercat.music.stop

internal const val PLAY_PAUSE_ACTION = "de.lucaspape.monstercat.playpause"
internal const val NEXT_ACTION = "de.lucaspape.monstercat.next"
internal const val PREV_ACTION = "de.lucaspape.monstercat.prev"
internal const val CLOSE_ACTION = "de.lucaspape.monstercat.close"

//notification var
internal const val musicChannelID = "Music Notification"
internal const val musicNotificationID = 1

private var lastButtonPress: Long = 0

private var playerServiceIntent: Intent? = null

private var serviceRunning = false

internal fun createPlayerNotification(
    title: String,
    version: String,
    artist: String,
    coverLocation: String
): Notification? {
    contextReference?.get()?.let { context ->
        val notificationBuilder = NotificationCompat.Builder(
            context,
            musicChannelID
        )

        val notificationStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        val prevIntent = Intent(PREV_ACTION)
        val playPauseIntent = Intent(PLAY_PAUSE_ACTION)
        val nextIntent = Intent(NEXT_ACTION)
        val closeIntent = Intent(CLOSE_ACTION)

        val prevPendingIntent =
            PendingIntent.getBroadcast(context, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextPendingIntent =
            PendingIntent.getBroadcast(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val closePendingIntent =
            PendingIntent.getBroadcast(context, 0, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        context.registerReceiver(
            NotificationIntentReceiver(), IntentFilter(
                PREV_ACTION
            )
        )
        context.registerReceiver(
            NotificationIntentReceiver(), IntentFilter(
                PLAY_PAUSE_ACTION
            )
        )
        context.registerReceiver(
            NotificationIntentReceiver(), IntentFilter(
                NEXT_ACTION
            )
        )
        context.registerReceiver(
            NotificationIntentReceiver(), IntentFilter(
                CLOSE_ACTION
            )
        )

        val prevAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_previous_black_24dp,
            PREV_ACTION, prevPendingIntent
        ).build()

        val playPauseIcon: Int = if (mediaPlayer?.isPlaying == true) {
            R.drawable.ic_pause_black_24dp
        } else {
            R.drawable.ic_play_arrow_black_24dp
        }

        val playPauseAction = NotificationCompat.Action.Builder(
            playPauseIcon,
            PLAY_PAUSE_ACTION, playPausePendingIntent
        ).build()
        val nextAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_next_black_24dp,
            NEXT_ACTION, nextPendingIntent
        ).build()
        val closeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_close_black_24dp,
            CLOSE_ACTION, closePendingIntent
        ).build()

        notificationBuilder
            .setShowWhen(false)
            .setStyle(notificationStyle)
            .setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
            .setLargeIcon(BitmapFactory.decodeFile(coverLocation))
            .setColor(Color.LTGRAY)
            .setContentTitle("$title $version")
            .setContentText(artist)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(closeAction)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        return notificationBuilder.build()
    }

    return null
}

class NotificationIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (System.currentTimeMillis() - lastButtonPress > 300) {
            when (intent?.action) {
                PREV_ACTION -> previous()
                PLAY_PAUSE_ACTION -> toggleMusic()
                NEXT_ACTION -> next()
                CLOSE_ACTION -> stop()
            }

            lastButtonPress = System.currentTimeMillis()
        }
    }
}

/**
 * Show notification
 */
internal fun startPlayerService(
) {
    if (!serviceRunning) {
        playerServiceIntent =
            Intent(contextReference!!.get()!!, PlayerService::class.java)

        playerServiceIntent?.putExtra("title", "")
        playerServiceIntent?.putExtra("version", "")
        playerServiceIntent?.putExtra("artist", "")
        playerServiceIntent?.putExtra("coverLocation", "")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            contextReference?.get()?.startForegroundService(
                playerServiceIntent
            )
        } else {
            contextReference?.get()?.startService(
                playerServiceIntent
            )
        }

        serviceRunning = true
    }
}

fun stopPlayerService() {
    playerServiceIntent?.let {
        contextReference?.get()?.stopService(
            playerServiceIntent
        )
    }

    serviceRunning = false
}

fun updateNotification(
    title: String,
    version: String,
    artist: String,
    coverLocation: String
) {
    val notificationManager =
        contextReference?.get()?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
    notificationManager?.notify(
        musicNotificationID,
        createPlayerNotification(
            title,
            version,
            artist,
            coverLocation
        )
    )
}

internal fun createNotificationChannel() {
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