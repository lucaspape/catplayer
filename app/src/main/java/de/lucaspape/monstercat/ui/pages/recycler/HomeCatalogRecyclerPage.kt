package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import android.widget.ImageButton
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.download.addDownloadSong
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.request.async.loadSongList
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import de.lucaspape.monstercat.ui.pages.util.playSongsFromViewDataAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

open class HomeCatalogRecyclerPage : RecyclerViewPage() {

    override val id = "catalog"

    override suspend fun onItemClick(
        context: Context,
        viewData: ArrayList<GenericItem>,
        itemIndex: Int
    ) {
        super.onItemClick(context, viewData, itemIndex)

        val fistItem = viewData[itemIndex]

        if (fistItem is CatalogItem) {
            val skipMonstercatSongs =
                Settings(context).getBoolean(context.getString(R.string.skipMonstercatSongsSetting)) == true

            withContext(Dispatchers.Main) {
                if (!id.contains("explore")){
                    val catalogViewData = ArrayList<CatalogItem>()

                    for (item in viewData) {
                        if (item is CatalogItem) {
                            catalogViewData.add(item)
                        }
                    }

                    playSongsFromViewDataAsync(
                        context,
                        skipMonstercatSongs,
                        catalogViewData,
                        itemIndex
                    )
                }
            }
        }
    }

    open fun showContextMenu(
        view: View,
        data: ArrayList<String>,
        listViewPosition: Int
    ) {
        CatalogItem.showContextMenu(view, data, listViewPosition)
    }

    override suspend fun onItemLongClick(
        view: View,
        viewData: ArrayList<GenericItem>,
        itemIndex: Int
    ) {
        super.onItemLongClick(view, viewData, itemIndex)

        val idList = ArrayList<String>()

        for (item in viewData) {
            if (item is CatalogItem) {
                idList.add(item.songId)
            }
        }

        if (idList.size > 0) {
            withContext(Dispatchers.Main) {
                showContextMenu(view, idList, itemIndex)
            }
        }
    }

    override suspend fun onMenuButtonClick(
        view: View,
        viewData: ArrayList<GenericItem>,
        itemIndex: Int
    ) {
        onItemLongClick(view, viewData, itemIndex)
    }

    override suspend fun onDownloadButtonClick(
        context: Context,
        item: GenericItem,
        downloadImageButton: ImageButton
    ) {
        if (item is CatalogItem) {
            val songDatabaseHelper = SongDatabaseHelper(context)
            val song = songDatabaseHelper.getSong(context, item.songId)

            withContext(Dispatchers.Main) {
                song?.let {
                    when {
                        song.downloaded -> {
                            File(song.downloadLocation).delete()

                            downloadImageButton.setImageURI(CatalogItem.getSongDownloadStatus(song))
                        }
                        else -> {
                            addDownloadSong(
                                context,
                                item.songId
                            ) {
                                downloadImageButton.setImageURI(
                                    CatalogItem.getSongDownloadStatus(song)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun idToAbstractItem(view: View, id: String): GenericItem {
        return CatalogItem(id)
    }

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        loadSongList(
            context,
            forceReload,
            skip,
            displayLoading,
            finishedCallback = {
                val catalogSongDatabaseHelper = ItemDatabaseHelper(context, "catalog")

                val songList = catalogSongDatabaseHelper.getItems(skip.toLong(), 50)
                val songIdList = ArrayList<String>()

                for (song in songList) {
                    songIdList.add(song.songId)
                }

                callback(songIdList)
            },
            errorCallback = {
                errorCallback(context.getString(R.string.errorLoadingSongList))
            })
    }

    override fun clearDatabase(context: Context) {
        ItemDatabaseHelper(context, "catalog").reCreateTable()
    }
}