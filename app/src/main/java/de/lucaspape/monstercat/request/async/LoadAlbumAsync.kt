package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.request.newLoadAlbumRequest
import de.lucaspape.monstercat.util.getAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parseAlbumSongToDB
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
    private val finishedCallback: (forceReload: Boolean, albumId: String, mcId: String, displayLoading: () -> Unit) -> Unit,
    private val errorCallback: (forceReload: Boolean, albumId: String, mcId: String, displayLoading: () -> Unit) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(forceReload, albumId, mcId, displayLoading)
        } else {
            errorCallback(forceReload, albumId, mcId, displayLoading)
        }
    }

    override fun onPreExecute() {
        contextReference.get()?.let { context ->
            val albumItemDatabaseHelper =
                AlbumItemDatabaseHelper(context, albumId)
            val albumItems = albumItemDatabaseHelper.getAllData()

            if (!forceReload && albumItems.isNotEmpty()) {
                finishedCallback(forceReload, albumId, mcId, displayLoading)
                cancel(true)
            } else {
                displayLoading()
                //continue to background task
            }
        }
    }

    override fun doInBackground(vararg param: Void?): Boolean {
        contextReference.get()?.let { context ->
            val requestQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            val albumItemDatabaseHelper =
                AlbumItemDatabaseHelper(context, albumId)

            displayLoading()

            var success = true
            val syncObject = Object()

            requestQueue.add(newLoadAlbumRequest(context, albumId, {
                val jsonArray = it.getJSONArray("tracks")

                albumItemDatabaseHelper.reCreateTable()

                //parse every single song into list
                for (k in (0 until jsonArray.length())) {
                    parseAlbumSongToDB(
                        jsonArray.getJSONObject(k),
                        albumId,
                        context
                    )
                }

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }, {
                success = false
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }))
            synchronized(syncObject) {
                syncObject.wait()

                return success
            }

        }

        return false
    }

}