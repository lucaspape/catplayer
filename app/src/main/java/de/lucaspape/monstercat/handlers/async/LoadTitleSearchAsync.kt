package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.request.AuthorizedRequest
import de.lucaspape.monstercat.util.parseSongSearchToSongList
import de.lucaspape.monstercat.util.parseSongToHashMap
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
    private val requestFinished : () -> Unit
) : AsyncTask<Void, Void, String>() {

    override fun onPostExecute(result: String?) {
        requestFinished()
    }

    override fun doInBackground(vararg params: Void?): String? {
        val syncObject = Object()

        contextReference.get()?.let {context ->
            val searchQueue = Volley.newRequestQueue(context)

            searchQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject){
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

                    val hashMapList = ArrayList<HashMap<String, Any?>>()

                    for (song in songList) {
                        hashMapList.add(parseSongToHashMap(song))
                    }

                    //display list
                    HomeHandler.searchResults = hashMapList

                },
                Response.ErrorListener { error ->
                    println(error)
                })

            searchQueue.add(searchRequest)
        }

        synchronized(syncObject){
            syncObject.wait()
        }

        return null
    }

}