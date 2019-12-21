package de.lucaspape.monstercat.music

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.lucaspape.monstercat.R

const val PLAY_PAUSE_ACTION = "de.lucaspape.monstercat.playpause"
const val NEXT_ACTION = "de.lucaspape.monstercat.next"
const val PREV_ACTION = "de.lucaspape.monstercat.prev"
const val CLOSE_ACTION = "de.lucaspape.monstercat.close"

//notification var
const val musicChannelID = "Music Notification"
const val musicNotificationID = 1

var lastButtonPress:Long = 0

var notificationServiceIntent:Intent? = null

var serviceRunning = false

fun createPlayerNotification(title:String, version: String, artist:String, coverLocation: String):Notification?{
    contextReference?.get()?.let {context ->
        val notificationBuilder = NotificationCompat.Builder(context, musicChannelID)

        val notificationStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0,1,2)

        val prevIntent = Intent(PREV_ACTION)
        val playPauseIntent = Intent(PLAY_PAUSE_ACTION)
        val nextIntent = Intent(NEXT_ACTION)
        val closeIntent = Intent(CLOSE_ACTION)

        val prevPendingIntent = PendingIntent.getBroadcast(context,0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val playPausePendingIntent = PendingIntent.getBroadcast(context,0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val nextPendingIntent = PendingIntent.getBroadcast(context,0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val closePendingIntent = PendingIntent.getBroadcast(context,0, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        context.registerReceiver(NotificationIntentReceiver(), IntentFilter(PREV_ACTION))
        context.registerReceiver(NotificationIntentReceiver(), IntentFilter(PLAY_PAUSE_ACTION))
        context.registerReceiver(NotificationIntentReceiver(), IntentFilter(NEXT_ACTION))
        context.registerReceiver(NotificationIntentReceiver(), IntentFilter(CLOSE_ACTION))

        val prevAction = NotificationCompat.Action.Builder(R.drawable.ic_skip_previous_black_24dp, PREV_ACTION, prevPendingIntent).build()

        val playPauseIcon:Int

        if(mediaPlayer?.isPlaying == true){
            playPauseIcon = R.drawable.ic_pause_black_24dp
        }else{
            playPauseIcon = R.drawable.ic_play_arrow_black_24dp
        }

        val playPauseAction = NotificationCompat.Action.Builder(playPauseIcon, PLAY_PAUSE_ACTION, playPausePendingIntent).build()
        val nextAction = NotificationCompat.Action.Builder(R.drawable.ic_skip_next_black_24dp, NEXT_ACTION, nextPendingIntent).build()
        val closeAction = NotificationCompat.Action.Builder(R.drawable.ic_close_black_24dp, CLOSE_ACTION, closePendingIntent).build()

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

class NotificationIntentReceiver:BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        if(System.currentTimeMillis() - lastButtonPress > 200){
            when(intent?.action){
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
    if(!serviceRunning){
        notificationServiceIntent =
            Intent(contextReference!!.get()!!, MusicNotificationService::class.java)

        notificationServiceIntent?.putExtra("title", "")
        notificationServiceIntent?.putExtra("version", "")
        notificationServiceIntent?.putExtra("artist", "")
        notificationServiceIntent?.putExtra("coverLocation", "")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            contextReference?.get()?.startForegroundService(notificationServiceIntent)
        } else {
            contextReference?.get()?.startService(notificationServiceIntent)
        }

        serviceRunning = true
    }
}

fun stopPlayerService(){
    notificationServiceIntent?.let {
        contextReference?.get()?.stopService(notificationServiceIntent)
    }

    serviceRunning = false
}

fun updateNotification(title: String, version: String, artist: String, coverLocation: String){
    val notificationManager = contextReference?.get()?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
    notificationManager?.notify(musicNotificationID, createPlayerNotification(title, version, artist, coverLocation))
}

private fun createNotificationChannel() {
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

class MusicNotificationService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent!!.getStringExtra("title")!!
        val version = intent.getStringExtra("version")!!
        val artist = intent.getStringExtra("artist")!!
        val coverLocation = intent.getStringExtra("coverLocation")!!

        createNotificationChannel()

        startForeground(musicNotificationID, createPlayerNotification(title, version, artist, coverLocation))

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)

        super.onDestroy()
    }
}
