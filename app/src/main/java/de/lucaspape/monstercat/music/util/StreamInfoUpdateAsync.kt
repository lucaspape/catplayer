package de.lucaspape.monstercat.music.util

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.notification.updateNotification
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
    }

    override fun doInBackground(vararg params: Void): String? {
        contextReference.get()?.let { context ->

            val artistTitleRequest =
                StringRequest(
                    Request.Method.GET,
                    context.getString(R.string.liveInfoUrl),
                    Response.Listener { artistTitleResponse ->
                        try {
                            val jsonObject = JSONObject(artistTitleResponse).getJSONObject("track")

                            val songId = parseSongToDB(jsonObject, context)

                            if (songId != liveSongId && songId != null) {
                                liveSongId = songId

                                publishProgress()
                            }
                        } catch (e: JSONException) {

                        }

                    },
                    Response.ErrorListener { error ->
                        println(error)
                    })

            val requestQueue = Volley.newRequestQueue(context)

            while (true) {
                requestQueue.add(artistTitleRequest)

                Thread.sleep(500)
            }
        }

        return null
    }

    override fun onProgressUpdate(vararg values: Void) {
        contextReference.get()?.let { context ->
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
        }
    }
}