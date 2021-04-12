package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.core.database.objects.Album
import de.lucaspape.monstercat.request.async.loadAlbum
import de.lucaspape.monstercat.request.async.loadAlbumTracks
import de.lucaspape.monstercat.ui.pages.util.Item
import de.lucaspape.monstercat.ui.pages.util.StringItem

class HomeCatalogAlbumRecyclerPage(
    private val albumId: String?,
    private val albumMcId: String
) : HomeCatalogRecyclerPage() {

    override val id = "album-$albumId"

    override suspend fun load(
        context: Context,
        forceReload: Boolean,
        skip: Int,
        displayLoading: () -> Unit,
        callback: (itemList: ArrayList<Item>) -> Unit,
        errorCallback: (errorMessage: String) -> Unit
    ) {
        if (skip == 0) {
            if (albumId == null) {
                val albumDatabaseHelper = AlbumDatabaseHelper(context)

                val finished: (album: Album) -> Unit = { album ->
                    val albumItemDatabaseHelper =
                        ItemDatabaseHelper(context, album.albumId)
                    val albumItemList = albumItemDatabaseHelper.getAllData(false)

                    val itemList = ArrayList<Item>()

                    for (albumItem in albumItemList) {
                        itemList.add(StringItem(null, albumItem.songId))
                    }

                    callback(itemList)
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
                loadAlbum(
                    context,
                    forceReload,
                    albumId,
                    albumMcId,
                    displayLoading,
                    {
                        val albumItemDatabaseHelper =
                            ItemDatabaseHelper(context, albumId)

                        val albumItemList = albumItemDatabaseHelper.getAllData(true)

                        val itemList = ArrayList<Item>()

                        for (albumItem in albumItemList) {
                            itemList.add(StringItem(null, albumItem.songId))
                        }

                        callback(itemList)
                    },
                    {
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
                return it.shownTitle
            }
        }

        return null
    }

    override fun clearDatabase(context: Context) {
        albumId?.let {
            ItemDatabaseHelper(context, it).reCreateTable()
        }
    }
}