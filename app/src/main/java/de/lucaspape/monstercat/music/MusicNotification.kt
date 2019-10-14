package de.lucaspape.monstercat.music

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.palette.graphics.Palette
import de.lucaspape.monstercat.R
import java.io.File

//notification var
internal const val channelID = "Music Notification"
internal const val notificationID = 1

private const val NOTIFICATION_PREVIOUS = "de.lucaspape.monstercat.previous"
private const val NOTIFICATION_DELETE = "de.lucaspape.monstercat.delete"
private const val NOTIFICATION_PAUSE = "de.lucaspape.monstercat.pause"
private const val NOTIFICATION_PLAY = "de.lucaspape.monstercat.play"
private const val NOTIFICATION_NEXT = "de.lucaspape.monstercat.next"

/**
 * Show notification
 */
internal fun showSongNotification(
    titleName: String,
    artistName: String,
    coverUrl: String,
    playing: Boolean
) {
    val context = contextReference!!.get()!!

    createNotificationChannel()

    val backgroundColor: Int
    val expandedRemoteViews: RemoteViews

    val coverFile = File(coverUrl)
    if (coverFile.exists()) {
        val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)
        backgroundColor = getDominantColor(bitmap)

        if (getTextColor(backgroundColor) == Color.WHITE) {
            expandedRemoteViews = RemoteViews(
                context.packageName,
                R.layout.notification_expanded_white
            )
            expandedRemoteViews.setTextColor(R.id.songname, Color.WHITE)
            expandedRemoteViews.setTextColor(R.id.artistname, Color.WHITE)
        } else {
            expandedRemoteViews = RemoteViews(
                context.packageName,
                R.layout.notification_expanded
            )
            expandedRemoteViews.setTextColor(R.id.songname, Color.BLACK)
            expandedRemoteViews.setTextColor(R.id.artistname, Color.BLACK)
        }

        expandedRemoteViews.setImageViewBitmap(R.id.coverimageview, bitmap)

        expandedRemoteViews.setInt(
            R.id.notificationlayout,
            "setBackgroundColor",
            backgroundColor
        )
    } else {
        expandedRemoteViews = RemoteViews(
            context.packageName,
            R.layout.notification_expanded
        )
    }

    expandedRemoteViews.setTextViewText(R.id.artistname, artistName)
    expandedRemoteViews.setTextViewText(R.id.songname, titleName)

    if (playing) {
        expandedRemoteViews.setViewVisibility(R.id.playButton, View.GONE)
        expandedRemoteViews.setViewVisibility(R.id.pauseButton, View.VISIBLE)
    } else {
        expandedRemoteViews.setViewVisibility(R.id.playButton, View.VISIBLE)
        expandedRemoteViews.setViewVisibility(R.id.pauseButton, View.GONE)
    }

    val notificationBuilder = NotificationCompat.Builder(context, channelID)
    notificationBuilder.setSmallIcon(R.drawable.ic_play_circle_filled_black_24dp)
    notificationBuilder.priority = NotificationCompat.PRIORITY_LOW
    notificationBuilder.setOngoing(true)

    notificationBuilder.setStyle(
        androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(
            mediaSession!!.sessionToken
        )
    )
    notificationBuilder.setCustomBigContentView(expandedRemoteViews)

    setListeners(expandedRemoteViews, context)

    val notificationManagerCompat = NotificationManagerCompat.from(context)
    notificationManagerCompat.notify(notificationID, notificationBuilder.build())

    context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PREVIOUS))
    context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_DELETE))
    context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PAUSE))
    context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_PLAY))
    context.registerReceiver(IntentReceiver(), IntentFilter(NOTIFICATION_NEXT))
}

private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val context = contextReference!!.get()!!

        val channelName = "Music Notification"
        val channelDescription = "Handy dandy description"
        val importance = NotificationManager.IMPORTANCE_LOW

        val notificationChannel = NotificationChannel(channelID, channelName, importance)

        notificationChannel.description = channelDescription

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

    }
}

/**
 * Notification button listeners
 */
private fun setListeners(view: RemoteViews, context: Context) {
    val previous = Intent(NOTIFICATION_PREVIOUS)
    val delete = Intent(NOTIFICATION_DELETE)
    val pause = Intent(NOTIFICATION_PAUSE)
    val play = Intent(NOTIFICATION_PLAY)
    val next = Intent(NOTIFICATION_NEXT)

    val pendingPrevious =
        PendingIntent.getBroadcast(context, 0, previous, PendingIntent.FLAG_CANCEL_CURRENT)
    view.setOnClickPendingIntent(R.id.prevButton, pendingPrevious)

    val pendingDelete =
        PendingIntent.getBroadcast(context, 0, delete, PendingIntent.FLAG_CANCEL_CURRENT)
    view.setOnClickPendingIntent(R.id.closeButton, pendingDelete)

    val pendingPause =
        PendingIntent.getBroadcast(context, 0, pause, PendingIntent.FLAG_CANCEL_CURRENT)
    view.setOnClickPendingIntent(R.id.pauseButton, pendingPause)

    val pendingPlay =
        PendingIntent.getBroadcast(context, 0, play, PendingIntent.FLAG_CANCEL_CURRENT)
    view.setOnClickPendingIntent(R.id.playButton, pendingPlay)

    val pendingNext =
        PendingIntent.getBroadcast(context, 0, next, PendingIntent.FLAG_CANCEL_CURRENT)
    view.setOnClickPendingIntent(R.id.nextButton, pendingNext)
}

class IntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when {
            intent!!.action.equals(NOTIFICATION_PREVIOUS) -> previous()
            intent.action.equals(NOTIFICATION_DELETE) -> {
                val notificationManager =
                    context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                pause()
                notificationManager.cancel(notificationID)
            }
            intent.action.equals(NOTIFICATION_PAUSE) -> pause()
            intent.action.equals(NOTIFICATION_PLAY) -> resume()
            intent.action.equals(NOTIFICATION_NEXT) -> next()
        }
    }
}

/**
 * Get the dominant color of a bitmap image (use for notification background color)
 */
private fun getDominantColor(bitmap: Bitmap): Int {
    val swatchesTemp = Palette.from(bitmap).generate().swatches
    val swatches = ArrayList<Palette.Swatch>(swatchesTemp)

    swatches.sortWith(Comparator { swatch1, swatch2 -> swatch2.population - swatch1.population })

    return if (swatches.size > 0) swatches[0].rgb else Color.WHITE
}

/**
 * Get either BLACK or WHITE, depending on the background color for best readability
 */
private fun getTextColor(background: Int): Int {
    val backgroundRed = Color.red(background)
    val backgroundGreen = Color.green(background)
    val backgroundBlue = Color.blue(background)

    val luma =
        ((0.299 * backgroundRed) + (0.587 * backgroundGreen) + (0.114 * backgroundBlue)) / 255

    return if (luma > 0.5) Color.BLACK else Color.WHITE
}


