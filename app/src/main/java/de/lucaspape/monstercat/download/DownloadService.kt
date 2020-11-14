package de.lucaspape.monstercat.download

import android.app.Service
import android.content.Intent
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
        downloadTask?.execute()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        downloadTask?.destroy()
        hideDownloadNotification(this)
    }
}