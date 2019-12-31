package de.lucaspape.monstercat.music.notification

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.IBinder
import de.lucaspape.monstercat.background.BackgroundService.Companion.updateLiveInfoAsync
import de.lucaspape.monstercat.activities.noisyReceiver
import de.lucaspape.monstercat.music.mediaPlayer
import java.lang.IllegalArgumentException

class PlayerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //register receiver which checks if headphones unplugged
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        val title = intent!!.getStringExtra("title")!!
        val version = intent.getStringExtra("version")!!
        val artist = intent.getStringExtra("artist")!!
        val coverLocation = intent.getStringExtra("coverLocation")!!

        createNotificationChannel()

        startForeground(
            musicNotificationID,
            createPlayerNotification(
                title,
                version,
                artist,
                coverLocation
            )
        )

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)

        mediaPlayer?.release()
        mediaPlayer?.stop()

        try {
            unregisterReceiver(noisyReceiver)
        } catch (e: IllegalArgumentException) {

        }

        updateLiveInfoAsync?.cancel(true)

        super.onDestroy()
    }
}
