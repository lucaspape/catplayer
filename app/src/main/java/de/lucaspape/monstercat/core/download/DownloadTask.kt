package de.lucaspape.monstercat.core.download

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.music.cid
import de.lucaspape.monstercat.core.music.connectSid
import de.lucaspape.monstercat.core.music.displayInfo
import de.lucaspape.monstercat.core.util.downloadFile
import de.lucaspape.monstercat.core.util.wifiConnected
import de.lucaspape.monstercat.core.music.util.BackgroundService
import de.lucaspape.monstercat.core.util.Settings
import java.io.File
import java.lang.Exception
import java.lang.ref.WeakReference

class DownloadTask(private val weakReference: WeakReference<Context>) :
    BackgroundService(500) {

    private var failedDownloads = 0

    override fun background(): Boolean {
        weakReference.get()?.let { context ->
            val settings = Settings.getSettings(context)
            val songDatabaseHelper = SongDatabaseHelper(context)

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
                                        connectSid,
                                        cid
                                    ) { max, current ->
                                        updateProgress(
                                            arrayOf(
                                                "progressUpdate",
                                                currentDownloadSong.shownTitle,
                                                max.toString(),
                                                current.toString(),
                                                false.toString()
                                            )
                                        )
                                    }

                                    updateProgress(
                                        arrayOf(
                                            "downloadFinished",
                                            downloadedSongs.toString()
                                        )
                                    )
                                } else {
                                    updateProgress(
                                        arrayOf(
                                            "alreadyDownloadedError",
                                            currentDownloadSong.shownTitle
                                        )
                                    )
                                }
                            } else {
                                updateProgress(
                                    arrayOf(
                                        "downloadNotAllowedError",
                                        currentDownloadSong.shownTitle
                                    )
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

            return failedDownloads <= 10
        }

        return false
    }

    override fun publishProgress(values: Array<String>?) {
        values?.let {
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
                        val max = values[2].toInt()
                        val current = values[3].toInt()
                        val int = values[4].toBoolean()

                        showDownloadNotification(
                            title,
                            current,
                            max,
                            int,
                            context
                        )
                    }
                    "downloadFinished" -> {
                        values[1].let {
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
}