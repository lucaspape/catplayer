package de.lucaspape.monstercat.core.music.notification

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
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.util.visiblePlaying
import java.lang.IllegalArgumentException

private const val PLAY_PAUSE_ACTION = "de.lucaspape.monstercat.playpause"
private const val NEXT_ACTION = "de.lucaspape.monstercat.next"
private const val PREV_ACTION = "de.lucaspape.monstercat.prev"
private const val CLOSE_ACTION = "de.lucaspape.monstercat.close"

const val musicNotificationID = 1

private var lastButtonPress: Long = 0

private var playerServiceIntent: Intent? = null

var serviceRunning = false

private var prevReceiver: BroadcastReceiver? = null
private var playPauseReceiver: BroadcastReceiver? = null
private var nextReceiver: BroadcastReceiver? = null
private var closeReceiver: BroadcastReceiver? = null

fun createPlayerNotification(
    context: Context,
    title: String,
    version: String,
    artist: String,
    bitmap: Bitmap?
): Notification {
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

    val playPauseIcon: Int = if (visiblePlaying) {
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

    val openActivityPendingIntent = PendingIntent.getActivity(
        context,
        0,
        openMainActivityIntent,
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

class NotificationIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            if (System.currentTimeMillis() - lastButtonPress > 300) {
                when (intent?.action) {
                    PREV_ACTION -> previous(context)
                    PLAY_PAUSE_ACTION -> toggleMusic(context)
                    NEXT_ACTION -> next(context)
                    CLOSE_ACTION -> stop(context)
                }

                lastButtonPress = System.currentTimeMillis()
            }
        }
    }
}

/**
 * Show notification
 */
fun startPlayerService(
    context: Context,
    songId: String?
) {
    if (!serviceRunning) {
        playerServiceIntent =
            Intent(context, PlayerService::class.java)

        playerServiceIntent?.putExtra("songId", songId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(
                playerServiceIntent
            )
        } else {
            context.startService(
                playerServiceIntent
            )
        }

        serviceRunning = true
    }
}

fun stopPlayerService(context: Context) {
    playerServiceIntent?.let {
        context.stopService(
            playerServiceIntent
        )
    }

    serviceRunning = false
}

fun updateNotification(context: Context, songId: String, bitmap: Bitmap) {
    SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
        updateNotification(context, song.title, song.version, song.artist, bitmap)
    }
}

fun updateNotification(
    context: Context,
    title: String,
    version: String,
    artist: String,
    bitmap: Bitmap
) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
    notificationManager?.notify(
        musicNotificationID,
        createPlayerNotification(
            context,
            title,
            version,
            artist,
            bitmap
        )
    )
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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