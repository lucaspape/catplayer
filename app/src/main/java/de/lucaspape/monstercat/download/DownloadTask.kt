package de.lucaspape.monstercat.download

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.music.MonstercatPlayer
import de.lucaspape.monstercat.util.*
import java.io.File
import java.lang.ref.WeakReference

class DownloadTask(private val weakReference: WeakReference<Context>) :
    AsyncTask<Void, String, String>() {

    override fun doInBackground(vararg p0: Void?): String? {

        val context = weakReference.get()!!

        var downloadedSongs = 0
        var downloadedCoverArrays = 0

        val settings = Settings(weakReference.get()!!)

        while (true) {
            try {
                if (wifiConnected(context) == false && settings.getSetting("downloadOverMobile") != "true") {
                    println("forbidden by user")
                    //TODO add msg
                } else {

                    if (downloadList[downloadedSongs]?.isNotEmpty() == true) {
                        val song = downloadList[downloadedSongs]
                        song?.let {
                            val url = it["url"] as String
                            val location = it["location"] as String

                            if(!File(location).exists()){
                                downloadFile(
                                    location,
                                    url,
                                    context.cacheDir.toString(),
                                    getSid()
                                ) { max, current ->
                                    publishProgress(
                                        song["shownTitle"] as String,
                                        max.toString(),
                                        current.toString(),
                                        false.toString()
                                    )
                                }
                            }

                            downloadList[downloadedSongs] = null
                        }

                    }

                    downloadedSongs++

                }

            } catch (e: IndexOutOfBoundsException) {
            }

            try {
                if (wifiConnected(context) == false && settings.getSetting("downloadCoversOverMobile") != "true") {
                    println("forbidden by user")
                } else {
                    if (downloadCoverArrayListList[downloadedCoverArrays]?.isNotEmpty() == true) {
                        val coverArray = downloadCoverArrayListList[downloadedCoverArrays]

                        coverArray?.let {
                            for (i in it.indices) {
                                val cover = it[i]

                                try {
                                    val url = cover["coverUrl"] as String
                                    val location = cover["coverLocation"] as String

                                    val primaryRes = cover["primaryRes"] as String
                                    val secondaryRes = cover["secondaryRes"] as String

                                    if (!File(location + primaryRes).exists() || !File(location + secondaryRes).exists()) {
                                        downloadCover(url, location, primaryRes, secondaryRes)

                                        publishProgress(
                                            context.getString(R.string.downloadingCoversMsg),
                                            it.size.toString(),
                                            i.toString(),
                                            false.toString()
                                        )
                                    }

                                } catch (e: TypeCastException) {

                                }

                                downloadCoverArrayListList[downloadedCoverArrays] = null
                            }
                        }
                    }

                }

                downloadedCoverArrays++
            } catch (e: IndexOutOfBoundsException) {
            }

            hideDownloadNotification(context)

            Thread.sleep(100)
        }
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