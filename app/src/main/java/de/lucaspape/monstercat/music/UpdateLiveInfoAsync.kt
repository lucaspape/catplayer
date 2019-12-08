package de.lucaspape.monstercat.music

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.StrictMode
import de.lucaspape.monstercat.twitch.Stream
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL


class UpdateLiveInfoAsync(private val contextReference: WeakReference<Context>,private val stream: Stream) : AsyncTask<Void, Void, String>(){
    override fun doInBackground(vararg params: Void?): String? {
        var previousTitle = stream.title
        var previousArtist = stream.artist

        contextReference.get()?.let { context ->
            updateCover(context, stream)

            while (true) {
                stream.updateInfo(context) {
                    if (it.title != previousTitle || it.artist != previousArtist) {
                        setTitle(it.title, "", it.artist)

                        startTextAnimation()

                        previousTitle = it.title
                        previousArtist = it.artist

                        updateCover(context, it)

                        setCover(context.filesDir.toString() + "/live.png", it.artist, it.title, context)

                        createSongNotification(
                            stream.title,
                            "",
                            stream.artist,
                            context.filesDir.toString() + "/live.png"
                        )
                    }
                }

                Thread.sleep(1000)
            }
        }

        return null
    }

    private fun updateCover(context: Context, stream: Stream){
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