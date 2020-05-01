package de.lucaspape.monstercat.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import java.lang.ref.WeakReference

class DownloadService : Service() {

    companion object {
        @JvmStatic
        var downloadTask: DownloadTask? = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        downloadTask = DownloadTask(WeakReference(applicationContext))
        downloadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        downloadTask?.cancel(true)
        hideDownloadNotification(this)
    }
}