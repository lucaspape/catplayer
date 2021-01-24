package de.lucaspape.monstercat.ui.abstract_items.content

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.download.addDownloadSong
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.request.async.loadGenresAsync
import de.lucaspape.monstercat.request.async.loadGreatestHitsAsync
import de.lucaspape.monstercat.request.async.loadMoodsAsync
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import de.lucaspape.monstercat.ui.pages.util.playSongsFromViewDataAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ExploreItem(
    val typeName: String,
    val openMood: (moodId: String) -> Unit,
    val openGenre: (genreId: String) -> Unit
) :
    AbstractItem<ExploreItem.ViewHolder>() {
    override val type: Int = 152

    override val layoutRes: Int
        get() = R.layout.list_explore

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(private val view: View) : FastAdapter.ViewHolder<ExploreItem>(view) {
        private var recyclerViewList: RecyclerViewPage? = null

        override fun bindView(item: ExploreItem, payloads: List<Any>) {
            recyclerViewList = object : RecyclerViewPage() {
                override suspend fun onItemClick(
                    context: Context,
                    viewData: ArrayList<GenericItem>,
                    itemIndex: Int
                ) {
                    super.onItemClick(context, viewData, itemIndex)

                    when (val clickedItem = viewData[itemIndex]) {
                        is MoodItem -> {
                            withContext(Dispatchers.Main){
                                item.openMood(clickedItem.moodId)
                            }
                        }
                        is GenreItem -> {
                            withContext(Dispatchers.Main){
                                item.openGenre(clickedItem.genreId)
                            }
                        }
                        is CatalogItem -> {
                            val skipMonstercatSongs =
                                Settings(context).getBoolean(context.getString(R.string.skipMonstercatSongsSetting)) == true

                            val catalogViewData = ArrayList<CatalogItem>()

                            for (catalogItem in viewData) {
                                if (catalogItem is CatalogItem) {
                                    catalogViewData.add(catalogItem)
                                }
                            }

                            withContext(Dispatchers.Main){
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

                override suspend fun onItemLongClick(
                    view: View,
                    viewData: ArrayList<GenericItem>,
                    itemIndex: Int
                ) {
                    super.onItemLongClick(view, viewData, itemIndex)

                    if(viewData[itemIndex] is CatalogItem){
                        val idList = ArrayList<String>()

                        for (catalogItem in viewData) {
                            if (catalogItem is CatalogItem) {
                                idList.add(catalogItem.songId)
                            }
                        }

                        withContext(Dispatchers.Main){
                            CatalogItem.showContextMenu(view, idList, itemIndex)
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

                        song?.let {
                            when {
                                File(song.downloadLocation).exists() -> {
                                    File(song.downloadLocation).delete()
                                    downloadImageButton.setImageURI(
                                        CatalogItem.getSongDownloadStatus(
                                            song
                                        )
                                    )
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

                override suspend fun idToAbstractItem(
                    view: View,
                    id: String
                ): GenericItem {
                    return when (item.typeName) {
                        "mood" -> {
                            MoodItem(id)
                        }
                        "genre" -> {
                            GenreItem(id)
                        }
                        else -> {
                            CatalogItem(id)
                        }
                    }
                }

                override fun getHeader(context: Context): String? {
                    return null
                }

                override val pageSize: Int = 100
                override fun getOrientation(view: View): Int {
                    return if (item.typeName == "greatest-hits") {
                        LinearLayout.VERTICAL
                    } else {
                        LinearLayout.HORIZONTAL
                    }
                }

                override suspend fun load(
                    context: Context,
                    forceReload: Boolean,
                    skip: Int,
                    displayLoading: () -> Unit,
                    callback: (itemIdList: ArrayList<String>) -> Unit,
                    errorCallback: (errorMessage: String) -> Unit
                ) {
                    when (item.typeName) {
                        "mood" -> {
                            loadMoodsAsync(
                                context,
                                forceReload,
                                displayLoading = {},
                                finishedCallback = { results ->
                                    if (skip == 0) {
                                        val idArray = ArrayList<String>()

                                        for (mood in results) {
                                            idArray.add(mood.moodId)
                                        }

                                        callback(idArray)
                                    } else {
                                        callback(ArrayList())
                                    }
                                },
                                errorCallback = {})
                        }
                        "genre" -> {
                            loadGenresAsync(
                                context,
                                forceReload,
                                displayLoading = {},
                                finishedCallback = { results ->
                                    if (skip == 0) {
                                        val idArray = ArrayList<String>()

                                        for (genre in results) {
                                            idArray.add(genre.genreId)
                                        }

                                        callback(idArray)
                                    } else {
                                        callback(ArrayList())
                                    }
                                },
                                errorCallback = {})
                        }
                        "greatest-hits" -> {
                            if (skip == 0) {
                                loadGreatestHitsAsync(
                                    context,
                                    forceReload,
                                    skip,
                                    100,
                                    displayLoading = {},
                                    finishedCallback = {
                                        val idArray = ArrayList<String>()

                                        val playlistItemDatabaseHelper =
                                            PlaylistItemDatabaseHelper(context, "greatest-hits")
                                        val items = playlistItemDatabaseHelper.getAllData(true)

                                        for (playlistItem in items) {
                                            idArray.add(playlistItem.songId)
                                        }

                                        callback(idArray)
                                    },
                                    errorCallback = {})
                            } else {
                                callback(ArrayList())
                            }
                        }
                    }
                }
            }

            recyclerViewList?.onCreate(view)
        }

        override fun unbindView(item: ExploreItem) {
            recyclerViewList = null
        }
    }
}