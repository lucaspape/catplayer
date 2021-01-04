package de.lucaspape.monstercat.ui.handlers.playlist

import android.content.Context
import android.view.View
import android.widget.ImageButton
import androidx.core.net.toUri
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.request.async.loadPlaylistAsync
import de.lucaspape.monstercat.ui.abstract_items.content.PlaylistItem
import de.lucaspape.monstercat.ui.handlers.deleteDownloadedPlaylistTracks
import de.lucaspape.monstercat.ui.handlers.downloadPlaylistAsync
import de.lucaspape.monstercat.ui.offlineDrawable
import de.lucaspape.monstercat.ui.handlers.RecyclerViewHandler

class PlaylistListHandler(private val loadPlaylist: (playlistId: String) -> Unit) :
    RecyclerViewHandler("playlist-list") {

    override fun onItemClick(context: Context, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        val item = viewData[itemIndex]

        if(item is PlaylistItem){
            loadPlaylist(item.playlistId)
        }
    }

    override fun onItemLongClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        val idList = ArrayList<String>()

        for (item in viewData) {
            if(item is PlaylistItem){
                idList.add(item.playlistId)
            }
        }

        PlaylistItem.showContextMenu(view, idList, itemIndex)
    }

    override fun onMenuButtonClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        onItemLongClick(view, viewData, itemIndex)
    }

    override fun onDownloadButtonClick(
        context: Context,
        item: GenericItem,
        downloadImageButton: ImageButton
    ) {
        if(item is PlaylistItem){
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

    override fun idToAbstractItem(view: View, id: String): GenericItem {
        return PlaylistItem(id)
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
                errorCallback()
            })
        }else{
            callback(ArrayList())
        }
    }

    override fun getHeader(context: Context): String {
        return context.getString(R.string.yourPlaylists)
    }
}