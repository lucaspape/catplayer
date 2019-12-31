package de.lucaspape.monstercat.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.StrictMode
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.twitch.Stream
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL

class UpdateLiveInfoAsync(
    private val contextReference: WeakReference<Context>,
    private val stream: Stream
) : AsyncTask<Void, String, String>() {

    companion object{
        @JvmStatic
        var previousTitle = ""
        @JvmStatic
        var previousArtist = ""
        @JvmStatic
        var previousVersion = ""
    }

    override fun doInBackground(vararg params: Void?): String? {
        previousTitle = stream.title
        previousArtist = stream.artist
        previousVersion = stream.version

        contextReference.get()?.let { context ->
            updateCover(context, stream)

            publishProgress(
                stream.title,
                stream.version,
                stream.artist,
                context.filesDir.toString() + "/live.png"
            )

            val volleyQueue = Volley.newRequestQueue(context)

            while (true) {
                stream.updateInfo(context, volleyQueue) {
                    if (it.title != previousTitle || it.artist != previousArtist || it.version != previousVersion) {
                        setTitle(it.title, it.version, it.artist)

                        startTextAnimation()

                        previousTitle = it.title
                        previousArtist = it.artist
                        previousVersion = it.version

                        updateCover(context, it)

                        publishProgress(
                            it.title,
                            it.version,
                            it.artist,
                            context.filesDir.toString() + "/live.png"
                        )
                    }
                }

                Thread.sleep(1000)
            }
        }

        return null
    }

    override fun onProgressUpdate(vararg values: String?) {
        val title = values[0]
        val version = values[1]
        val artist = values[2]
        val coverLocation = values[3]

        title?.let {
            version?.let {
                artist?.let {
                    coverLocation?.let {
                        setCover(
                            title,
                            version,
                            artist,
                            coverLocation
                        )

                        updateNotification(
                            title,
                            version,
                            artist,
                            coverLocation
                        )
                    }
                }
            }
        }
    }

    private fun updateCover(context: Context, stream: Stream) {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        val connection =
            URL(stream.albumCoverUpdateUrl).openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input = connection.inputStream
        val primaryBitmap = BitmapFactory.decodeStream(input)

        FileOutputStream(context.filesDir.toString() + "/live.png").use { out ->
            primaryBitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

}