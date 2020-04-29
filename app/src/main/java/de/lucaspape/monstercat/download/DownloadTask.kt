package de.lucaspape.monstercat.download

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.util.*
import de.lucaspape.util.Settings
import java.io.File
import java.lang.Exception
import java.lang.ref.WeakReference

class DownloadTask(private val weakReference: WeakReference<Context>) :
    AsyncTask<Void, String, String>() {

    override fun doInBackground(vararg p0: Void?): String? {
        weakReference.get()?.let { context ->
            val settings = Settings.getSettings(context)
            val songDatabaseHelper = SongDatabaseHelper(context)

            var failedDownloads = 0

            while (failedDownloads <= 10) {
                if (wifiConnected(context) == false && settings.getBoolean("downloadOverMobile") != true) {
                    println("forbidden by user")
                } else {
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
                                            publishProgress(
                                                "progressUpdate",
                                                currentDownloadSong.shownTitle,
                                                max.toString(),
                                                current.toString(),
                                                false.toString()
                                            )
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

                Thread.sleep(10)
            }
        }

        return null
    }

    override fun onProgressUpdate(vararg values: String?) {
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