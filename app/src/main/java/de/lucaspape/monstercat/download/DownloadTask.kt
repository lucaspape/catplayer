package de.lucaspape.monstercat.download

import android.content.Context
import android.net.ConnectivityManager
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.getSid
import de.lucaspape.monstercat.auth.loggedIn
import de.lucaspape.monstercat.settings.Settings
import java.lang.ref.WeakReference

class DownloadTask(private val weakReference: WeakReference<Context>) :
    AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg p0: Void?): String? {

        val context = weakReference.get()!!

        var downloadedSongs = 0
        var downloadedCoverArrays = 0

        val connectivityManager =
            weakReference.get()!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val settings = Settings(weakReference.get()!!)

        while (true) {
            //TODO dont use depraced stuff

            val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

            try {
                val sSid = getSid()

                if (sSid != null) {
                    if (wifi != null && !wifi.isConnected && settings.getSetting("downloadOverMobile") != "true") {
                        println("forbidden by user")
                    } else {
                        if (downloadList[downloadedSongs]!!.isNotEmpty()) {
                            val song = downloadList[downloadedSongs]
                            val url = song!!["url"] as String
                            val location = song["location"] as String
                            val shownTitle = song["shownTitle"] as String

                            downloadSong(url, location, shownTitle, sSid, context)

                            downloadList[downloadedSongs] = null


                        }
                        downloadedSongs++
                    }
                }

            } catch (e: IndexOutOfBoundsException) {
            }

            try {
                if (wifi != null && !wifi.isConnected && settings.getSetting("downloadCoversOverMobile") != "true") {
                    println("forbidden by user")
                } else {
                    if (downloadCoverArrayListList[downloadedCoverArrays]!!.isNotEmpty()) {
                        val coverArray = downloadCoverArrayListList[downloadedCoverArrays]

                        for (i in coverArray!!.indices) {
                            showDownloadNotification(
                                context.getString(R.string.downloadingCoversMsg),
                                i,
                                coverArray.size,
                                false,
                                context
                            )

                            val cover = coverArray[i]

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

                        hideDownloadNotification(context)
                    }
                }

                downloadedCoverArrays++
            } catch (e: IndexOutOfBoundsException) {
            }


            Thread.sleep(100)
        }
    }


}