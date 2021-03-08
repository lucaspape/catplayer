package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.MoodDatabaseHelper
import de.lucaspape.monstercat.request.async.loadMood
import de.lucaspape.monstercat.ui.pages.util.Item

class MoodContentsRecyclerPage(private val moodId: String) :
    PlaylistContentsRecyclerPage(moodId) {

    override val id = "mood-$moodId"

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemList: ArrayList<Item>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        loadMood(
            context,
            forceReload,
            moodId, skip, 100, displayLoading, finishedCallback = {
                val playlistItemDatabaseHelper =
                    ItemDatabaseHelper(
                        context,
                        moodId
                    )

                val playlistItems = playlistItemDatabaseHelper.getAllData(false)

                val itemList = ArrayList<Item>()

                for (i in (playlistItems.size - 1 downTo 0)) {
                    itemList.add(Item(playlistItems[i].songId, null))
                }

                callback(itemList)

            }, errorCallback = {
                errorCallback(context.getString(R.string.errorLoadingPlaylistTracks))
            })
    }

    override fun getHeader(context: Context): String? {
        MoodDatabaseHelper(context).getMood(moodId)?.name?.let {
            return it
        }

        return null
    }
}