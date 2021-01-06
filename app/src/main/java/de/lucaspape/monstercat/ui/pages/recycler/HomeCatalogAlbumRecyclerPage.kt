package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.AlbumItemDatabaseHelper
import de.lucaspape.monstercat.core.database.objects.Album
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.request.async.loadAlbumAsync
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.ui.pages.util.loadAlbumTracks
import de.lucaspape.monstercat.ui.pages.util.playSongsFromViewDataAsync

class HomeCatalogAlbumRecyclerPage(
    private val albumId: String?,
    private val albumMcId: String
) : HomeCatalogRecyclerPage("album-$albumMcId") {

    override fun onItemClick(context: Context, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        val fistItem = viewData[itemIndex]

        if (fistItem is CatalogItem) {
            val skipMonstercatSongs =
                Settings(context).getBoolean(context.getString(R.string.skipMonstercatSongsSetting)) == true

            val catalogViewData = ArrayList<CatalogItem>()

            for (item in viewData) {
                if (item is CatalogItem) {
                    catalogViewData.add(item)
                }
            }

            playSongsFromViewDataAsync(
                context,
                skipMonstercatSongs,
                catalogViewData,
                itemIndex
            )
        }
    }

    override fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemIdList: ArrayList<String>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        if (skip == 0) {
            if (albumId == null) {
                val albumDatabaseHelper = AlbumDatabaseHelper(context)

                val finished: (album: Album) -> Unit = { album ->
                    val albumItemDatabaseHelper =
                        AlbumItemDatabaseHelper(context, album.albumId)
                    val albumItemList = albumItemDatabaseHelper.getAllData()

                    val idList = ArrayList<String>()

                    for (albumItem in albumItemList) {
                        idList.add(albumItem.songId)
                    }

                    callback(idList)
                }

                val album = albumDatabaseHelper.getAlbumFromMcId(albumMcId)

                if (album == null || forceReload) {
                    loadAlbumTracks(
                        context,
                        albumMcId,
                        finishedCallback = {
                            albumDatabaseHelper.getAlbumFromMcId(albumMcId)?.let { album ->
                                finished(album)
                            }
                        },
                        errorCallback = {
                            errorCallback(context.getString(R.string.errorLoadingAlbum))
                        }
                    )
                } else {
                    finished(album)
                }
            } else {
                loadAlbumAsync(
                    context,
                    forceReload,
                    albumId,
                    albumMcId,
                    displayLoading,
                    { _, _, _, _ ->
                        val albumItemDatabaseHelper =
                            AlbumItemDatabaseHelper(context, albumId)

                        val albumItemList = albumItemDatabaseHelper.getAllData()

                        val idList = ArrayList<String>()

                        for (albumItem in albumItemList) {
                            idList.add(albumItem.songId)
                        }

                        callback(idList)
                    },
                    { _, _, _, _ ->
                        errorCallback(context.getString(R.string.errorLoadingAlbumList))
                    })
            }
        } else {
            callback(ArrayList())
        }
    }

    override fun getHeader(context: Context): String? {
        albumId?.let {
            AlbumDatabaseHelper(context).getAlbum(albumId)?.let {
                return it.title
            }
        }

        return null
    }

    override fun clearDatabase(context: Context) {
        albumId?.let {
            AlbumItemDatabaseHelper(context, it).reCreateTable()
        }
    }
}