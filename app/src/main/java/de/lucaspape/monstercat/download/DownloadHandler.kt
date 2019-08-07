package de.lucaspape.monstercat.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.target.Target
import de.lucaspape.monstercat.MainActivity
import de.lucaspape.monstercat.R
import java.io.*
import java.lang.Exception

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
    }

    fun addSong(url: String, location: String, shownTitle: String) {
        val downloadTrack = HashMap<String, Any?>()
        downloadTrack["url"] = url
        downloadTrack["location"] = location
        downloadTrack["shownTitle"] = shownTitle

        downloadList.add(downloadTrack)
    }

    fun addSongArray(array:ArrayList<HashMap<String, Any?>>){
        downloadArrayListList.add(array)
    }

    fun showNotification(context: Context) {
        createNotificationChannel(context)

        val notificationBuilder = NotificationCompat.Builder(context, channelID)
        notificationBuilder.setContentTitle("Downloading something idk dont ask me")
        notificationBuilder.setSmallIcon(R.drawable.ic_play_circle_filled_black_24dp)
        notificationBuilder.priority = NotificationCompat.PRIORITY_LOW
        notificationBuilder.setOngoing(true)

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

