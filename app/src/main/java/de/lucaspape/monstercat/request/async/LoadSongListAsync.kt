package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.CatalogSongDatabaseHelper
import de.lucaspape.monstercat.request.newLoadSongListRequest
import de.lucaspape.monstercat.util.getAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parseCatalogSongToDB
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

            val requestQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            requestQueue.add(newLoadSongListRequest(context, skip, {
                val jsonArray = it.getJSONArray("results")

                for (i in (0 until jsonArray.length())) {
                    parseCatalogSongToDB(
                        jsonArray.getJSONObject(i),
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