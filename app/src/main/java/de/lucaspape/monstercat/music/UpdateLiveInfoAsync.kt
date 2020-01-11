package de.lucaspape.monstercat.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.StrictMode
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.twitch.Stream
import java.io.FileNotFoundException
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

                        publishProgress(
                            it.title,
                            it.version,
                            it.artist,
                            it.albumCoverUpdateUrl
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
        val coverUrl = values[3]

        title?.let {
            version?.let {
                artist?.let {
                    coverUrl?.let {
                        setCover(previousTitle, previousVersion, previousArtist, coverUrl) { bitmap ->
                            updateNotification(
                                title,
                                version,
                                artist,
                                bitmap
                            )
                        }
                    }
                }
            }
        }
    }

}