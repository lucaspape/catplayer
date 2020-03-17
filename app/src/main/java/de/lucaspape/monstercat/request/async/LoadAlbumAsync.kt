package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parsAlbumSongToDB
import de.lucaspape.monstercat.util.sid
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Loads an album into database
 */
class LoadAlbumAsync(
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val albumId: String,
    private val mcId: String,
    private val displayLoading: () -> Unit,
    private val finishedCallback: (forceReload:Boolean, albumId:String, mcId:String, displayLoading:() -> Unit) -> Unit,
    private val errorCallback: (forceReload:Boolean, albumId:String, mcId:String, displayLoading:() -> Unit) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if(result){
            finishedCallback(forceReload, albumId, mcId, displayLoading)
        }else{
            errorCallback(forceReload, albumId, mcId, displayLoading)
        }
    }

    override fun onPreExecute() {
        contextReference.get()?.let { context ->
            val albumItemDatabaseHelper =
                AlbumItemDatabaseHelper(context, albumId)
            val albumItems = albumItemDatabaseHelper.getAllData()

            if (!forceReload && albumItems.isNotEmpty()) {
                finishedCallback(forceReload, albumId, mcId, displayLoading)
                cancel(true)
            } else {
                displayLoading()
                //continue to background task
            }
        }
    }

    override fun doInBackground(vararg param: Void?): Boolean {
        contextReference.get()?.let { context ->
            val requestQueue = Volley.newRequestQueue(context)

            val albumItemDatabaseHelper =
                AlbumItemDatabaseHelper(context, albumId)

            displayLoading()

            var success = true
            val syncObject = Object()

            requestQueue.addRequestFinishedListener<Any> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            val requestUrl =
                context.getString(R.string.loadAlbumSongsUrl) + "/" + mcId

            val listRequest = AuthorizedRequest(
                Request.Method.GET, requestUrl,
                sid, Response.Listener { response ->
                    val json = JSONObject(response)
                    val jsonArray = json.getJSONArray("tracks")

                    albumItemDatabaseHelper.reCreateTable()

                    //parse every single song into list
                    for (k in (0 until jsonArray.length())) {
                        parsAlbumSongToDB(
                            jsonArray.getJSONObject(k),
                            albumId,
                            context
                        )
                    }

                }, Response.ErrorListener {
                    success = false
                }
            )

            requestQueue.add(listRequest)
            synchronized(syncObject) {
                syncObject.wait()

                return success
            }

        }

        return false
    }

}