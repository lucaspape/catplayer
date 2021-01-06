package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.ImageButton
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.request.async.loadAlbumListAsync
import de.lucaspape.monstercat.ui.abstract_items.content.AlbumItem
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage

class HomeAlbumRecyclerPage(private val onSingleAlbumLoad: (albumId: String, albumMcId: String) -> Unit) :
    RecyclerViewPage("album-list") {

    override fun onItemClick(context: Context, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        val albumDatabaseHelper = AlbumDatabaseHelper(context)

        val albumItem = viewData[itemIndex]

        if(albumItem is AlbumItem){
            albumDatabaseHelper.getAlbum(albumItem.albumId)?.mcID?.let { mcID ->
                onSingleAlbumLoad(albumItem.albumId, mcID)
            }
        }
    }

    override fun onItemLongClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)
        val albumMcIdList = ArrayList<String>()

        for (item in viewData) {
            if(item is AlbumItem){
                albumDatabaseHelper.getAlbum(item.albumId)?.mcID?.let { mcID ->
                    albumMcIdList.add(mcID)
                }
            }
        }

        AlbumItem.showContextMenu(view, albumMcIdList, itemIndex)
    }

    override fun onMenuButtonClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int) {
    }

    override fun onDownloadButtonClick(
        context: Context,
        item: GenericItem,
        downloadImageButton: ImageButton
    ) {
    }

    override fun idToAbstractItem(view:View, id: String): GenericItem {
        return AlbumItem(id, (view.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE))
    }

    override fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        loadAlbumListAsync(context, forceReload, skip, displayLoading, { _, _, _ ->
            val albumDatabaseHelper =
                AlbumDatabaseHelper(context)
            val albumList =
                albumDatabaseHelper.getAlbums(skip.toLong(), 50)

            val idList = ArrayList<String>()

            for (album in albumList) {
                idList.add(album.albumId)
            }

            callback(idList)

        }, { _, _, _ ->
            errorCallback(context.getString(R.string.errorLoadingAlbumList))
        })
    }

    override fun getHeader(context: Context): String? {
        return null
    }

    override fun clearDatabase(context: Context) {
        AlbumDatabaseHelper(context).reCreateTable(context, false)
    }
}