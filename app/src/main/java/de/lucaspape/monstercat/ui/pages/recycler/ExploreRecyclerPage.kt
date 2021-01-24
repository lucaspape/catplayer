package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.core.database.helper.GenreDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.MoodDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.ui.abstract_items.content.ExploreItem
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem

open class ExploreRecyclerPage(
    val openMood: (moodId: String) -> Unit,
    val openGenre: (genreId: String) -> Unit
) : HomeCatalogRecyclerPage() {

    override val id = "explore"

    override suspend fun idToAbstractItem(view: View, id: String): GenericItem {
        return if (id.contains("separator-")) {
            HeaderTextItem(id.replace("separator-", ""))
        } else {
            ExploreItem(id.replace("item-", ""), openMood, openGenre)
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
            val idArray = ArrayList<String>()

            idArray.add("separator-Moods")
            idArray.add("item-mood")

            idArray.add("separator-Genres")
            idArray.add("item-genre")

            idArray.add("separator-Greatest Hits")
            idArray.add("item-greatest-hits")

            callback(idArray)
        } else {
            callback(ArrayList())
        }
    }

    override fun clearDatabase(context: Context) {
        MoodDatabaseHelper(context).reCreateTable()
        GenreDatabaseHelper(context).reCreateTable()
        PlaylistItemDatabaseHelper(context, "greatest-hits").reCreateTable()
    }
}