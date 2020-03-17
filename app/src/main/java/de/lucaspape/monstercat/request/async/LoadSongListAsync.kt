package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parseCatalogSongToDB
import de.lucaspape.monstercat.util.sid
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Load song list into database
 */
class LoadSongListAsync(
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
            val catalogSongDatabaseHelper =
                CatalogSongDatabaseHelper(context)
            val songIdList = catalogSongDatabaseHelper.getSongs(skip.toLong(), 50)

            if (!forceReload && songIdList.isNotEmpty()) {
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

            requestQueue.addRequestFinishedListener<Any> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            val requestUrl =
                context.getString(R.string.loadSongsUrl) + "?limit=50&skip=" + skip.toString()

            val listRequest = AuthorizedRequest(
                Request.Method.GET, requestUrl, sid,
                Response.Listener { response ->
                    val json = JSONObject(response)
                    val jsonArray = json.getJSONArray("results")

                    for (i in (jsonArray.length() - 1 downTo 0)) {

                        parseCatalogSongToDB(
                            jsonArray.getJSONObject(i),
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