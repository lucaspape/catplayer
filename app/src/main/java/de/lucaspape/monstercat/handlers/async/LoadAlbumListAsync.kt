package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.getSid
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parseAlbumToDB
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Load album list into database
 */
class LoadAlbumListAsync(
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val loadMax: Int,
    private val displayLoading: () -> Unit,
    private val requestFinished : () -> Unit
) : AsyncTask<Void, Void, String>() {

    override fun onPostExecute(result: String?) {
        requestFinished()
    }

    override fun onPreExecute() {
        contextReference.get()?.let { context ->
            val albumDatabaseHelper =
                AlbumDatabaseHelper(context)
            val albumList = albumDatabaseHelper.getAllAlbums()

            if (!forceReload && albumList.isNotEmpty()) {
                requestFinished()
                cancel(true)
            }else{
                displayLoading()
            }
        }
    }

    override fun doInBackground(vararg param: Void?): String? {
        contextReference.get()?.let { context ->
            val requestQueue = Volley.newRequestQueue(context)

            val tempList = arrayOfNulls<JSONObject>(loadMax)

            val albumDatabaseHelper =
                AlbumDatabaseHelper(context)
            val albumList = albumDatabaseHelper.getAllAlbums()

            if (!forceReload && albumList.isNotEmpty()) {
                return null
            } else {
                val syncObject = Object()

                requestQueue.addRequestFinishedListener<Any> {

                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }

                for (i in (0 until loadMax / 50)) {
                    val requestUrl =
                        context.getString(R.string.loadAlbumsUrl) + "?limit=50&skip=" + i * 50
                    val albumsRequest = AuthorizedRequest(
                        Request.Method.GET, requestUrl, getSid(),
                        Response.Listener { response ->
                            val json = JSONObject(response)
                            val jsonArray = json.getJSONArray("results")

                            for (k in (0 until jsonArray.length())) {
                                val jsonObject = jsonArray.getJSONObject(k)

                                tempList[i * 50 + k] = jsonObject
                            }
                        },
                        Response.ErrorListener { }
                    )

                    requestQueue.add(albumsRequest)

                    synchronized(syncObject) {
                        syncObject.wait()
                    }
                }

                tempList.reverse()

                albumDatabaseHelper.reCreateTable()

                for (jsonObject in tempList) {
                    if (jsonObject != null) {
                        parseAlbumToDB(jsonObject, context)
                    }
                }
            }
        }

        return null
    }

}