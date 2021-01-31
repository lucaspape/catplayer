package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.MoodDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.request.async.loadMood

class MoodContentsRecyclerPage(private val moodId: String) :
    PlaylistContentsRecyclerPage(moodId) {

    override val id = "mood-$moodId"

    override val pageSize = 100

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        loadMood(
            context,
            forceReload,
            moodId, skip, 100, displayLoading, finishedCallback = {
                val playlistItemDatabaseHelper =
                    PlaylistItemDatabaseHelper(
                        context,
                        moodId
                    )

                val playlistItems = playlistItemDatabaseHelper.getAllData(false)

                val idList = ArrayList<String>()

                for (i in (playlistItems.size - 1 downTo 0)) {
                    idList.add(playlistItems[i].songId)
                }

                callback(idList)

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