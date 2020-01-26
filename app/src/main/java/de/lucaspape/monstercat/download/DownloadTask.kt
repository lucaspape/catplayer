package de.lucaspape.monstercat.download

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.MonstercatPlayer
import de.lucaspape.monstercat.util.*
import java.io.File
import java.lang.ref.WeakReference

class DownloadTask(private val weakReference: WeakReference<Context>) :
    AsyncTask<Void, String, String>() {

    override fun doInBackground(vararg p0: Void?): String? {
        weakReference.get()?.let { context ->
            var downloadedSongs = 0
            var streamDownloadedSongs = 0

            val settings = Settings(context)
            val songDatabaseHelper = SongDatabaseHelper(context)

            while (true) {
                if (wifiConnected(context) == false && settings.getSetting("downloadOverMobile") != "true") {
                    println("forbidden by user")
                } else {
                    try {
                        val currentStreamDownloadSong = songDatabaseHelper.getSong(
                            context,
                            streamDownloadList[streamDownloadedSongs]
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
                                            currentStreamDownloadSong.shownTitle,
                                            max.toString(),
                                            current.toString(),
                                            false.toString()
                                        )
                                    }
                                } else {
                                    displayInfo(
                                        context,
                                        context.getString(
                                            R.string.alreadyDownloadedMsg,
                                            currentStreamDownloadSong.shownTitle
                                        )
                                    )
                                }
                            } else {
                                displayInfo(
                                    context,
                                    context.getString(
                                        R.string.downloadNotAvailableMsg,
                                        currentStreamDownloadSong.shownTitle
                                    )
                                )
                            }

                            streamDownloadedSongs++
                        }
                    } catch (e: java.lang.IndexOutOfBoundsException) {

                    }
                }

                if (wifiConnected(context) == false && settings.getSetting("downloadOverMobile") != "true") {
                    println("forbidden by user")
                    //TODO add msg
                } else {
                    try {
                        val currentDownloadSong =
                            songDatabaseHelper.getSong(context, downloadList[downloadedSongs])
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
                                            currentDownloadSong.shownTitle,
                                            max.toString(),
                                            current.toString(),
                                            false.toString()
                                        )
                                    }
                                } else {
                                    displayInfo(
                                        context,
                                        context.getString(
                                            R.string.alreadyDownloadedMsg,
                                            currentDownloadSong.shownTitle
                                        )
                                    )
                                }
                            } else {
                                displayInfo(
                                    context,
                                    context.getString(
                                        R.string.downloadNotAvailableMsg,
                                        currentDownloadSong.shownTitle
                                    )
                                )
                            }

                            downloadedSongs++
                        }
                    } catch (e: java.lang.IndexOutOfBoundsException) {

                    }
                }

                hideDownloadNotification(context)

                Thread.sleep(100)
            }
        }

        return null
    }

    override fun onProgressUpdate(vararg values: String?) {
        val title = values[0]
        val max = values[1]?.toInt()
        val current = values[2]?.toInt()
        val int = values[3]?.toBoolean()

        title?.let {
            max?.let {
                current?.let {
                    int?.let {
                        showDownloadNotification(
                            title,
                            current,
                            max,
                            int,
                            MonstercatPlayer.contextReference!!.get()!!
                        )
                    }
                }
            }
        }
    }
}