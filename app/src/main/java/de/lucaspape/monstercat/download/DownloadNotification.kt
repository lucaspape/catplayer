package de.lucaspape.monstercat.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.activities.MainActivity

private const val downloadNotificationID = 2

internal fun showDownloadNotification(
    shownTitle: String,
    progress: Int,
    max: Int,
    indeterminate: Boolean,
    context: Context
) {
    createDownloadNotificationChannel(context)

    val openActivityIntent = Intent(context, MainActivity::class.java)
    val openActivityPendingIntent =
        PendingIntent.getActivity(context, 0, openActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT)

    val notificationBuilder = NotificationCompat.Builder(
        context,
        context.getString(R.string.downloadNotificationChannelId)
    )
    notificationBuilder.setContentTitle(shownTitle)
    notificationBuilder.setSmallIcon(R.drawable.ic_file_download_black_24dp)
    notificationBuilder.priority = NotificationCompat.PRIORITY_LOW
    notificationBuilder.setOngoing(true)
    notificationBuilder.setContentIntent(openActivityPendingIntent)

    notificationBuilder.setProgress(max, progress, indeterminate)

    val notificationManagerCompat = NotificationManagerCompat.from(context)
    notificationManagerCompat.notify(downloadNotificationID, notificationBuilder.build())
}

fun hideDownloadNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(downloadNotificationID)
}

private fun createDownloadNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelName = context.getString(R.string.downloadNotificationChannelId)
        val channelDescription = context.getString(R.string.downloadNotificationDescription)
        val importance = NotificationManager.IMPORTANCE_LOW

        val notificationChannel = NotificationChannel(
            context.getString(R.string.downloadNotificationChannelId),
            channelName,
            importance
        )

        notificationChannel.description = channelDescription

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

    }
}