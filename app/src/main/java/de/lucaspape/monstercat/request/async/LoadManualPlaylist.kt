package de.lucaspape.monstercat.request.async

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.newLoadPlaylistRequest
import de.lucaspape.monstercat.util.getAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parsePlaylistToDB
import de.lucaspape.util.BackgroundTask
import java.lang.ref.WeakReference

/**
 * Load playlists into database
 */
class LoadManualPlaylist(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val finishedCallback: () -> Unit,
    private val errorCallback: () -> Unit
) : BackgroundTask() {
    override fun background() {
        contextReference.get()?.let { context ->
            var success = true
            val syncObject = Object()

            val getManualPlaylistsRequestQueue = getAuthorizedRequestQueue(
                context,
                context.getString(R.string.connectApiHost)
            )

            getManualPlaylistsRequestQueue.add(newLoadPlaylistRequest(context, playlistId, {
                parsePlaylistToDB(
                    context,
                    it,
                    false
                )

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

                updateProgress(arrayOf(success.toString()))
            }
        }
    }

    override fun publishProgress(values: Array<String>?) {
        var success = false

        values?.let {
            success = values[0].toBoolean()
        }

        if (success) {
            finishedCallback()
        } else {
            errorCallback()
        }
    }
}