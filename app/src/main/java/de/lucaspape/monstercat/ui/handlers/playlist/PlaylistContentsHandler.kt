package de.lucaspape.monstercat.ui.handlers.playlist

import android.content.Context
import android.view.View
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.core.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.request.async.loadPlaylistTracksAsync
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.ui.handlers.home.HomeCatalogHandler

class PlaylistContentsHandler(private val playlistId: String) : HomeCatalogHandler("playlist-$playlistId") {
    override fun onItemLongClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        val idList = ArrayList<String>()

        for (item in viewData) {
            if(item is CatalogItem){
                idList.add(item.songId)
            }
        }

        CatalogItem.showContextMenuPlaylist(view, idList, itemIndex, playlistId)
    }

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

    override fun clearDatabase(context: Context) {
        PlaylistItemDatabaseHelper(context, playlistId).reCreateTable()
    }
}