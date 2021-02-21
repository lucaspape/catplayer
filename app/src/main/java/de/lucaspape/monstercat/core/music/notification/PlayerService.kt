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
        createMediaSession(applicationContext)

        //register receiver which checks if headphones unplugged
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        var title = ""
        var artist = ""
        var version = ""

        createNotificationChannel(applicationContext)

        startForeground(
            musicNotificationID,
            createPlayerNotification(
                applicationContext,
                title,
                version,
                artist,
                null
            )
        )


        intent?.getStringExtra("songId")?.let { songId ->
            if(songId != "stream"){
                SongDatabaseHelper(this).getSong(this, songId)?.let { song ->
                    title = song.title
                    artist = song.artist
                    version = song.version

                    setCover(
                        this,
                        song.albumId,
                        song.artistId
                    ) {
                        startForeground(
                            musicNotificationID,
                            createPlayerNotification(
                                applicationContext,
                                song.title,
                                song.version,
                                song.artist,
                                it
                            )
                        )
                    }
                }
            }else{
                title = "Livestream"
                artist = "Monstercat"
                version = ""

                setCover(
                    this,
                    "",
                    ""
                ) {
                    startForeground(
                        musicNotificationID,
                        createPlayerNotification(
                            applicationContext,
                            title,
                            version,
                            artist,
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
            unregisterReceiver(noisyReceiver)
        } catch (e: IllegalArgumentException) {

        }

        serviceRunning = false

        super.onDestroy()
    }
}
