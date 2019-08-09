package de.lucaspape.monstercat.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.lucaspape.monstercat.R

/**
 * This class is meant to be static
 */
class DownloadHandler {
    //notification var
    private val channelID = "Download Notification"
    private val notificationID = 1

    companion object {
        @JvmStatic
        val downloadList = ArrayList<HashMap<String, Any?>>()

        @JvmStatic
        val downloadArrayListList = ArrayList<ArrayList<HashMap<String, Any?>>>()

        @JvmStatic
        val downloadCoverList = ArrayList<HashMap<String,Any?>>()

        @JvmStatic
        val downloadCoverArrayListList = ArrayList<ArrayList<HashMap<String,Any?>>>()
    }

    fun addSong(url: String, location: String, shownTitle: String) {
        val downloadTrack = HashMap<String, Any?>()
        downloadTrack["url"] = url
        downloadTrack["location"] = location
        downloadTrack["shownTitle"] = shownTitle

        downloadList.add(downloadTrack)
    }

    fun addSongArray(array: ArrayList<HashMap<String, Any?>>) {
        downloadArrayListList.add(array)
    }

    fun addCover(url: String, location: String, primaryRes:String, secondaryRes:String){
        val cover = HashMap<String, Any?>()
        cover["coverUrl"] = url
        cover["location"] = location
        cover["primaryRes"] = primaryRes
        cover["secondaryRes"] = secondaryRes

        downloadCoverList.add(cover)
    }

    fun addCoverArray(covers: ArrayList<HashMap<String, Any?>>){
        downloadCoverArrayListList.add(covers)
    }

    fun showNotification(shownTitle: String, progress: Int, max: Int, indeterminate: Boolean, context: Context) {
        createNotificationChannel(context)

        val notificationBuilder = NotificationCompat.Builder(context, channelID)
        notificationBuilder.setContentTitle(shownTitle)
        notificationBuilder.setSmallIcon(R.drawable.ic_play_circle_filled_black_24dp)
        notificationBuilder.priority = NotificationCompat.PRIORITY_LOW
        notificationBuilder.setOngoing(true)

        notificationBuilder.setProgress(max, progress, indeterminate)

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.notify(notificationID, notificationBuilder.build())
    }

    fun hideNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationID)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Download Notification"
            val channelDescription = "Handy dandy description"
            val importance = NotificationManager.IMPORTANCE_LOW

            val notificationChannel = NotificationChannel(channelID, channelName, importance)

            notificationChannel.description = channelDescription

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }
}

