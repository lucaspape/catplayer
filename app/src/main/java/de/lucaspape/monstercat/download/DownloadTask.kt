package de.lucaspape.monstercat.download

import android.content.Context
import android.net.ConnectivityManager
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.getSid
import de.lucaspape.monstercat.music.contextReference
import de.lucaspape.monstercat.settings.Settings
import java.lang.NullPointerException
import java.lang.ref.WeakReference

class DownloadTask(private val weakReference: WeakReference<Context>) :
    AsyncTask<Void, String, String>() {

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
                        try {
                            if (downloadList[downloadedSongs]!!.isNotEmpty()) {
                                val song = downloadList[downloadedSongs]
                                val url = song!!["url"] as String
                                val location = song["location"] as String

                                downloadSong(url, location, sSid, song["shownTitle"] as String) { shownTitle, max, current ->
                                    publishProgress(shownTitle, max.toString(), current.toString(), false.toString())
                                }

                                downloadList[downloadedSongs] = null
                            }

                            downloadedSongs++
                        }catch (e: NullPointerException){

                        }
                    }
                }

            } catch (e: IndexOutOfBoundsException) {
            }

            try {
                if (wifi != null && !wifi.isConnected && settings.getSetting("downloadCoversOverMobile") != "true") {
                    println("forbidden by user")
                } else {
                    try{
                        if (downloadCoverArrayListList[downloadedCoverArrays]!!.isNotEmpty()) {
                            val coverArray = downloadCoverArrayListList[downloadedCoverArrays]

                            for (i in coverArray!!.indices) {
                                publishProgress(context.getString(R.string.downloadingCoversMsg), coverArray.size.toString(), i.toString(), false.toString())

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
                        }
                    }catch (e: NullPointerException){

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
        val title = values[0]!!
        val max = values[1]!!.toInt()
        val current = values[2]!!.toInt()
        val int = values[3]!!.toBoolean()

        showDownloadNotification(title, current, max, int, contextReference!!.get()!!)
    }


}