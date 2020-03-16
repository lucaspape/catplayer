package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.handlers.abstract_items.CatalogItem
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parseSongSearchToSongList
import de.lucaspape.monstercat.util.sid
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Load title search into HomeHandler.searchResults
 */
class LoadTitleSearchAsync(
    private val contextReference: WeakReference<Context>,
    private val searchString: String,
    private val skip: Int,
    private val requestFinished: (searchResults: ArrayList<CatalogItem>) -> Unit
) : AsyncTask<Void, Void, String>() {

    var searchResults = ArrayList<CatalogItem>()

    override fun onPostExecute(result: String?) {
        requestFinished(searchResults)
    }

    override fun doInBackground(vararg params: Void?): String? {
        val syncObject = Object()

        contextReference.get()?.let { context ->
            val searchQueue = Volley.newRequestQueue(context)

            searchQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            val searchRequest = AuthorizedRequest(Request.Method.GET,
                context.getString(R.string.loadSongsUrl) + "?term=$searchString&limit=50&skip=" + skip.toString() + "&fields=&search=$searchString",
                sid,
                Response.Listener { response ->
                    val jsonArray = JSONObject(response).getJSONArray("results")

                    val songList =
                        parseSongSearchToSongList(context, jsonArray)

                    for (song in songList) {
                        searchResults.add(CatalogItem(song.songId))
                    }

                },
                Response.ErrorListener { error ->
                    println(error)
                })

            searchQueue.add(searchRequest)
        }

        synchronized(syncObject) {
            syncObject.wait()
        }

        return null
    }

}