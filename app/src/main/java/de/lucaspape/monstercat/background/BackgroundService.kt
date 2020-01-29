package de.lucaspape.monstercat.background

import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import de.lucaspape.monstercat.download.DownloadTask
import de.lucaspape.monstercat.download.hideDownloadNotification
import de.lucaspape.monstercat.handlers.async.BackgroundAsync
import de.lucaspape.monstercat.music.StreamInfoUpdateAsync
import java.lang.ref.WeakReference

class BackgroundService : Service() {
    companion object {
        @JvmStatic
        var loadContinuousSongListAsyncTask: BackgroundAsync? = null
        @JvmStatic
        var downloadTask: DownloadTask? = null
        @JvmStatic
        var waitForDownloadTask: AsyncTask<Void, Void, String>? = null
        @JvmStatic
        var streamInfoUpdateAsync: StreamInfoUpdateAsync? = null
        @JvmStatic
        var serviceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        downloadTask = DownloadTask(WeakReference(applicationContext))
        downloadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        serviceRunning = true

        return START_STICKY
    }

    override fun onDestroy() {
        loadContinuousSongListAsyncTask?.cancel(true)
        downloadTask?.cancel(true)
        waitForDownloadTask?.cancel(true)
        streamInfoUpdateAsync?.cancel(true)

        hideDownloadNotification(this)

        serviceRunning = false

        super.onDestroy()
    }
}