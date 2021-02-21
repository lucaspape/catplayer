package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.GenreDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.request.async.loadGenre

class GenreContentsRecyclerPage(private val genreId: String) :
    PlaylistContentsRecyclerPage(genreId) {

    override val id = "genre-$genreId"

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        val genreName = GenreDatabaseHelper(context).getGenre(genreId)?.name

        genreName?.let{
            loadGenre(
                context,
                forceReload,
                it, id, skip, 100, displayLoading, finishedCallback = {
                    val playlistItemDatabaseHelper =
                        ItemDatabaseHelper(
                            context,
                            id
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
    }

    override fun getHeader(context: Context): String? {
        GenreDatabaseHelper(context).getGenre(genreId)?.name?.let {
            return it
        }

        return null
    }
}