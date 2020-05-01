package de.lucaspape.monstercat.music.util

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.request.newLiveInfoRequest
import de.lucaspape.monstercat.util.getAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parseSongToDB
import org.json.JSONException
import java.lang.ref.WeakReference

class StreamInfoUpdateAsync(
    private val contextReference: WeakReference<Context>
) : AsyncTask<Void, Void, String>() {
    companion object {
        @JvmStatic
        var liveSongId = ""

        @JvmStatic
        var fallbackTitle = ""

        @JvmStatic
        var fallbackArtist = ""

        @JvmStatic
        var fallbackVersion = ""

        @JvmStatic
        var fallbackCoverUrl = ""
    }

    override fun doInBackground(vararg params: Void): String? {
        contextReference.get()?.let { context ->

            val artistTitleRequest = newLiveInfoRequest(context, {
                try {
                    val songId =
                        parseSongToDB(it.getJSONObject("track"), context)

                    if (songId != liveSongId && songId != null) {
                        liveSongId = songId

                        publishProgress()
                    }
                } catch (e: JSONException) {
                    try {
                        fallbackTitle = it.getString("title")
                        fallbackArtist = it.getString("artist")
                        fallbackVersion = it.getString("version")
                        fallbackCoverUrl = it.getString("coverUrl")
                        liveSongId = ""

                        publishProgress()
                    } catch (e: JSONException) {

                    }
                }
            }, {})

            val requestQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            while (true) {
                artistTitleRequest?.let{
                    requestQueue.add(it)
                }

                Thread.sleep(500)
            }
        }

        return null
    }

    override fun onProgressUpdate(vararg values: Void) {
        contextReference.get()?.let { context ->
            if (liveSongId != "") {
                setCover(
                    context,
                    liveSongId
                ) { bitmap ->
                    updateNotification(
                        context,
                        liveSongId,
                        bitmap
                    )
                }

                val songDatabaseHelper = SongDatabaseHelper(context)

                songDatabaseHelper.getSong(context, liveSongId)?.let { song ->
                    title = "${song.title} ${song.version}"
                    artist = song.artist
                }
            } else {
                setCustomCover(
                    context,
                    fallbackTitle + fallbackVersion + fallbackArtist,
                    fallbackCoverUrl
                ) { bitmap ->
                    updateNotification(context, fallbackTitle, fallbackVersion, fallbackArtist, bitmap)
                }

                title = "$fallbackTitle $fallbackVersion"
                artist = fallbackArtist
            }
        }
    }
}