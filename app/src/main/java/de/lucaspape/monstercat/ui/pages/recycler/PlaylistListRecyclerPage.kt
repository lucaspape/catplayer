package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import android.widget.ImageButton
import androidx.core.net.toUri
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.request.async.loadPlaylists
import de.lucaspape.monstercat.ui.abstract_items.content.PlaylistItem
import de.lucaspape.monstercat.ui.offlineDrawable
import de.lucaspape.monstercat.ui.pages.util.Item
import de.lucaspape.monstercat.ui.pages.util.deleteDownloadedPlaylistTracks
import de.lucaspape.monstercat.ui.pages.util.downloadPlaylistAsync
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistListRecyclerPage(private val loadPlaylist: (playlistId: String) -> Unit) :
    RecyclerViewPage() {

    override val id = "playlists"

    private var currentPlaylistId = ""

    override suspend fun onItemClick(context: Context, viewData: List<GenericItem>, itemIndex: Int) {
        super.onItemClick(context, viewData, itemIndex)

        val item = viewData[itemIndex]

        if (item is PlaylistItem) {
            withContext(Dispatchers.Main){
                currentPlaylistId = item.playlistId

                saveData()

                loadPlaylist(item.playlistId)
            }
        }
    }

    override suspend fun onItemLongClick(view: View, viewData: List<GenericItem>, itemIndex: Int) {
        super.onItemLongClick(view, viewData, itemIndex)

        val idList = ArrayList<String>()

        for (item in viewData) {
            if (item is PlaylistItem) {
                idList.add(item.playlistId)
            }
        }

        withContext(Dispatchers.Main){
            PlaylistItem.showContextMenu(view, idList, itemIndex) {
                //idk about the +1 but it works so I really dont care
                removeItem(itemIndex+1)

                clearDatabase(view.context)
            }
        }
    }

    override suspend fun onMenuButtonClick(view: View, viewData: List<GenericItem>, itemIndex: Int) {
        onItemLongClick(view, viewData, itemIndex)
    }

    override suspend fun onDownloadButtonClick(
        context: Context,
        item: GenericItem,
        downloadImageButton: ImageButton
    ) {
        if (item is PlaylistItem) {
            withContext(Dispatchers.Main){
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
    }

    override suspend fun itemToAbstractItem(view: View, item: Item): GenericItem {
        return PlaylistItem(item.itemId)
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
            loadPlaylists(context, forceReload, true, displayLoading, {
                val playlistDatabaseHelper =
                    PlaylistDatabaseHelper(context)
                val playlists = playlistDatabaseHelper.getAllPlaylists()

                val itemList = ArrayList<Item>()

                for (playlist in playlists) {
                    itemList.add(Item(playlist.playlistId, null))
                }

                callback(itemList)
            }, {
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

    override fun restore(data: HashMap<String, String>?): Boolean {
        return if (data != null) {
            val playlistId = data["playlistId"]

            if (!playlistId.isNullOrBlank()) {
                loadPlaylist(playlistId)
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    override fun save(): HashMap<String, String> {
        val hashMap = HashMap<String, String>()

        hashMap["playlistId"] = currentPlaylistId

        return hashMap
    }
}