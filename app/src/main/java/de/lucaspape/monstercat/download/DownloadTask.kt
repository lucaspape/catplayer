package de.lucaspape.monstercat.download

import android.content.Context
import androidx.lifecycle.ViewModel
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.util.*
import de.lucaspape.util.Settings
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception
import java.lang.ref.WeakReference

class DownloadTask(private val weakReference: WeakReference<Context>) :
    ViewModel() {

    private val viewModelJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var lastActive: Long = 0

    val active: Boolean
        get() {
            return System.currentTimeMillis() - lastActive <= 1000
        }

    fun execute() {
        scope.launch {
            withContext(Dispatchers.IO) {
                weakReference.get()?.let { context ->
                    val settings = Settings.getSettings(context)
                    val songDatabaseHelper = SongDatabaseHelper(context)

                    var failedDownloads = 0

                    while (failedDownloads <= 10) {
                        lastActive = System.currentTimeMillis()

                        if (wifiConnected(context) == true || settings.getBoolean("downloadOverMobile") == true) {
                            try {
                                downloadList[downloadedSongs].get()?.let { currentDownloadObject ->
                                    val currentDownloadSong =
                                        songDatabaseHelper.getSong(
                                            context,
                                            currentDownloadObject.songId
                                        )

                                    currentDownloadSong?.let {
                                        if (currentDownloadSong.isDownloadable) {
                                            if (!File(currentDownloadSong.downloadLocation).exists()) {
                                                downloadFile(
                                                    currentDownloadSong.downloadLocation,
                                                    currentDownloadSong.downloadUrl,
                                                    context.cacheDir.toString(),
                                                    getSid(context)
                                                ) { max, current ->
                                                    scope.launch {
                                                        publishProgress(
                                                            "progressUpdate",
                                                            currentDownloadSong.shownTitle,
                                                            max.toString(),
                                                            current.toString(),
                                                            false.toString()
                                                        )
                                                    }
                                                }

                                                publishProgress(
                                                    "downloadFinished",
                                                    downloadedSongs.toString()
                                                )
                                            } else {
                                                publishProgress(
                                                    "alreadyDownloadedError",
                                                    currentDownloadSong.shownTitle
                                                )
                                            }
                                        } else {
                                            publishProgress(
                                                "downloadNotAllowedError",
                                                currentDownloadSong.shownTitle
                                            )
                                        }

                                        downloadedSongs++
                                    }
                                }


                            } catch (e: java.lang.IndexOutOfBoundsException) {
                                failedDownloads++
                                hideDownloadNotification(context)
                            }
                        }

                        delay(10)
                    }
                }
            }
        }
    }

    private suspend fun publishProgress(vararg values: String?) {
        withContext(Dispatchers.Main) {
            val type = values[0]

            weakReference.get()?.let { context ->
                when (type) {
                    "alreadyDownloadedError" -> {
                        val shownTitle = values[1]

                        println(
                            context.getString(
                                R.string.alreadyDownloadedMsg,
                                shownTitle
                            )
                        )
                    }
                    "downloadNotAllowedError" -> {
                        val shownTitle = values[1]

                        displayInfo(
                            context,
                            context.getString(
                                R.string.downloadNotAvailableMsg,
                                shownTitle
                            )
                        )
                    }
                    "progressUpdate" -> {
                        val title = values[1]
                        val max = values[2]?.toInt()
                        val current = values[3]?.toInt()
                        val int = values[4]?.toBoolean()

                        title?.let {
                            max?.let {
                                current?.let {
                                    int?.let {
                                        showDownloadNotification(
                                            title,
                                            current,
                                            max,
                                            int,
                                            context
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "downloadFinished" -> {
                        values[1]?.let {
                            val listIndex = Integer.parseInt(it)

                            downloadList[listIndex].get()?.let { downloadObject ->
                                downloadObject.downloadFinished()
                            }

                        }
                    }
                    else -> throw Exception("Unknown type exception")
                }
            }
        }
    }

    fun destroy() {
        scope.cancel()
    }
}