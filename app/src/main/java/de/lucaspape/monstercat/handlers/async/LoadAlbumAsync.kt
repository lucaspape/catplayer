package de.lucaspape.monstercat.handlers.async

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
    private val requestFinished: () -> Unit
) : AsyncTask<Void, Void, String>() {

    override fun onPostExecute(result: String?) {
        requestFinished()
    }

    override fun onPreExecute() {
        contextReference.get()?.let { context ->
            val albumItemDatabaseHelper =
                AlbumItemDatabaseHelper(context, albumId)
            val albumItems = albumItemDatabaseHelper.getAllData()

            if (!forceReload && albumItems.isNotEmpty()) {
                requestFinished()
                cancel(true)
            } else {
                displayLoading()
                //continue to background task
            }
        }
    }

    override fun doInBackground(vararg param: Void?): String? {
        contextReference.get()?.let { context ->
            val requestQueue = Volley.newRequestQueue(context)

            val albumItemDatabaseHelper =
                AlbumItemDatabaseHelper(context, albumId)

            displayLoading()

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

                }, Response.ErrorListener { }
            )

            requestQueue.add(listRequest)
            synchronized(syncObject) {
                syncObject.wait()
            }

        }

        return null
    }

}