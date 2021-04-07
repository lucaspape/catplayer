package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.GenreDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.request.async.loadGenre
import de.lucaspape.monstercat.ui.pages.util.Item

class GenreContentsRecyclerPage(private val genreId: String) :
    PlaylistContentsRecyclerPage(genreId) {

    override val id = "genre-$genreId"

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemList: ArrayList<Item>) -> Unit,
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

                    val itemList = ArrayList<Item>()

                    for (i in (playlistItems.size - 1 downTo 0)) {
                        itemList.add(Item(null, playlistItems[i].songId))
                    }

                    callback(itemList)

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