package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.GenreDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.MoodDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.StreamDatabaseHelper
import de.lucaspape.monstercat.ui.abstract_items.content.ExploreItem
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.ui.pages.util.Item

open class ExploreRecyclerPage(
    val openMood: (moodId: String) -> Unit,
    val openGenre: (genreId: String) -> Unit,
    val openPublicPlaylist: (publicPlaylistId:String) -> Unit
) : HomeCatalogRecyclerPage() {

    override val id = "explore"

    private var currentMoodId = ""
    private var currentGenreId = ""
    private var currentPublicPlaylistId = ""

    override suspend fun itemToAbstractItem(view: View, item: Item): GenericItem {
        return if (item.typeId == "separator") {
            HeaderTextItem(item.itemId)
        } else {
            ExploreItem(item.itemId, {
                currentMoodId = it
                currentGenreId = ""
                currentPublicPlaylistId = ""

                saveData()

                openMood(it)
            }, {
                currentMoodId = ""
                currentGenreId = it
                currentPublicPlaylistId = ""

                saveData()

                openGenre(it)
            }, {
                currentMoodId = ""
                currentGenreId = ""
                currentPublicPlaylistId = it

                saveData()

                openPublicPlaylist(it)
            })
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
            val items = ArrayList<Item>()

            items.add(Item(context.getString(R.string.streams), "separator"))
            items.add(Item("stream", "item"))

            items.add(Item("Public Playlists", "separator"))
            items.add(Item("public-playlists", "item"))

            items.add(Item(context.getString(R.string.moods), "separator"))
            items.add(Item("mood", "item"))

            items.add(Item(context.getString(R.string.genres), "separator"))
            items.add(Item("genre", "item"))

            items.add(Item(context.getString(R.string.greatestHits), "separator"))
            items.add(Item("greatest-hits", "item"))

            callback(items)
        } else {
            callback(ArrayList())
        }
    }

    override fun clearDatabase(context: Context) {
        MoodDatabaseHelper(context).reCreateTable()
        GenreDatabaseHelper(context).reCreateTable()
        ItemDatabaseHelper(context, "greatest-hits").reCreateTable()
        StreamDatabaseHelper(context).reCreateTable()
    }

    override fun restore(data: HashMap<String, String>?): Boolean {
        if (data != null) {
            val moodId = data["moodId"]
            val genreId = data["genreId"]
            val publicPlaylistId = data["publicPlaylistId"]

            return if (!moodId.isNullOrBlank()) {
                openMood(moodId)
                true
            } else if (!genreId.isNullOrBlank()) {
                openGenre(genreId)
                true
            } else if(!publicPlaylistId.isNullOrBlank()){
                openPublicPlaylist(publicPlaylistId)
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
        hashMap["publicPlaylistId"] = currentPublicPlaylistId

        return hashMap
    }
}