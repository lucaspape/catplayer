package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import android.widget.ImageButton
import androidx.core.net.toUri
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.request.async.loadPlaylistAsync
import de.lucaspape.monstercat.ui.abstract_items.content.PlaylistItem
import de.lucaspape.monstercat.ui.offlineDrawable
import de.lucaspape.monstercat.ui.pages.util.deleteDownloadedPlaylistTracks
import de.lucaspape.monstercat.ui.pages.util.downloadPlaylistAsync
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistListRecyclerPage(private val loadPlaylist: (playlistId: String) -> Unit) :
    RecyclerViewPage() {

    override val id = "playlists"

    override suspend fun onItemClick(context: Context, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        super.onItemClick(context, viewData, itemIndex)

        val item = viewData[itemIndex]

        if (item is PlaylistItem) {
            withContext(Dispatchers.Main){
                loadPlaylist(item.playlistId)
            }
        }
    }

    override suspend fun onItemLongClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        super.onItemLongClick(view, viewData, itemIndex)

        val idList = ArrayList<String>()

        for (item in viewData) {
            if (item is PlaylistItem) {
                idList.add(item.playlistId)
            }
        }

        withContext(Dispatchers.Main){
            PlaylistItem.showContextMenu(view, idList, itemIndex)
        }
    }

    override suspend fun onMenuButtonClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        onItemLongClick(view, viewData, itemIndex)
    }

    override suspend fun onDownloadButtonClick(
        context: Context,
        item: GenericItem,
        downloadImageButton: ImageButton
    ) {
        if (item is PlaylistItem) {
            if (item.getDownloadStatus(context) == offlineDrawable) {
                deleteDownloadedPlaylistTracks(
                    context,
                    item.playlistId
                ) {
                    downloadImageButton.setImageURI(
                        item.getDownloadStatus(context).toUri()
                    )
                }
            } else {
                downloadPlaylistAsync(
                    downloadImageButton,
                    item.playlistId
                ) {
                    downloadImageButton.setImageURI(
                        item.getDownloadStatus(context).toUri()
                    )
                }
            }
        }
    }

    override suspend fun idToAbstractItem(view: View, id: String): GenericItem {
        return PlaylistItem(id)
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
            loadPlaylistAsync(context, forceReload, true, displayLoading, { _, _ ->
                val playlistDatabaseHelper =
                    PlaylistDatabaseHelper(context)
                val playlists = playlistDatabaseHelper.getAllPlaylists()

                val idList = ArrayList<String>()

                for (playlist in playlists) {
                    idList.add(playlist.playlistId)
                }

                callback(idList)
            }, { _, _ ->
                errorCallback(context.getString(R.string.errorLoadingPlaylists))
            })
        } else {
            callback(ArrayList())
        }
    }

    override fun getHeader(context: Context): String {
        return context.getString(R.string.yourPlaylists)
    }

    override fun clearDatabase(context: Context) {
        PlaylistDatabaseHelper(context).reCreateTable(context, false)
    }
}