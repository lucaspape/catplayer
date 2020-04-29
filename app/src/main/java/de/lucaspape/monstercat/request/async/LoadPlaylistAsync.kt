package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.ManualPlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parsePlaylistToDB
import org.json.JSONException
import org.json.JSONObject
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference

/**
 * Load playlists into database
 */
class LoadPlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val loadManual: Boolean,
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

            val playlistRequestQueue = newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            val playlistUrl = context.getString(R.string.playlistsUrl)

            var success = true
            val syncObject = Object()

            playlistRequestQueue.addRequestFinishedListener<Any> {
                if(loadManual){
                    val manualPlaylists = ManualPlaylistDatabaseHelper(context).getAllPlaylists()

                    //LOAD MANUAL ADDED PLAYLISTS
                    val taskList = ArrayList<LoadManualPlaylist>()
                    var i = 0

                    for(playlist in manualPlaylists){
                        taskList.add(LoadManualPlaylist(contextReference, playlist.playlistId, {
                            try {
                                val task = taskList[i]
                                i++
                                task.executeOnExecutor(THREAD_POOL_EXECUTOR)
                            }catch (e: IndexOutOfBoundsException){
                                synchronized(syncObject) {
                                    syncObject.notify()
                                }
                            }
                        }, {
                            success = false

                            synchronized(syncObject) {
                                syncObject.notify()
                            }
                        }))
                    }

                    try{
                        val task = taskList[i]
                        i++
                        task.executeOnExecutor(THREAD_POOL_EXECUTOR)
                    }catch (e: IndexOutOfBoundsException){
                        synchronized(syncObject) {
                            syncObject.notify()
                        }
                    }
                }else{
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }
            }

            playlistDatabaseHelper.reCreateTable(context, false)

            val playlistRequest = StringRequest(
                Request.Method.GET, playlistUrl,
                Response.Listener { response ->
                    try{
                        val jsonObject = JSONObject(response)
                        val jsonArray = jsonObject.getJSONArray("results")

                        for (i in (0 until jsonArray.length())) {
                            parsePlaylistToDB(
                                context,
                                jsonArray.getJSONObject(i),
                                true
                            )
                        }
                    }catch (e: JSONException){

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