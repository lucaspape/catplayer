package de.lucaspape.monstercat.download

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.MusicPlayer
import de.lucaspape.monstercat.util.*
import java.io.File
import java.lang.Exception
import java.lang.ref.WeakReference

class DownloadTask(private val weakReference: WeakReference<Context>) :
    AsyncTask<Void, String, String>() {

    override fun doInBackground(vararg p0: Void?): String? {
        weakReference.get()?.let { context ->
            val settings = Settings(context)
            val songDatabaseHelper = SongDatabaseHelper(context)

            var failedDownloads = 0

            while (failedDownloads <= 10) {
                if (wifiConnected(context) == false && settings.getSetting("downloadOverMobile") != "true") {
                    println("forbidden by user")
                } else {
                    try {
                        streamDownloadList[streamDownloadedSongs].get()
                            ?.let { currentDownloadObject ->
                                val currentStreamDownloadSong = songDatabaseHelper.getSong(
                                    context,
                                    currentDownloadObject.songId
                                )

                                currentStreamDownloadSong?.let {
                                    if (currentStreamDownloadSong.isDownloadable) {
                                        if (!File(currentStreamDownloadSong.streamDownloadLocation).exists()) {
                                            downloadFile(
                                                currentStreamDownloadSong.streamDownloadLocation,
                                                currentStreamDownloadSong.streamUrl,
                                                context.cacheDir.toString(),
                                                sid
                                            ) { max, current ->
                                                publishProgress(
                                                    "progressUpdate",
                                                    currentStreamDownloadSong.shownTitle,
                                                    max.toString(),
                                                    current.toString(),
                                                    false.toString()
                                                )
                                            }

                                            publishProgress(
                                                "streamDownloadFinished",
                                                streamDownloadedSongs.toString()
                                            )
                                        } else {
                                            publishProgress(
                                                "alreadyDownloadedError",
                                                currentStreamDownloadSong.shownTitle
                                            )
                                        }
                                    } else {
                                        publishProgress(
                                            "downloadNotAllowedError",
                                            currentStreamDownloadSong.shownTitle
                                        )
                                    }

                                    streamDownloadedSongs++
                                }
                            }


                    } catch (e: java.lang.IndexOutOfBoundsException) {
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
                                                sid
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
                }

                Thread.sleep(10)
            }
        }

        return null
    }

    override fun onProgressUpdate(vararg values: String?) {
        val type = values[0]

        MusicPlayer.contextReference?.get()?.let { context ->
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
                "streamDownloadFinished" -> {
                    values[1]?.let {
                        val listIndex = Integer.parseInt(it)

                        streamDownloadList[listIndex].get()?.let { downloadObject ->
                            downloadObject.downloadFinished()
                        }

                    }
                }
                else -> throw Exception("Unknown type exception")
            }
        }
    }
}