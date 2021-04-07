package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.PublicPlaylistDatabaseHelper
import de.lucaspape.monstercat.request.async.loadPlaylistTracks
import de.lucaspape.monstercat.ui.pages.util.Item

class PublicPlaylistContentsRecyclerPage(private val publicPlaylistId: String) :
    PlaylistContentsRecyclerPage(publicPlaylistId) {

    override val id = "public-playlist-$publicPlaylistId"

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemList: ArrayList<Item>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        loadPlaylistTracks(
            context,
            forceReload,
            publicPlaylistId, displayLoading, finishedCallback = {
                val playlistItemDatabaseHelper =
                    ItemDatabaseHelper(
                        context,
                        publicPlaylistId
                    )

                val playlistItems = playlistItemDatabaseHelper.getAllData(false)

                val itemList = ArrayList<Item>()

                for (i in (playlistItems.size - 1 downTo 0)) {
                    itemList.add(Item(null, playlistItems[i].songId))
                }

                callback(itemList)

            }, errorCallback = {
                errorCallback(context.getString(R.string.errorLoadingPlaylistTracks))
            })
    }

    override fun getHeader(context: Context): String? {
        PublicPlaylistDatabaseHelper(context).getPlaylist(publicPlaylistId)?.playlistName?.let {
            return it
        }

        return null
    }
}