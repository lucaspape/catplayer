package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.getSid
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parsAlbumSongToDB
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadAlbumAsync(
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val itemValue: HashMap<*, *>,
    private val requestFinished : () -> Unit
) : AsyncTask<Void, Void, String>() {

    override fun onPostExecute(result: String?) {
        requestFinished()
    }

    override fun doInBackground(vararg param: Void?): String? {
        val albumId = itemValue["id"] as String
        val mcID = itemValue["mcID"] as String

        contextReference.get()?.let { context ->
            val requestQueue = Volley.newRequestQueue(context)

            val albumItemDatabaseHelper =
                AlbumItemDatabaseHelper(context, albumId)
            val albumItems = albumItemDatabaseHelper.getAllData()

            if (!forceReload && albumItems.isNotEmpty()) {
                return null
            } else {
                val syncObject = Object()

                requestQueue.addRequestFinishedListener<Any> {
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }

                val requestUrl =
                    context.getString(R.string.loadAlbumSongsUrl) + "/" + mcID

                val listRequest = AuthorizedRequest(
                    Request.Method.GET, requestUrl,
                    getSid(), Response.Listener { response ->
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
        }

        return null
    }

}