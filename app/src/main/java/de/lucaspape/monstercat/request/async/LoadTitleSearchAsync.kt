package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.newSearchTrackRequest
import de.lucaspape.monstercat.ui.abstract_items.CatalogItem
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parseSongSearchToSongList
import java.lang.ref.WeakReference

/**
 * Load title search into HomeHandler.searchResults
 */
class LoadTitleSearchAsync(
    private val contextReference: WeakReference<Context>,
    private val searchString: String,
    private val skip: Int,
    private val finishedCallback: (searchString: String, skip: Int, searchResults: ArrayList<CatalogItem>) -> Unit,
    private val errorCallback: (searchString: String, skip: Int) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    private var searchResults = ArrayList<CatalogItem>()

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(searchString, skip, searchResults)
        } else {
            errorCallback(searchString, skip)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            var success = true
            val syncObject = Object()
            val searchQueue =
                newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            searchQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            searchQueue.add(newSearchTrackRequest(context, searchString, skip, false, {
                val jsonArray = it.getJSONArray("results")

                val songList =
                    parseSongSearchToSongList(context, jsonArray)

                for (song in songList) {
                    searchResults.add(CatalogItem(song.songId))
                }
            }, {
                success = false
            }))

            synchronized(syncObject) {
                syncObject.wait()

                return success
            }
        }

        return false
    }

}