package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.request.async.loadPlaylistTracks
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.ui.pages.util.Item
import de.lucaspape.monstercat.ui.pages.util.StringItem

open class PlaylistContentsRecyclerPage(private val playlistId: String) :
    HomeCatalogRecyclerPage() {

    override val id = "playlist-$playlistId"

    override fun showContextMenu(
        view: View,
        data: ArrayList<String>,
        listViewPosition: Int,
    ) {
        CatalogItem.showContextMenuPlaylist(view, data, listViewPosition, playlistId) {
            removeItem(listViewPosition+1) {
                clearDatabase(view.context)
            }
        }
    }

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemList: ArrayList<Item>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        if (skip == 0) {
            loadPlaylistTracks(
                context,
                forceReload,
                playlistId, displayLoading, finishedCallback = {
                    val playlistItemDatabaseHelper =
                        ItemDatabaseHelper(
                            context,
                            playlistId
                        )

                    val playlistItems = playlistItemDatabaseHelper.getAllData(true)

                    val itemList = ArrayList<Item>()

                    for (i in (playlistItems.size - 1 downTo 0)) {
                        itemList.add(StringItem(null, playlistItems[i].songId))
                    }

                    callback(itemList)

                }, errorCallback = {
                    errorCallback(context.getString(R.string.errorLoadingPlaylistTracks))
                })
        } else {
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
        ItemDatabaseHelper(context, playlistId).reCreateTable()
    }
}