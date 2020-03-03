package de.lucaspape.monstercat.music.notification

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.IBinder
import de.lucaspape.monstercat.activities.noisyReceiver
import de.lucaspape.monstercat.background.BackgroundService
import de.lucaspape.monstercat.music.MusicPlayer
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

        createNotificationChannel()

        startForeground(
            musicNotificationID,
            createPlayerNotification(
                title,
                version,
                artist,
                null
            )
        )

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)

        MusicPlayer.exoPlayer?.release()
        MusicPlayer.exoPlayer?.stop()
        BackgroundService.streamInfoUpdateAsync?.cancel(true)

        try {
            unregisterReceiver(noisyReceiver)
        } catch (e: IllegalArgumentException) {

        }

        super.onDestroy()
    }
}
