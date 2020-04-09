package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.AuthorizedStringRequest
import de.lucaspape.monstercat.util.parsePlaylistToDB
import de.lucaspape.monstercat.util.sid
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Load playlists into database
 */
class LoadManualPlaylist(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val finishedCallback: () -> Unit,
    private val errorCallback: () -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback()
        } else {
            errorCallback()
        }
    }

    override fun doInBackground(vararg param: Void?): Boolean {
        contextReference.get()?.let { context ->
            var success = true
            val syncObject = Object()

            val getManualPlaylistsRequestQueue = Volley.newRequestQueue(context)

            getManualPlaylistsRequestQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            val getManualPlaylistInfoRequest = AuthorizedStringRequest(
                Request.Method.GET,
                context.getString(R.string.playlistUrl) + playlistId, sid,
                Response.Listener { response ->
                    try {
                        val jsonObject = JSONObject(response)

                        parsePlaylistToDB(
                            context,
                            jsonObject,
                            false
                        )
                    } catch (e: JSONException) {
                        success = false
                    }
                },
                Response.ErrorListener {
                    success = false
                }
            )

            getManualPlaylistsRequestQueue.add(getManualPlaylistInfoRequest)

            synchronized(syncObject) {
                syncObject.wait()
                return success
            }
        }

        return false
    }

}