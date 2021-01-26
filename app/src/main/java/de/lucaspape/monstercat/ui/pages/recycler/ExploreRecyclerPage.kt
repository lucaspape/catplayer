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

    private var currentMoodId = ""
    private var currentGenreId = ""

    override suspend fun idToAbstractItem(view: View, id: String): GenericItem {
        return if (id.contains("separator-")) {
            HeaderTextItem(id.replace("separator-", ""))
        } else {
            ExploreItem(id.replace("item-", ""), {
                currentMoodId = it
                currentGenreId = ""

                openMood(it)
            }, {
                currentMoodId = ""
                currentGenreId = it

                openGenre(it)
            })
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

    override fun restore(data: HashMap<String, String>?): Boolean {
        if (data != null) {
            val moodId = data["moodId"]
            val genreId = data["genreId"]

            return if (!moodId.isNullOrBlank()) {
                openMood(moodId)
                true
            } else if (!genreId.isNullOrBlank()) {
                openGenre(genreId)
                true
            } else {
                false
            }
        } else {
            return false
        }
    }

    override fun save(): HashMap<String, String> {
        val hashMap = HashMap<String, String>()

        hashMap["moodId"] = currentMoodId
        hashMap["genreId"] = currentGenreId

        return hashMap
    }
}