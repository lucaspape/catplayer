package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parseAlbumToDB
import de.lucaspape.monstercat.util.sid
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Load album list into database
 */
class LoadAlbumListAsync(
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val skip: Int,
    private val displayLoading: () -> Unit,
    private val requestFinished: () -> Unit
) : AsyncTask<Void, Void, String>() {

    override fun onPostExecute(result: String?) {
        requestFinished()
    }

    override fun onPreExecute() {
        contextReference.get()?.let { context ->
            val albumDatabaseHelper =
                AlbumDatabaseHelper(context)
            val albumList = albumDatabaseHelper.getAlbums(skip.toLong(), 1)

            if (!forceReload && albumList.isNotEmpty()) {
                requestFinished()
                cancel(true)
            } else {
                displayLoading()
            }
        }
    }

    override fun doInBackground(vararg param: Void?): String? {
        val syncObject = Object()

        contextReference.get()?.let { context ->
            val requestQueue = Volley.newRequestQueue(context)

            requestQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject){
                    syncObject.notify()
                }
            }

            val requestUrl =
                context.getString(R.string.loadAlbumsUrl) + "?limit=50&skip=" + skip.toString()
            val albumsRequest = AuthorizedRequest(
                Request.Method.GET, requestUrl, sid,
                Response.Listener { response ->
                    val json = JSONObject(response)
                    val jsonArray = json.getJSONArray("results")

                    for(i in (jsonArray.length()-1 downTo 0)){
                        parseAlbumToDB(jsonArray.getJSONObject(i), context)
                    }

                },
                Response.ErrorListener { }
            )

            requestQueue.add(albumsRequest)

        }

        synchronized(syncObject){
            syncObject.wait()
        }

        return null
    }

}