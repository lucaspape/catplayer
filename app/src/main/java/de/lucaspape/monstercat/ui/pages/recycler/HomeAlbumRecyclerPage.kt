package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.LinearLayout
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.request.async.loadAlbumListAsync
import de.lucaspape.monstercat.ui.abstract_items.content.AlbumItem
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeAlbumRecyclerPage(private val onSingleAlbumLoad: (albumId: String, albumMcId: String) -> Unit) :
    RecyclerViewPage() {

    override val id = "albums"

    private var currentAlbumId = ""
    private var currentAlbumMcId = ""

    override suspend fun onItemClick(context: Context, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        super.onItemClick(context, viewData, itemIndex)

        val albumDatabaseHelper = AlbumDatabaseHelper(context)

        val albumItem = viewData[itemIndex]

        if (albumItem is AlbumItem) {
            albumDatabaseHelper.getAlbum(albumItem.albumId)?.mcID?.let { mcID ->
                withContext(Dispatchers.Main){
                    currentAlbumId = albumItem.albumId
                    currentAlbumMcId = mcID

                    saveData()

                    onSingleAlbumLoad(albumItem.albumId, mcID)
                }
            }
        }
    }

    override suspend fun onItemLongClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        super.onItemLongClick(view, viewData, itemIndex)

        val albumDatabaseHelper = AlbumDatabaseHelper(view.context)
        val albumMcIdList = ArrayList<String>()

        for (item in viewData) {
            if (item is AlbumItem) {
                albumDatabaseHelper.getAlbum(item.albumId)?.mcID?.let { mcID ->
                    albumMcIdList.add(mcID)
                }
            }
        }

        withContext(Dispatchers.Main){
            AlbumItem.showContextMenu(view, albumMcIdList, itemIndex)
        }
    }

    override suspend fun idToAbstractItem(view: View, id: String): GenericItem {
        return AlbumItem(
            id,
            (view.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        )
    }

    override suspend fun load(
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

    override fun clearDatabase(context: Context) {
        AlbumDatabaseHelper(context).reCreateTable(context, false)
    }

    override fun getOrientation(view: View): Int {
        return if(view.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            LinearLayout.HORIZONTAL
        }else{
            LinearLayout.VERTICAL
        }
    }

    override fun restore(data: HashMap<String, String>?): Boolean {
        return if (data != null) {
            val albumId = data["albumId"]
            val albumMcId = data["albumMcId"]

            if (!albumId.isNullOrBlank() && ! albumMcId.isNullOrBlank()) {
                onSingleAlbumLoad(albumId, albumMcId)
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

        hashMap["albumId"] = currentAlbumId
        hashMap["albumMcId"] = currentAlbumMcId

        return hashMap
    }
}