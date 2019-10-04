package de.lucaspape.monstercat.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.AsyncTask
import android.os.Build
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.target.Target
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.sid
import de.lucaspape.monstercat.auth.loggedIn
import java.io.*
import java.lang.IndexOutOfBoundsException
import java.lang.RuntimeException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URISyntaxException
import java.net.URL

class DownloadTask(private val weakReference: WeakReference<Context>) :
    AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg p0: Void?): String? {

        val context = weakReference.get()!!

        var downloadedSongs = 0
        var downloadedCoverArrays = 0

        while (true) {
            val connectivityManager = weakReference.get()!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

            if(wifi != null){
                if(wifi.isConnected){
                    try {


                        if (downloadList[downloadedSongs].isNotEmpty()) {
                            val song = downloadList[downloadedSongs]
                            val url = song["url"] as String
                            val location = song["location"] as String
                            val shownTitle = song["shownTitle"] as String

                            if (loggedIn) {
                                showDownloadNotification(shownTitle, 0, 0, true, context)
                                var downloaded = false

                                while (!downloaded) {
                                    downloaded = downloadSong(url, location, sid, context)
                                }

                                hideDownloadNotification(context)
                            }

                        }
                        downloadedSongs++
                    } catch (e: IndexOutOfBoundsException) {
                    }

                    try {
                        if (downloadCoverArrayListList[downloadedCoverArrays].isNotEmpty()) {
                            val coverArray = downloadCoverArrayListList[downloadedCoverArrays]

                            for (i in coverArray.indices) {
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

                            }

                            hideDownloadNotification(context)
                        }

                        downloadedCoverArrays++
                    } catch (e: IndexOutOfBoundsException) {
                    }
                }

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
            val glideUrl: GlideUrl

            try {
                glideUrl = GlideUrl(
                    url, LazyHeaders.Builder()
                        .addHeader("Cookie", "connect.sid=$sid").build()
                )
            } catch (e: URISyntaxException) {
                return false
            }

            try {
                try {
                    try {
                        val downloadFile = Glide.with(context)
                            .load(glideUrl)
                            .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get()

                        val destFile = File(location)

                        val bufferedInputStream = BufferedInputStream(FileInputStream(downloadFile))
                        val bufferedOutputStream = BufferedOutputStream(FileOutputStream(destFile))

                        val buffer = ByteArray(1024)

                        var len: Int
                        len = bufferedInputStream.read(buffer)
                        while (len > 0) {
                            bufferedOutputStream.write(buffer, 0, len)
                            len = bufferedInputStream.read(buffer)
                        }
                        bufferedOutputStream.flush()
                        bufferedOutputStream.close()

                        return true
                    } catch (e: RuntimeException) {
                        return false
                    }
                } catch (e: SocketTimeoutException) {
                    return false
                }

            } catch (e: GlideException) {
                return false
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
                val url = URL("$downloadUrl?image_width=$primaryRes")
                val connection = url.openConnection() as HttpURLConnection
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
            }

            return true
        } catch (e: IOException) {
            // Log exception
            return false
        }
    }
}