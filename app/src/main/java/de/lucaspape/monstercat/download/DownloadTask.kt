package de.lucaspape.monstercat.download

import android.content.Context
import android.os.AsyncTask
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.target.Target
import de.lucaspape.monstercat.MainActivity
import java.io.*
import java.lang.IndexOutOfBoundsException

class DownloadTask(private val context: Context) : AsyncTask<Void, Void, String>() {
    override fun doInBackground(vararg p0: Void?): String? {

        var downloadedSongs = 0
        var downloadedSongArrays = 0

        while (true) {
            try {
                if (DownloadHandler.downloadList[downloadedSongs].isNotEmpty()) {
                    val song = DownloadHandler.downloadList[downloadedSongs]
                    val url = song["url"] as String
                    val location = song["location"] as String
                    val shownTitle = song["shownTitle"] as String

                    if (MainActivity.loggedIn) {
                        MainActivity.downloadHandler!!.showNotification(shownTitle, 0, 0, true, context)
                        var downloaded = false

                        while (!downloaded) {
                            downloaded = downloadSong(url, location, MainActivity.sid, context)
                        }

                        MainActivity.downloadHandler!!.hideNotification(context)
                    }
                    downloadedSongs++
                }
            } catch (e: IndexOutOfBoundsException) {
            }

            try {
                if (DownloadHandler.downloadArrayListList[downloadedSongArrays].isNotEmpty()) {
                    val songArrayList = DownloadHandler.downloadArrayListList[downloadedSongArrays]

                    if (MainActivity.loggedIn) {
                        MainActivity.downloadHandler!!.showNotification("", 0, songArrayList.size, false, context)

                        for (i in songArrayList.indices) {
                            val song = songArrayList[i]
                            val url = song["downloadUrl"] as String
                            val location = song["downloadLocation"] as String
                            val shownTitle = song["shownTitle"] as String

                            MainActivity.downloadHandler!!.showNotification(
                                shownTitle,
                                i,
                                songArrayList.size,
                                false,
                                context
                            )

                            var downloaded = false

                            while (!downloaded) {
                                downloaded = downloadSong(url, location, MainActivity.sid, context)
                            }
                        }

                        MainActivity.downloadHandler!!.hideNotification(context)

                    }
                    downloadedSongArrays++
                }
            } catch (e: IndexOutOfBoundsException) {

            }

            Thread.sleep(500)
        }
    }

    private fun downloadSong(url: String, location: String, sid: String, context: Context): Boolean {
        try {
            val glideUrl = GlideUrl(
                url, LazyHeaders.Builder()
                    .addHeader("Cookie", "connect.sid=$sid").build()
            )

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
            } catch (e: GlideException) {
                return false
            }

        } catch (e: IOException) {
            return false
        }
    }
}