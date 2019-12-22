package de.lucaspape.monstercat.music.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder
import de.lucaspape.monstercat.music.mediaPlayer

class PlayerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        super.onDestroy()
    }
}
