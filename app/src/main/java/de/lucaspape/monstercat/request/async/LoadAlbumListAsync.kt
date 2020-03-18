package de.lucaspape.monstercat.request.async

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
    private val finishedCallback: (forceReload: Boolean, skip: Int, displayLoading: () -> Unit) -> Unit,
    private val errorCallback: (forceReload: Boolean, skip: Int, displayLoading: () -> Unit) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(forceReload, skip, displayLoading)
        } else {
            errorCallback(forceReload, skip, displayLoading)
        }
    }

    override fun onPreExecute() {
        contextReference.get()?.let { context ->
            val albumDatabaseHelper =
                AlbumDatabaseHelper(context)
            val albumList = albumDatabaseHelper.getAlbums(skip.toLong(), 50)

            if (!forceReload && albumList.isNotEmpty()) {
                finishedCallback(forceReload, skip, displayLoading)
                cancel(true)
            } else {
                displayLoading()
            }
        }
    }

    override fun doInBackground(vararg param: Void?): Boolean {
        contextReference.get()?.let { context ->
            var success = true
            val syncObject = Object()

            val requestQueue = Volley.newRequestQueue(context)

            requestQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
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

                    for (i in (0 until jsonArray.length())) {
                        parseAlbumToDB(jsonArray.getJSONObject(i), context)
                    }

                },
                Response.ErrorListener {
                    success = false
                }
            )

            requestQueue.add(albumsRequest)

            synchronized(syncObject) {
                syncObject.wait()

                return success
            }

        }

        return false
    }

}