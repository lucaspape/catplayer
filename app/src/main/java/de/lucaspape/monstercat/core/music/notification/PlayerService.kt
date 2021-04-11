package de.lucaspape.monstercat.core.music.notification

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.IBinder
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.util.setCover
import java.lang.IllegalArgumentException

val noisyReceiver = NoisyReceiver()

class PlayerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //make sure session is started
        createMediaSession(applicationContext, false)

        //register receiver which checks if headphones unplugged
        applicationContext.registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        createNotificationChannel(applicationContext)

        startForeground(
            musicNotificationID,
            createPlayerNotification(
                applicationContext,
                "",
                "",
                null
            )
        )

        intent?.getStringExtra("songId")?.let { songId ->
            SongDatabaseHelper(this).getSong(songId)?.let { song ->
                setCover(
                    this,
                    song.albumId,
                    song.artistId
                ) {
                    startForeground(
                        musicNotificationID,
                        createPlayerNotification(
                            applicationContext,
                            song.shownTitle,
                            song.artist,
                            it
                        )
                    )
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)

        exoPlayer?.release()
        exoPlayer?.stop()

        try {
            applicationContext.unregisterReceiver(noisyReceiver)
        } catch (e: IllegalArgumentException) {

        }

        serviceRunning = false

        super.onDestroy()
    }
}
