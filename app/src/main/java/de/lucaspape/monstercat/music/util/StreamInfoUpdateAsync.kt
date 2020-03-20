package de.lucaspape.monstercat.music.util

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.music.notification.updateNotification
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

class StreamInfoUpdateAsync(
    private val contextReference: WeakReference<Context>
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
            while (true) {
                updateInfo(context) { title, version, artist, albumId ->
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
            //TODO artistId
            setCover(
                context,
                liveAlbumId,
                ""
            ) { bitmap ->
                updateNotification(
                    liveTitle,
                    liveVersion,
                    liveArtist,
                    bitmap
                )
            }

            title = "$liveTitle $liveVersion"
            artist = liveArtist
        }
    }

    fun updateInfo(
        context: Context,
        callback: (title: String, version: String, artist: String, albumId: String) -> Unit
    ) {
        val volleyQueue = Volley.newRequestQueue(context)

        val artistTitleRequest =
            StringRequest(
                Request.Method.GET,
                context.getString(R.string.liveInfoUrl),
                Response.Listener { artistTitleResponse ->
                    try {
                        val jsonObject = JSONObject(artistTitleResponse)

                        val title = jsonObject.getString("title")
                        val version = jsonObject.getString("version")
                        val artist = jsonObject.getString("artist")
                        val albumId = jsonObject.getString("releaseId")

                        callback(title, version, artist, albumId)
                    } catch (e: JSONException) {

                    }

                },
                Response.ErrorListener { error ->
                    println(error)
                })

        volleyQueue.add(artistTitleRequest)
    }

}