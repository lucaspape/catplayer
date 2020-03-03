package de.lucaspape.monstercat.background

import android.app.Service
import android.content.Intent
import android.os.IBinder
import de.lucaspape.monstercat.download.DownloadTask
import de.lucaspape.monstercat.download.hideDownloadNotification
import de.lucaspape.monstercat.handlers.async.BackgroundAsync
import de.lucaspape.monstercat.music.StreamInfoUpdateAsync

class BackgroundService : Service() {
    companion object {
        @JvmStatic
        var loadContinuousSongListAsyncTask: BackgroundAsync? = null

        @JvmStatic
        var downloadTask: DownloadTask? = null

        @JvmStatic
        var streamInfoUpdateAsync: StreamInfoUpdateAsync? = null

        @JvmStatic
        var serviceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceRunning = true

        return START_STICKY
    }

    override fun onDestroy() {
        loadContinuousSongListAsyncTask?.cancel(true)
        downloadTask?.cancel(true)
        streamInfoUpdateAsync?.cancel(true)

        hideDownloadNotification(this)

        serviceRunning = false

        super.onDestroy()
    }
}