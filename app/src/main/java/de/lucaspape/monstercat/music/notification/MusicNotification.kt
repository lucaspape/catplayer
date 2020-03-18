package de.lucaspape.monstercat.music.notification

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.activities.MainActivity
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.contextReference
import java.lang.IllegalArgumentException

private const val PLAY_PAUSE_ACTION = "de.lucaspape.monstercat.playpause"
private const val NEXT_ACTION = "de.lucaspape.monstercat.next"
private const val PREV_ACTION = "de.lucaspape.monstercat.prev"
private const val CLOSE_ACTION = "de.lucaspape.monstercat.close"

internal const val musicNotificationID = 1

private var lastButtonPress: Long = 0

private var playerServiceIntent: Intent? = null

private var serviceRunning = false

private var prevReceiver: BroadcastReceiver? = null
private var playPauseReceiver: BroadcastReceiver? = null
private var nextReceiver: BroadcastReceiver? = null
private var closeReceiver: BroadcastReceiver? = null

internal fun createPlayerNotification(
    title: String,
    version: String,
    artist: String,
    bitmap: Bitmap?
): Notification? {
    contextReference?.get()?.let { context ->
        val notificationBuilder = NotificationCompat.Builder(
            context,
            context.getString(R.string.musicNotificationChannelId)
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

        try {
            context.unregisterReceiver(prevReceiver)
            context.unregisterReceiver(playPauseReceiver)
            context.unregisterReceiver(nextReceiver)
            context.unregisterReceiver(closeReceiver)
        } catch (e: IllegalArgumentException) {

        }

        prevReceiver = NotificationIntentReceiver()
        playPauseReceiver = NotificationIntentReceiver()
        nextReceiver = NotificationIntentReceiver()
        closeReceiver = NotificationIntentReceiver()

        context.registerReceiver(
            prevReceiver, IntentFilter(
                PREV_ACTION
            )
        )
        context.registerReceiver(
            playPauseReceiver, IntentFilter(
                PLAY_PAUSE_ACTION
            )
        )
        context.registerReceiver(
            nextReceiver, IntentFilter(
                NEXT_ACTION
            )
        )
        context.registerReceiver(
            closeReceiver, IntentFilter(
                CLOSE_ACTION
            )
        )

        val prevAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_previous_black_24dp,
            PREV_ACTION, prevPendingIntent
        ).build()

        val playPauseIcon: Int = if (exoPlayer?.isPlaying == true) {
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

        val openActivityIntent = Intent(context, MainActivity::class.java)
        val openActivityPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        notificationBuilder
            .setShowWhen(false)
            .setStyle(notificationStyle)
            .setSmallIcon(R.drawable.ic_icon)
            .setLargeIcon(bitmap)
            .setColor(Color.LTGRAY)
            .setContentTitle("$title $version")
            .setContentText(artist)
            .setContentIntent(openActivityPendingIntent)
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
    songId: String?
) {
    if (!serviceRunning) {
        playerServiceIntent =
            Intent(contextReference!!.get()!!, PlayerService::class.java)

        playerServiceIntent?.putExtra("songId", songId)

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
    bitmap: Bitmap
) {
    val notificationManager =
        contextReference?.get()
            ?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
    notificationManager?.notify(
        musicNotificationID,
        createPlayerNotification(
            title,
            version,
            artist,
            bitmap
        )
    )
}

internal fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        contextReference?.get()?.let { context ->
            val channelName = context.getString(R.string.musicNotificationChannelId)
            val channelDescription = context.getString(R.string.musicNotificationDescription)
            val importance = NotificationManager.IMPORTANCE_LOW

            val notificationChannel = NotificationChannel(
                context.getString(R.string.musicNotificationChannelId),
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