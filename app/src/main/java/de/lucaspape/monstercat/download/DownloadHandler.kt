package de.lucaspape.monstercat.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.lucaspape.monstercat.R

//notification var

private const val channelID = "Download Notification"
private const val notificationID = 2

val downloadList = ArrayList<HashMap<String, Any?>?>()
val downloadCoverArrayListList = ArrayList<ArrayList<HashMap<String, Any?>>?>()

fun addDownloadSong(url: String, location: String, shownTitle: String) {
    val downloadTrack = HashMap<String, Any?>()
    downloadTrack["url"] = url
    downloadTrack["location"] = location
    downloadTrack["shownTitle"] = shownTitle

    downloadList.add(downloadTrack)
}

fun addDownloadCoverArray(covers: ArrayList<HashMap<String, Any?>>) {
    downloadCoverArrayListList.add(covers)
}

fun showDownloadNotification(
    shownTitle: String,
    progress: Int,
    max: Int,
    indeterminate: Boolean,
    context: Context
) {
    createDownloadNotificationChannel(context)

    val notificationBuilder = NotificationCompat.Builder(context, channelID)
    notificationBuilder.setContentTitle(shownTitle)
    notificationBuilder.setSmallIcon(R.drawable.ic_play_circle_filled_black_24dp)
    notificationBuilder.priority = NotificationCompat.PRIORITY_LOW
    notificationBuilder.setOngoing(true)

    notificationBuilder.setProgress(max, progress, indeterminate)

    val notificationManagerCompat = NotificationManagerCompat.from(context)
    notificationManagerCompat.notify(notificationID, notificationBuilder.build())
}

fun hideDownloadNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(notificationID)
}

private fun createDownloadNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelName = "Download Notification"
        val channelDescription = "Handy dandy description"
        val importance = NotificationManager.IMPORTANCE_LOW

        val notificationChannel = NotificationChannel(channelID, channelName, importance)

        notificationChannel.description = channelDescription

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

    }
}


