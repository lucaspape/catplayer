package de.lucaspape.monstercat.music.util

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parseSongToDB
import org.json.JSONException
import org.json.JSONObject
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

            val artistTitleRequest =
                StringRequest(
                    Request.Method.GET,
                    context.getString(R.string.customApiBaseUrl) + "liveinfo",
                    Response.Listener { artistTitleResponse ->
                        try {
                            val jsonObject = JSONObject(artistTitleResponse)

                            try {
                                val songId =
                                    parseSongToDB(jsonObject.getJSONObject("track"), context)

                                if (songId != liveSongId && songId != null) {
                                    liveSongId = songId

                                    publishProgress()
                                }
                            } catch (e: JSONException) {
                                try {
                                    fallbackTitle = jsonObject.getString("title")
                                    fallbackArtist = jsonObject.getString("artist")
                                    fallbackVersion = jsonObject.getString("version")
                                    fallbackCoverUrl = jsonObject.getString("coverUrl")
                                    liveSongId = ""

                                    publishProgress()
                                } catch (e: JSONException) {

                                }
                            }
                        } catch (e: JSONException) {

                        }

                    },
                    Response.ErrorListener { error ->
                        println(error)
                    })

            val requestQueue = newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            while (true) {
                requestQueue.add(artistTitleRequest)

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
                    updateNotification(fallbackTitle, fallbackVersion, fallbackArtist, bitmap)
                }

                title = "$fallbackTitle $fallbackVersion"
                artist = fallbackArtist
            }
        }
    }
}