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
import de.lucaspape.monstercat.core.music.filter
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.request.async.loadSongList
import de.lucaspape.monstercat.ui.pages.util.Item
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import de.lucaspape.monstercat.ui.pages.util.playSongsFromViewDataAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

open class HomeCatalogRecyclerPage : RecyclerViewPage() {

    override val id = "catalog"

    override suspend fun onItemClick(
        context: Context,
        viewData: List<GenericItem>,
        itemIndex: Int
    ) {
        super.onItemClick(context, viewData, itemIndex)

        val fistItem = viewData[itemIndex]

        if (fistItem is CatalogItem) {
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
        viewData: List<GenericItem>,
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
        viewData: List<GenericItem>,
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
            val song = songDatabaseHelper.getSong(item.songId)

            withContext(Dispatchers.Main) {
                song?.let {
                    when {
                        song.downloaded(context) -> {
                            song.deleteDownload(context)

                            downloadImageButton.setImageURI(CatalogItem.getSongDownloadStatus(context, song))
                        }
                        else -> {
                            addDownloadSong(
                                context,
                                item.songId
                            ) {
                                downloadImageButton.setImageURI(
                                    CatalogItem.getSongDownloadStatus(context, song)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun itemToAbstractItem(view: View, item: Item): GenericItem? {
        val hideToBeSkipped = Settings.getSettings(view.context).getBoolean(view.context.getString(R.string.hideToBeSkippedSetting))

        if(hideToBeSkipped == true){
            val songDatabaseHelper = SongDatabaseHelper(view.context)

            songDatabaseHelper.getSong(item.itemId)?.let{
                if(!filter(it)){
                    return CatalogItem(item.itemId)
                }
            }

            return null
        }else{
            return CatalogItem(item.itemId)
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
        loadSongList(
            context,
            forceReload,
            skip,
            displayLoading,
            finishedCallback = {
                val catalogSongDatabaseHelper = ItemDatabaseHelper(context, "catalog")

                val songList = catalogSongDatabaseHelper.getItems(skip.toLong(), 50)
                val itemList = ArrayList<Item>()

                for (song in songList) {
                    itemList.add(Item(song.songId, null))
                }

                callback(itemList)
            },
            errorCallback = {
                errorCallback(context.getString(R.string.errorLoadingSongList))
            })
    }

    override fun clearDatabase(context: Context) {
        ItemDatabaseHelper(context, "catalog").reCreateTable()
    }
}