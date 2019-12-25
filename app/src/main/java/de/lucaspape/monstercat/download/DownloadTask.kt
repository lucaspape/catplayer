package de.lucaspape.monstercat.download

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.music.contextReference
import de.lucaspape.monstercat.util.*
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
                val sSid = getSid()

                if (sSid != null) {
                    if (wifiConnected(context) == false && settings.getSetting("downloadOverMobile") != "true") {
                        println("forbidden by user")
                        //TODO add msg
                    } else {

                        if (downloadList[downloadedSongs]?.isNotEmpty() == true) {
                            val song = downloadList[downloadedSongs]
                            song?.let {
                                val url = it["url"] as String
                                val location = it["location"] as String

                                downloadFile(location, url, context.cacheDir.toString(), sSid) { max, current ->
                                    publishProgress(
                                        song["shownTitle"] as String,
                                        max.toString(),
                                        current.toString(),
                                        false.toString()
                                    )
                                }

                                downloadList[downloadedSongs] = null
                            }

                        }

                        downloadedSongs++
                    }
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
                                publishProgress(
                                    context.getString(R.string.downloadingCoversMsg),
                                    it.size.toString(),
                                    i.toString(),
                                    false.toString()
                                )

                                val cover = it[i]

                                try {
                                    val url = cover["coverUrl"] as String
                                    val location = cover["coverLocation"] as String

                                    val primaryRes = cover["primaryRes"] as String
                                    val secondaryRes = cover["secondaryRes"] as String

                                    downloadCover(url, location, primaryRes, secondaryRes)
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
                            contextReference!!.get()!!
                        )
                    }
                }
            }
        }
    }
}