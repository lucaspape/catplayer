package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.request.newDeletePlaylistTrackRequest
import de.lucaspape.monstercat.util.newAuthorizedRequestQueue
import java.lang.ref.WeakReference

class DeletePlaylistTrackAsync(
    private val contextReference: WeakReference<Context>,
    private val songId: String,
    private val playlistId: String,
    private val songDeleteIndex: Int,
    private val finishedCallback: (songId: String, playlistId: String, songDeleteIndex: Int) -> Unit,
    private val errorCallback: (songId: String, playlistId: String, songDeleteIndex: Int) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(songId, playlistId, songDeleteIndex)
        } else {
            errorCallback(songId, playlistId, songDeleteIndex)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                val deleteSongVolleyQueue = newAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

                var success = true
                val syncObject = Object()

                deleteSongVolleyQueue.addRequestFinishedListener<Any?> {
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }

                deleteSongVolleyQueue.add(newDeletePlaylistTrackRequest(context, playlistId, song, songDeleteIndex, {}, {
                    success = false
                }))

                synchronized(syncObject) {
                    syncObject.wait()

                    return success
                }
            }
        }

        return false
    }
}