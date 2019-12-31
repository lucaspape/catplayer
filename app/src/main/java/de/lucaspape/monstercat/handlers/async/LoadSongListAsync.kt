package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.getSid
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parseCatalogSongToDB
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Load song list into database
 */
class LoadSongListAsync(
    private val contextReference: WeakReference<Context>,
    private val forceReload: Boolean,
    private val loadMax: Int,
    private val requestFinished : () -> Unit
) : AsyncTask<Void, Void, String>() {

    override fun onPostExecute(result: String?) {
        requestFinished()
    }

    override fun doInBackground(vararg param: Void?): String? {
        contextReference.get()?.let { context ->
            val catalogSongDatabaseHelper =
                CatalogSongDatabaseHelper(context)
            val songIdList = catalogSongDatabaseHelper.getAllSongs()

            if (!forceReload && songIdList.isNotEmpty()) {
                return null
            } else {
                val requestQueue = Volley.newRequestQueue(context)

                val sortedList = arrayOfNulls<JSONObject>(loadMax)

                val syncObject = Object()

                requestQueue.addRequestFinishedListener<Any> {
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }

                for (i in (0 until loadMax / 50)) {
                    val requestUrl =
                        context.getString(R.string.loadSongsUrl) + "?limit=50&skip=" + i * 50

                    val listRequest = AuthorizedRequest(
                        Request.Method.GET, requestUrl, getSid(),
                        Response.Listener { response ->
                            val json = JSONObject(response)
                            val jsonArray = json.getJSONArray("results")

                            //parse every single song into list
                            for (k in (0 until jsonArray.length())) {
                                sortedList[i * 50 + k] = jsonArray.getJSONObject(k)
                            }

                        }, Response.ErrorListener { }
                    )

                    requestQueue.add(listRequest)

                    synchronized(syncObject) {
                        syncObject.wait()
                    }
                }

                sortedList.reverse()

                catalogSongDatabaseHelper.reCreateTable()

                for (jsonObject in sortedList) {
                    if (jsonObject != null) {

                        parseCatalogSongToDB(
                            jsonObject,
                            context
                        )
                    }
                }
            }
        }

        return null
    }
}