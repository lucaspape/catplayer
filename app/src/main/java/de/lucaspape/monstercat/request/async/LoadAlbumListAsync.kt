package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.request.newLoadAlbumListRequest
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parseAlbumToDB
import java.lang.ref.WeakReference

/**
 * Load album list into database
 */
class LoadAlbumListAsync(
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
            val albumDatabaseHelper =
                AlbumDatabaseHelper(context)
            val albumList = albumDatabaseHelper.getAlbums(skip.toLong(), 50)

            if (!forceReload && albumList.isNotEmpty()) {
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

            val requestQueue = newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            requestQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            requestQueue.add(newLoadAlbumListRequest(context, skip, {
                val jsonArray = it.getJSONArray("results")

                for (i in (0 until jsonArray.length())) {
                    parseAlbumToDB(jsonArray.getJSONObject(i), context)
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