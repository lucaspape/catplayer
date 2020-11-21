package de.lucaspape.monstercat.request.async

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.newLoadPlaylistRequest
import de.lucaspape.monstercat.core.util.parsePlaylistToDB
import de.lucaspape.monstercat.request.getAuthorizedRequestQueue
import de.lucaspape.util.BackgroundTask

/**
 * Load playlists into database
 */
class LoadManualPlaylist(
    private val context: Context,
    private val playlistId: String,
    private val finishedCallback: () -> Unit,
    private val errorCallback: () -> Unit
) : BackgroundTask<Boolean>() {
    override suspend fun background() {
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

            updateProgress(true)
        }, {
            updateProgress(false)
        }))
    }

    override suspend fun publishProgress(value: Boolean) {
        if (value) {
            finishedCallback()
        } else {
            errorCallback()
        }
    }
}