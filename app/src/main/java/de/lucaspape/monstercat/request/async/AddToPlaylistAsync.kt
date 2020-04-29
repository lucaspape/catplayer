package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.request.newAddToPlaylistRequest
import de.lucaspape.monstercat.util.getAuthorizedRequestQueue
import java.lang.ref.WeakReference

class AddToPlaylistAsync(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val songId: String,
    private val finishedCallback: (playlistId: String, songId: String) -> Unit,
    private val errorCallback: (playlistId: String, songId: String) -> Unit
) : AsyncTask<Void, Void, Boolean>() {

    override fun onPostExecute(result: Boolean) {
        if (result) {
            finishedCallback(playlistId, songId)
        } else {
            errorCallback(playlistId, songId)
        }
    }

    override fun doInBackground(vararg params: Void?): Boolean {
        contextReference.get()?.let { context ->
            SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                val addToPlaylistQueue =
                    getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

                var success = true
                val syncObject = Object()

                addToPlaylistQueue.add(
                    newAddToPlaylistRequest(
                        context,
                        playlistId,
                        song,
                        {
                            synchronized(syncObject) {
                                syncObject.notify()
                            }
                        },
                        {
                            success = false
                            synchronized(syncObject) {
                                syncObject.notify()
                            }
                        })
                )

                synchronized(syncObject) {
                    syncObject.wait()

                    return success
                }
            }
        }

        return false
    }

}