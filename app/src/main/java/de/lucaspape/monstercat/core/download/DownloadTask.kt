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
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference

class DownloadTask(private val weakReference: WeakReference<Context>) :
    BackgroundService<DownloadStatus>(500) {

    private var failedDownloads = 0

    override fun background(): Boolean {
        weakReference.get()?.let { context ->
            val settings = Settings.getSettings(context)
            val songDatabaseHelper = SongDatabaseHelper(context)

            if (wifiConnected(context) || settings.getBoolean("downloadOverMobile") == true) {
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
                                            DownloadStatus(
                                                "progressUpdate", currentDownloadSong.shownTitle,
                                                max,
                                                current,
                                                false
                                            )
                                        )
                                    }

                                    updateProgress(
                                        DownloadStatus(
                                            "downloadFinished",
                                            downloadedSongs.toString(), 0, 0, false
                                        )
                                    )
                                } else {
                                    updateProgress(
                                        DownloadStatus(
                                            "alreadyDownloadedError",
                                            currentDownloadSong.shownTitle,
                                            0,
                                            0,
                                            false
                                        )
                                    )
                                }
                            } else {
                                updateProgress(
                                    DownloadStatus(
                                        "downloadNotAllowedError",
                                        currentDownloadSong.shownTitle,
                                        0,
                                        0,
                                        false
                                    )
                                )
                            }

                            downloadedSongs++
                        }
                    }


                } catch (e: IndexOutOfBoundsException) {
                    failedDownloads++
                    hideDownloadNotification(context)
                }
            }

            return failedDownloads <= 10
        }

        return false
    }

    override fun publishProgress(value: DownloadStatus) {
            weakReference.get()?.let { context ->
                when (value.type) {
                    "alreadyDownloadedError" -> {
                        println(
                            context.getString(
                                R.string.alreadyDownloadedMsg,
                                value.title
                            )
                        )
                    }
                    "downloadNotAllowedError" -> {
                        displayInfo(
                            context,
                            context.getString(
                                R.string.downloadNotAvailableMsg,
                                value.title
                            )
                        )
                    }
                    "progressUpdate" -> {
                        showDownloadNotification(
                            value.title,
                            value.current,
                            value.max,
                            value.int,
                            context
                        )
                    }
                    "downloadFinished" -> {
                        val listIndex = Integer.parseInt(value.title)

                        downloadList[listIndex].get()?.let { downloadObject ->
                            downloadObject.downloadFinished()
                        }
                    }
                    else -> throw Exception("Unknown type exception")
                }
            }
    }
}

//kinda shitty but better than it was before
class DownloadStatus(
    val type: String,
    val title: String,
    val max: Int,
    val current: Int,
    val int: Boolean
)