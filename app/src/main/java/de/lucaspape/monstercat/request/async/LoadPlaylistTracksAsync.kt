package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.util.newRequestQueue
import de.lucaspape.monstercat.util.parsePlaylistTrackToDB
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

/**
 * Load playlist tracks into database
 */
class LoadPlaylistTracksAsync(
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val playlistId: String,
    private val displayLoading: () -> Unit,
    private val finishedCallback: (forceReload: Boolean, playlistId: String, displayLoading: () -> Unit) -> Unit,
    private val errorCallback: (forceReload: Boolean, playlistId: String, displayLoading: () -> Unit) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(forceReload, playlistId, displayLoading)
        } else {
            errorCallback(forceReload, playlistId, displayLoading)
        }
    }

    override fun onPreExecute() {
        contextReference.get()?.let { context ->
            val playlistItemDatabaseHelper =
                PlaylistItemDatabaseHelper(
                    context,
                    playlistId
                )
            val playlistItems = playlistItemDatabaseHelper.getAllData()

            if (!forceReload && playlistItems.isNotEmpty()) {
                finishedCallback(forceReload, playlistId, displayLoading)
                cancel(true)
            } else {
                displayLoading()
            }
        }
    }

    override fun doInBackground(vararg param: Void?): Boolean {
        contextReference.get()?.let { context ->
            val playlistItemDatabaseHelper =
                PlaylistItemDatabaseHelper(
                    context,
                    playlistId
                )

            val syncObject = Object()

            val jsonObjectList = ArrayList<JSONObject?>()

            var success = true

            var skip = 0
            var nextEmpty = false

            var playlistTrackUrl =
                context.getString(R.string.playlistTrackUrl) + playlistId + "/catalog?skip=" + skip.toString() + "&limit=50"

            var playlistTrackRequest = StringRequest(Request.Method.GET, playlistTrackUrl, Response.Listener { response ->
                val jsonObject = JSONObject(response)
                val jsonArray = jsonObject.getJSONArray("results")

                for(k in (0 until jsonArray.length())){
                    jsonObjectList.add(jsonArray.getJSONObject(k))
                }

                nextEmpty = jsonArray.length() != 50

            }, Response.ErrorListener {
                success = false
            })

            val trackRequestQueue = newRequestQueue(context)

            trackRequestQueue.addRequestFinishedListener<Any?> {
                if(!nextEmpty && success){
                    skip += 50
                    playlistTrackUrl = context.getString(R.string.playlistTrackUrl) + playlistId + "/catalog?skip=" + skip.toString() + "&limit=50"

                    playlistTrackRequest = StringRequest(Request.Method.GET, playlistTrackUrl, Response.Listener { response ->
                        val jsonObject = JSONObject(response)
                        val jsonArray = jsonObject.getJSONArray("results")

                        for(k in (0 until jsonArray.length())){
                            jsonObjectList.add(jsonArray.getJSONObject(k))
                        }

                        nextEmpty = jsonArray.length() != 50

                    }, Response.ErrorListener {
                        success = false
                    })

                    trackRequestQueue.add(playlistTrackRequest)
                }else{
                    synchronized(syncObject){
                        syncObject.notify()
                    }
                }
            }

            trackRequestQueue.add(playlistTrackRequest)

            synchronized(syncObject){
                syncObject.wait()

                if(success){
                    playlistItemDatabaseHelper.reCreateTable()

                    for (playlistObject in jsonObjectList) {
                        if (playlistObject != null) {
                            parsePlaylistTrackToDB(
                                playlistId,
                                playlistObject,
                                context
                            )
                        }

                    }
                }

                return success
            }
        }

        return false
    }

}