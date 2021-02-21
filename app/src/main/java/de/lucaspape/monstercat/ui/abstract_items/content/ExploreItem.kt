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
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.request.async.loadGenres
import de.lucaspape.monstercat.request.async.loadGreatestHits
import de.lucaspape.monstercat.request.async.loadLiveStreams
import de.lucaspape.monstercat.request.async.loadMoods
import de.lucaspape.monstercat.ui.displaySnackBar
import de.lucaspape.monstercat.ui.pages.recycler.HomeCatalogRecyclerPage
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import de.lucaspape.monstercat.ui.pages.util.playSongsFromViewDataAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            recyclerViewList = object : HomeCatalogRecyclerPage() {
                override val id = "explore-${item.typeName}"

                override suspend fun onItemClick(
                    context: Context,
                    viewData: ArrayList<GenericItem>,
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
                            val skipMonstercatSongs =
                                Settings(context).getBoolean(context.getString(R.string.skipMonstercatSongsSetting)) == true

                            val catalogViewData = ArrayList<CatalogItem>()

                            for (catalogItem in viewData) {
                                if (catalogItem is CatalogItem) {
                                    catalogViewData.add(catalogItem)
                                }
                            }

                            withContext(Dispatchers.Main) {
                                playSongsFromViewDataAsync(
                                    context,
                                    skipMonstercatSongs,
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
                        "stream" -> {
                            StreamItem(id)
                        }
                        else -> {
                            CatalogItem(id)
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
                    callback: (itemIdList: ArrayList<String>) -> Unit,
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

                                        val idArray = ArrayList<String>()

                                        for (mood in results) {
                                            idArray.add(mood.moodId)
                                        }

                                        callback(idArray)
                                    },
                                    errorCallback = {})

                            }
                            "genre" -> {
                                loadGenres(
                                    context,
                                    forceReload,
                                    displayLoading = {},
                                    finishedCallback = { results ->
                                        val idArray = ArrayList<String>()

                                        for (genre in results) {
                                            idArray.add(genre.genreId)
                                        }

                                        callback(idArray)
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
                                        val idArray = ArrayList<String>()

                                        val playlistItemDatabaseHelper =
                                            ItemDatabaseHelper(context, "greatest-hits")
                                        val items = playlistItemDatabaseHelper.getAllData(true)

                                        for (playlistItem in items) {
                                            idArray.add(playlistItem.songId)
                                        }

                                        callback(idArray)
                                    },
                                    errorCallback = {})
                            }
                            "stream" -> {
                                loadLiveStreams(
                                    context,
                                    forceReload,
                                    displayLoading = {
                                        displaySnackBar(
                                            view,
                                            view.context.getString(R.string.livestreamWarning),
                                            null
                                        ) {}
                                    },
                                    finishedCallback = {
                                        val idArray = ArrayList<String>()

                                        val streamDatabaseHelper = StreamDatabaseHelper(context)
                                        val streams = streamDatabaseHelper.getAllStreams()

                                        for (stream in streams) {
                                            idArray.add(stream.name)
                                        }

                                        callback(idArray)
                                    },
                                    errorCallback = {
                                    })
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