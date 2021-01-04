package de.lucaspape.monstercat.ui.handlers.playlist

import android.content.Context
import de.lucaspape.monstercat.core.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.request.async.loadPlaylistTracksAsync
import de.lucaspape.monstercat.ui.handlers.home.HomeCatalogHandler

class PlaylistContentsHandler(private val playlistId: String) : HomeCatalogHandler("playlist-$playlistId") {
    override fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: () -> Unit
    ) {
        if(skip == 0){
            loadPlaylistTracksAsync(
                context,
                forceReload,
                playlistId, displayLoading
                , finishedCallback = { _, _, _ ->
                    val playlistItemDatabaseHelper =
                        PlaylistItemDatabaseHelper(
                            context,
                            playlistId
                        )

                    val playlistItems = playlistItemDatabaseHelper.getAllData()

                    val idList = ArrayList<String>()

                    for (i in (playlistItems.size - 1 downTo 0)) {
                        idList.add(playlistItems[i].songId)
                    }

                    callback(idList)

                }, errorCallback = { _, _, _ ->
                    errorCallback()
                })
        }else{
            callback(ArrayList())
        }
    }

    override fun getHeader(context: Context): String? {
        PlaylistDatabaseHelper(context).getPlaylist(playlistId)?.playlistName?.let {
            return it
        }

        return null
    }
}