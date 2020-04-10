package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.sid
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

class RenamePlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val playlistName: String,
    private val finishedCallback: (playlistName: String, playlistId:String) -> Unit,
    private val errorCallback: (playlistName: String, playlistId:String) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(playlistName, playlistId)
        } else {
            errorCallback(playlistName, playlistId)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            val playlistPatchUrl = context.getString(R.string.playlistUrl) + playlistId

            val postObject = JSONObject()

            postObject.put("name", playlistName)

            val newPlaylistVolleyQueue = Volley.newRequestQueue(context)

            var success = true
            val syncObject = Object()

            newPlaylistVolleyQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            val newPlaylistRequest = object : JsonObjectRequest(
                Method.PATCH, playlistPatchUrl, postObject,
                Response.Listener {
                },
                Response.ErrorListener {
                    success = false
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

            synchronized(syncObject) {
                syncObject.wait()

                return success
            }
        }

        return false
    }

}