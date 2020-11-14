package de.lucaspape.monstercat.request.async

import android.content.Context
import androidx.lifecycle.ViewModel
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.newLoadPlaylistRequest
import de.lucaspape.monstercat.util.getAuthorizedRequestQueue
import de.lucaspape.monstercat.util.parsePlaylistToDB
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * Load playlists into database
 */
class LoadManualPlaylist(
    private val contextReference: WeakReference<Context>,
    private val playlistId: String,
    private val finishedCallback: () -> Unit,
    private val errorCallback: () -> Unit
) : ViewModel() {

    private val viewModelJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + viewModelJob)

    fun execute() {
        scope.launch {
            withContext(Dispatchers.Default) {
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

                        scope.launch {
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    finishedCallback()
                                } else {
                                    errorCallback()
                                }
                            }

                        }

                    }
                }
            }
        }
    }

}