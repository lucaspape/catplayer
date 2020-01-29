package de.lucaspape.monstercat.music

import android.content.Context
import android.os.AsyncTask
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.twitch.Stream
import java.lang.ref.WeakReference

class StreamInfoUpdateAsync(
    private val contextReference: WeakReference<Context>,
    private val stream: Stream
) : AsyncTask<Void, Void, String>() {
    companion object {
        @JvmStatic
        var liveTitle = ""
        var liveVersion = ""
        var liveArtist = ""
        var liveAlbumId = ""
    }

    override fun doInBackground(vararg params: Void): String? {
        liveTitle = ""
        liveVersion = ""
        liveArtist = ""
        liveAlbumId = ""

        contextReference.get()?.let { context ->
            val volleyQueue = Volley.newRequestQueue(context)

            while (true) {
                stream.updateInfo(context, volleyQueue) { title, version, artist, albumId ->
                    if (liveTitle != title || liveVersion != version || liveArtist != artist || liveAlbumId != albumId) {
                        liveTitle = title
                        liveVersion = version
                        liveArtist = artist
                        liveAlbumId = albumId

                        //update cover
                        publishProgress()
                    }
                }

                Thread.sleep(500)
            }
        }

        return null
    }

    override fun onProgressUpdate(vararg values: Void) {
        contextReference.get()?.let { context ->
            setCover(context, liveTitle, liveVersion, liveArtist, liveAlbumId) { bitmap ->
                updateNotification(liveTitle, liveVersion, liveArtist, bitmap)
            }

            setTitle(liveTitle, liveVersion, liveArtist)
            startTextAnimation()
        }
    }

}