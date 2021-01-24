package de.lucaspape.monstercat.ui.abstract_items.content

import android.content.Context
import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.async.loadGenresAsync
import de.lucaspape.monstercat.request.async.loadMoodsAsync
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewList

class ExploreItem(val typeName: String, val openMood: (moodId: String) -> Unit, val openGenre: (genreId: String)->Unit) :
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

        var recyclerViewList: RecyclerViewList? = null

        override fun bindView(item: ExploreItem, payloads: List<Any>) {
            recyclerViewList = object : RecyclerViewList(null) {
                override fun onItemClick(
                    context: Context,
                    viewData: ArrayList<GenericItem>,
                    itemIndex: Int
                ) {
                    val clickedItem = viewData[itemIndex]

                    if (clickedItem is MoodItem) {
                        item.openMood(clickedItem.moodId)
                    }else if(clickedItem is GenreItem){
                        item.openGenre(clickedItem.genreId)
                    }
                }

                override fun onItemLongClick(
                    view: View,
                    viewData: ArrayList<GenericItem>,
                    itemIndex: Int
                ) {

                }

                override fun idToAbstractItem(
                    view: View,
                    id: String
                ): GenericItem {
                    return if (item.typeName == "mood") {
                        MoodItem(id)
                    } else {
                        GenreItem(id)
                    }
                }

                override fun load(
                    context: Context,
                    forceReload: Boolean,
                    skip: Int,
                    callback: (itemIdList: ArrayList<String>) -> Unit,
                    errorCallback: (errorMessage: String) -> Unit
                ) {
                    if(item.typeName == "mood"){
                        loadMoodsAsync(context, forceReload, displayLoading = {}, finishedCallback = { results ->
                            if (skip == 0) {
                                val idArray = ArrayList<String>()

                                for (mood in results) {
                                    idArray.add(mood.moodId)
                                }

                                callback(idArray)
                            } else {
                                callback(ArrayList())
                            }
                        }, errorCallback = {})
                    }else if(item.typeName == "genre"){
                        loadGenresAsync(context, forceReload, displayLoading = {}, finishedCallback = { results ->
                            if (skip == 0) {
                                val idArray = ArrayList<String>()

                                for (genre in results) {
                                    idArray.add(genre.genreId)
                                }

                                callback(idArray)
                            } else {
                                callback(ArrayList())
                            }
                        }, errorCallback = {})
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