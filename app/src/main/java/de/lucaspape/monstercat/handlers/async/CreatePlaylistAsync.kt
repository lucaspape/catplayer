package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.displayInfo
import de.lucaspape.monstercat.util.sid
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

class CreatePlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistName: String
) : AsyncTask<Void, Void, String>() {
    override fun doInBackground(vararg params: Void?): String? {
        contextReference.get()?.let { context ->
            val playlistPostUrl = context.getString(R.string.newPlaylistUrl)

            val postObject = JSONObject()

            postObject.put("name", playlistName)
            postObject.put("public", false)
            postObject.put("tracks", JSONArray())

            val newPlaylistVolleyQueue = Volley.newRequestQueue(context)

            val newPlaylistRequest = object : JsonObjectRequest(
                Method.POST, playlistPostUrl, postObject,
                Response.Listener {
                    displayInfo(context, context.getString(R.string.playlistCreatedMsg))
                },
                Response.ErrorListener {
                    displayInfo(context, context.getString(R.string.errorCreatingPlaylist))
                }
            ) {
                override fun getHeaders(): Map<String, String> {
                    return if (sid != null) {
                        val headerParams = HashMap<String, String>()
                        headerParams["Cookie"] = "connect.sid=$sid"
                        headerParams
                    } else {
                        super.getHeaders()
                    }
                }
            }

            newPlaylistVolleyQueue.add(newPlaylistRequest)
        }

        return null
    }

}