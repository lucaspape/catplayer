package de.lucaspape.monstercat.music

import android.content.Context
import android.os.AsyncTask
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.twitch.Stream
import java.lang.ref.WeakReference

class UpdateLiveInfoAsync(
    private val contextReference: WeakReference<Context>,
    private val stream: Stream
) : AsyncTask<Void, String, String>() {

    companion object {
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
                stream.albumCoverUpdateUrl
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
                            it.releaseId
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
        val albumId = values[3]

        title?.let {
            version?.let {
                artist?.let {
                    albumId?.let {
                        contextReference.get()?.let { context ->
                            setCover(
                                context,
                                previousTitle,
                                previousVersion,
                                previousArtist,
                                albumId
                            ) { bitmap ->
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

}