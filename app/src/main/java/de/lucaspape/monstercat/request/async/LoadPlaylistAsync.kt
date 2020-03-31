package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.request.AuthorizedStringRequest
import de.lucaspape.monstercat.util.parsePlaylistToDB
import de.lucaspape.monstercat.util.sid
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Load playlists into database
 */
class LoadPlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val displayLoading: () -> Unit,
    private val finishedCallback: (forceReload:Boolean, displayLoading:() -> Unit) -> Unit,
    private val errorCallback: (forceReload:Boolean, displayLoading:() -> Unit) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if(result){
            finishedCallback(forceReload, displayLoading)
        }else{
            errorCallback(forceReload, displayLoading)
        }
    }

    override fun onPreExecute() {
        contextReference.get()?.let { context ->
            val playlistDatabaseHelper =
                PlaylistDatabaseHelper(context)
            val playlists = playlistDatabaseHelper.getAllPlaylists()

            if (!forceReload && playlists.isNotEmpty()) {
                finishedCallback(forceReload, displayLoading)
                cancel(true)
            } else {
                displayLoading()
            }
        }
    }

    override fun doInBackground(vararg param: Void?): Boolean {
        contextReference.get()?.let { context ->
            val playlistDatabaseHelper =
                PlaylistDatabaseHelper(context)

            val playlistRequestQueue = Volley.newRequestQueue(context)

            val playlistUrl = context.getString(R.string.playlistsUrl)

            var success = true
            val syncObject = Object()

            playlistRequestQueue.addRequestFinishedListener<Any> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            val playlistRequest = AuthorizedStringRequest(
                Request.Method.GET, playlistUrl, sid,
                Response.Listener { response ->
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.getJSONArray("results")

                    playlistDatabaseHelper.reCreateTable(context, false)

                    for (i in (0 until jsonArray.length())) {
                        parsePlaylistToDB(
                            context,
                            jsonArray.getJSONObject(i)
                        )
                    }

                },
                Response.ErrorListener {
                    success = false
                })

            playlistRequestQueue.add(playlistRequest)

            synchronized(syncObject) {
                syncObject.wait()

                return success
            }
        }

        return false
    }

}