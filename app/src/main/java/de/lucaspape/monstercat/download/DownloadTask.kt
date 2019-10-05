package de.lucaspape.monstercat.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.sid
import de.lucaspape.monstercat.auth.loggedIn
import de.lucaspape.monstercat.settings.Settings
import java.io.*
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL

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
            val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

            try {
                if (wifi != null && !wifi.isConnected && settings.getSetting("downloadOverMobile") != "true") {
                    println("forbidden by user")
                } else {
                    if (downloadList[downloadedSongs]!!.isNotEmpty()) {
                        val song = downloadList[downloadedSongs]
                        val url = song!!["url"] as String
                        val location = song["location"] as String
                        val shownTitle = song["shownTitle"] as String

                        if (loggedIn) {
                            showDownloadNotification(shownTitle, 0, 0, true, context)
                            var downloaded = false

                            while (!downloaded) {
                                downloaded = downloadSong(url, location, sid, context)
                            }

                            downloadList[downloadedSongs] = null
                            hideDownloadNotification(context)
                        }

                    }
                    downloadedSongs++
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

    private fun downloadSong(
        url: String,
        location: String,
        sid: String,
        context: Context
    ): Boolean {
        try {
            val requestQueue = Volley.newRequestQueue(context)

            val params = HashMap<String, String>()
            if (loggedIn) {
                params["Cookie"] = "connect.sid=$sid"
            }

            var noError: Boolean = true
            val syncObject = Object()

            val downloadRequest = DownloadRequest(Request.Method.GET, url,
                Response.Listener { response ->
                    val outputFile = File(location)
                    outputFile.writeBytes(response)

                    noError = true

                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                },
                Response.ErrorListener {
                    noError = false

                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }, params
            )

            requestQueue.add(downloadRequest)

            synchronized(syncObject) {
                syncObject.wait()

                return noError
            }

        } catch (e: IOException) {
            return false
        }
    }

    private fun downloadCover(
        downloadUrl: String,
        location: String,
        primaryRes: String,
        secondaryRes: String
    ): Boolean {
        try {
            if (!File(location + primaryRes).exists() || !File(location + secondaryRes).exists()) {
                val connection =
                    URL("$downloadUrl?image_width=$primaryRes").openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val primaryBitmap = BitmapFactory.decodeStream(input)

                FileOutputStream(location + primaryRes).use { out ->
                    primaryBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val secondaryBitmap =
                    Bitmap.createScaledBitmap(
                        primaryBitmap,
                        secondaryRes.toInt(),
                        secondaryRes.toInt(),
                        false
                    )

                FileOutputStream(location + secondaryRes).use { out ->
                    secondaryBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                connection.disconnect()
            }

            return true
        } catch (e: IOException) {
            // Log exception
            return false
        }
    }
}