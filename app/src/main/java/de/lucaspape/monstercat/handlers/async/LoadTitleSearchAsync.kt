package de.lucaspape.monstercat.handlers.async

import android.content.Context
import android.os.AsyncTask
import android.view.View
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.download.addDownloadCoverArray
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.util.parseSongSearchToSongList
import de.lucaspape.monstercat.util.parseSongToHashMap
import org.json.JSONObject
import java.lang.ref.WeakReference

class LoadTitleSearchAsync(
    private val viewReference: WeakReference<View>,
    private val contextReference: WeakReference<Context>,
    private val searchString: String
) : AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg params: Void?): String? {
        contextReference.get()?.let {context ->
            val searchQueue = Volley.newRequestQueue(context)

            val searchRequest = StringRequest(Request.Method.GET,
                context.getString(R.string.loadSongsUrl) + "?term=$searchString&limit=50&skip=0&fields=&search=$searchString",
                Response.Listener { response ->
                    val jsonArray = JSONObject(response).getJSONArray("results")

                    val songList =
                        parseSongSearchToSongList(context, jsonArray)

                    val hashMapList = ArrayList<HashMap<String, Any?>>()

                    for (song in songList) {
                        hashMapList.add(parseSongToHashMap(context, song))
                    }

                    //display list
                    HomeHandler.currentListViewData = hashMapList

                    viewReference.get()?.let {view ->
                        HomeHandler.updateListView(view)
                        HomeHandler.redrawListView(view)

                        //download cover art
                        addDownloadCoverArray(HomeHandler.currentListViewData)

                        HomeHandler.albumContentsDisplayed = false
                    }

                },
                Response.ErrorListener { error ->
                    println(error)
                })

            searchQueue.add(searchRequest)
        }

        return null
    }

}