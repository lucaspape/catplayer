package de.lucaspape.monstercat.ui.abstract_items.content

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.flavor.playYoutubeLivestream
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.StreamDatabaseHelper
import de.lucaspape.monstercat.core.music.util.playStream
import de.lucaspape.monstercat.request.async.*
import de.lucaspape.monstercat.ui.pages.recycler.HomeCatalogRecyclerPage
import de.lucaspape.monstercat.ui.pages.util.Item
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import de.lucaspape.monstercat.ui.pages.util.playSongsFromViewDataAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExploreItem(
    val typeName: String,
    val openMood: (moodId: String) -> Unit,
    val openGenre: (genreId: String) -> Unit,
    val openPublicPlaylist: (publicPlaylistid: String) -> Unit
) :
    AbstractItem<ExploreItem.ViewHolder>() {
    override val type: Int = 1003

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
            recyclerViewList = object : HomeCatalogRecyclerPage() {
                override val id = "explore-${item.typeName}"

                override suspend fun onItemClick(
                    context: Context,
                    viewData: List<GenericItem>,
                    itemIndex: Int
                ) {
                    super.onItemClick(context, viewData, itemIndex)

                    when (val clickedItem = viewData[itemIndex]) {
                        is MoodItem -> {
                            withContext(Dispatchers.Main) {
                                item.openMood(clickedItem.moodId)
                            }
                        }
                        is GenreItem -> {
                            withContext(Dispatchers.Main) {
                                item.openGenre(clickedItem.genreId)
                            }
                        }
                        is CatalogItem -> {
                            val catalogViewData = ArrayList<CatalogItem>()

                            for (catalogItem in viewData) {
                                if (catalogItem is CatalogItem) {
                                    catalogViewData.add(catalogItem)
                                }
                            }

                            withContext(Dispatchers.Main) {
                                playSongsFromViewDataAsync(
                                    context,
                                    catalogViewData,
                                    itemIndex
                                )
                            }
                        }
                        is StreamItem -> {
                            withContext(Dispatchers.Main) {
                                val stream = StreamDatabaseHelper(context).getStream(clickedItem.streamName)
                                
                                if(stream?.streamUrl?.contains("youtube") == true){
                                    playYoutubeLivestream(view, clickedItem.streamName)
                                }else{
                                    playStream(context, clickedItem.streamName)
                                }
                            }
                        }
                        is PublicPlaylistItem -> {
                            withContext(Dispatchers.Main){
                                item.openPublicPlaylist(clickedItem.publicPlaylistId)
                            }
                        }
                    }
                }

                override suspend fun itemToAbstractItem(
                    view: View,
                    item: Item
                ): GenericItem {
                    return when (item.typeId) {
                        "mood" -> {
                            MoodItem(item.itemId)
                        }
                        "genre" -> {
                            GenreItem(item.itemId)
                        }
                        "stream" -> {
                            StreamItem(item.itemId)
                        }
                        "public-playlist" -> {
                            PublicPlaylistItem(item.itemId)
                        }
                        else -> {
                            CatalogItem(item.itemId)
                        }
                    }
                }

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
                    callback: (itemList: ArrayList<Item>) -> Unit,
                    errorCallback: (errorMessage: String) -> Unit
                ) {
                    if (skip == 0) {
                        when (item.typeName) {
                            "mood" -> {
                                loadMoods(
                                    context,
                                    forceReload,
                                    displayLoading = {},
                                    finishedCallback = { results ->

                                        val itemArray = ArrayList<Item>()

                                        for (mood in results) {
                                            itemArray.add(Item(mood.moodId, "mood"))
                                        }

                                        callback(itemArray)
                                    },
                                    errorCallback = {})

                            }
                            "genre" -> {
                                loadGenres(
                                    context,
                                    forceReload,
                                    displayLoading = {},
                                    finishedCallback = { results ->
                                        val itemArray = ArrayList<Item>()

                                        for (genre in results) {
                                            itemArray.add(Item(genre.genreId, "genre"))
                                        }

                                        callback(itemArray)
                                    },
                                    errorCallback = {})

                            }
                            "greatest-hits" -> {
                                loadGreatestHits(
                                    context,
                                    forceReload,
                                    skip,
                                    100,
                                    displayLoading = {},
                                    finishedCallback = {
                                        val itemArray = ArrayList<Item>()

                                        val playlistItemDatabaseHelper =
                                            ItemDatabaseHelper(context, "greatest-hits")
                                        val items = playlistItemDatabaseHelper.getAllData(true)

                                        for (playlistItem in items) {
                                            itemArray.add(Item(playlistItem.songId, null))
                                        }

                                        callback(itemArray)
                                    },
                                    errorCallback = {})
                            }
                            "stream" -> {
                                loadLiveStreams(
                                    context,
                                    forceReload,
                                    displayLoading = {
                                    },
                                    finishedCallback = {
                                        val itemArray = ArrayList<Item>()

                                        val streamDatabaseHelper = StreamDatabaseHelper(context)
                                        val streams = streamDatabaseHelper.getAllStreams()

                                        for (stream in streams) {
                                            itemArray.add(Item(stream.name, "stream"))
                                        }

                                        callback(itemArray)
                                    },
                                    errorCallback = {
                                    })
                            }
                            "public-playlists" -> {
                                loadPublicPlaylists(
                                    context,
                                    forceReload,
                                    displayLoading = {},
                                    finishedCallback = { results ->

                                        val itemArray = ArrayList<Item>()

                                        for (publicPlaylist in results) {
                                            itemArray.add(Item(publicPlaylist.playlistId, "public-playlist"))
                                        }

                                        callback(itemArray)
                                    },
                                    errorCallback = {})
                            }
                        }
                    } else {
                        callback(ArrayList())
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