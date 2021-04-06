package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.core.database.helper.FilterDatabaseHelper
import de.lucaspape.monstercat.core.music.applyFilterSettings
import de.lucaspape.monstercat.ui.abstract_items.content.FilterItem
import de.lucaspape.monstercat.ui.pages.util.Item
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage

class FiltersRecyclerPage : RecyclerViewPage() {
    override suspend fun onMenuButtonClick(
        view: View,
        viewData: List<GenericItem>,
        itemIndex: Int
    ) {
        val item = viewData[itemIndex]

        if(item is FilterItem){
            FilterDatabaseHelper(view.context).removeFilter(item.filter.id)
        }

        removeItem(itemIndex) {
            applyFilterSettings(view.context)
        }
    }

    override suspend fun itemToAbstractItem(view: View, item: Item): GenericItem? {
        FilterDatabaseHelper(view.context).getFilterFromId(Integer.parseInt(item.itemId))?.let{
            return FilterItem(it)
        }

        return null
    }

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemList: ArrayList<Item>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        if(skip == 0){
            val filterDatabaseHelper = FilterDatabaseHelper(context)

            val filters = filterDatabaseHelper.getAllFilters()

            val itemList = ArrayList<Item>()

            for(filter in filters){
                itemList.add(Item(filter.id.toString(), null))
            }

            callback(itemList)
        }else{
            callback(ArrayList())
        }
    }
}