package de.lucaspape.monstercat.music.notification

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.IBinder
import de.lucaspape.monstercat.activities.noisyReceiver
import de.lucaspape.monstercat.background.BackgroundService
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.contextReference
import de.lucaspape.monstercat.music.exoPlayer
import de.lucaspape.monstercat.music.setCover
import java.lang.IllegalArgumentException

class PlayerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //register receiver which checks if headphones unplugged
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        val songId = intent!!.getStringExtra("songId")!!

        var title = ""
        var artist = ""
        var version = ""

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

        contextReference?.get()?.let { context ->
            SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                title = song.title
                artist = song.artist
                version = song.version

                setCover(context, song.title, song.version, song.artist, song.albumId) {
                    startForeground(
                        musicNotificationID,
                        createPlayerNotification(
                            song.title,
                            song.version,
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
        BackgroundService.streamInfoUpdateAsync?.cancel(true)

        try {
            unregisterReceiver(noisyReceiver)
        } catch (e: IllegalArgumentException) {

        }

        super.onDestroy()
    }
}
