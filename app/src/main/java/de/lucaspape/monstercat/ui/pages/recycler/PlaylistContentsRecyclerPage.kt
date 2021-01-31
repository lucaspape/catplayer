package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.request.async.loadPlaylistTracksAsync
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.ui.pages.util.playSongsFromCatalogDbAsync
import de.lucaspape.monstercat.ui.pages.util.playSongsFromViewDataAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class PlaylistContentsRecyclerPage(private val playlistId: String) :
    HomeCatalogRecyclerPage() {

    override val id = "playlist-$playlistId"

    override fun showContextMenu(
        view: View,
        data: ArrayList<String>,
        listViewPosition: Int,
    ) {
        CatalogItem.showContextMenuPlaylist(view, data, listViewPosition, playlistId)
    }

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        if (skip == 0) {
            loadPlaylistTracksAsync(
                context,
                forceReload,
                playlistId, displayLoading, finishedCallback = { _, _, _ ->
                    val playlistItemDatabaseHelper =
                        PlaylistItemDatabaseHelper(
                            context,
                            playlistId
                        )

                    val playlistItems = playlistItemDatabaseHelper.getAllData(true)

                    val idList = ArrayList<String>()

                    for (i in (playlistItems.size - 1 downTo 0)) {
                        idList.add(playlistItems[i].songId)
                    }

                    callback(idList)

                }, errorCallback = { _, _, _ ->
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
        PlaylistItemDatabaseHelper(context, playlistId).reCreateTable()
    }
}