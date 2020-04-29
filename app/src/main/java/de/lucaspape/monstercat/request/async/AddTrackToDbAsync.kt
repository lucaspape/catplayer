package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.database.objects.Song
import de.lucaspape.monstercat.request.newSearchTrackRequest
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parseSongToDB
import java.lang.ref.WeakReference

class AddTrackToDbAsync(
    private val contextReference: WeakReference<Context>,
    private val trackId: String,
    private val finishedCallback: (trackId: String, song: Song) -> Unit,
    private val errorCallback: (trackId: String) -> Unit
) : AsyncTask<Void, Void, Song?>() {

    override fun onPostExecute(result: Song?) {
        if (result != null) {
            finishedCallback(trackId, result)
        } else {
            errorCallback(trackId)
        }
    }

    override fun doInBackground(vararg params: Void?): Song? {
        contextReference.get()?.let { context ->
            val syncObject = Object()

            val volleyQueue =
                newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            volleyQueue.addRequestFinishedListener<Any?> {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            volleyQueue.add(newSearchTrackRequest(context, trackId, 0, true, {
                val jsonArray = it.getJSONArray("results")

                for (i in (0 until jsonArray.length())) {
                    parseSongToDB(jsonArray.getJSONObject(i), context)
                }
            }, {}))

            synchronized(syncObject) {
                syncObject.wait()

                return SongDatabaseHelper(context).getSong(context, trackId)
            }
        }

        return null
    }

}