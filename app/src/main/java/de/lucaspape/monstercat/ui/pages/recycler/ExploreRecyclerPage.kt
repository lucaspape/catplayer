package de.lucaspape.monstercat.ui.pages.recycler

import android.content.Context
import android.view.View
import android.widget.ImageButton
import com.mikepenz.fastadapter.GenericItem
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.GenreDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.MoodDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.download.addDownloadSong
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.ui.abstract_items.content.ExploreItem
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.ui.pages.util.playSongsFromCatalogDbAsync
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage
import java.io.File

open class ExploreRecyclerPage(
    cacheId: String,
    val openMood: (moodId: String) -> Unit,
    val openGenre: (genreId: String) -> Unit
) : RecyclerViewPage(cacheId) {

    override fun onItemClick(context: Context, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        val fistItem = viewData[itemIndex]

        if (fistItem is CatalogItem) {
            val skipMonstercatSongs =
                Settings(context).getBoolean(context.getString(R.string.skipMonstercatSongsSetting)) == true

            playSongsFromCatalogDbAsync(
                context,
                skipMonstercatSongs,
                fistItem.songId
            )
        }
    }

    override fun onItemLongClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        val idList = ArrayList<String>()

        for (item in viewData) {
            if (item is CatalogItem) {
                idList.add(item.songId)
            }
        }

        CatalogItem.showContextMenu(view, idList, itemIndex)
    }

    override fun onMenuButtonClick(view: View, viewData: ArrayList<GenericItem>, itemIndex: Int) {
        onItemLongClick(view, viewData, itemIndex)
    }

    override fun onDownloadButtonClick(
        context: Context,
        item: GenericItem,
        downloadImageButton: ImageButton
    ) {
        if (item is CatalogItem) {
            val songDatabaseHelper = SongDatabaseHelper(context)
            val song = songDatabaseHelper.getSong(context, item.songId)

            song?.let {
                when {
                    File(song.downloadLocation).exists() -> {
                        File(song.downloadLocation).delete()
                        downloadImageButton.setImageURI(CatalogItem.getSongDownloadStatus(song))
                    }
                    else -> {
                        addDownloadSong(
                            context,
                            item.songId
                        ) {
                            downloadImageButton.setImageURI(
                                CatalogItem.getSongDownloadStatus(song)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun idToAbstractItem(view: View, id: String): GenericItem {
        return if (id.contains("separator-")) {
            HeaderTextItem(id.replace("separator-", ""))
        } else {
            ExploreItem(id.replace("item-", ""), openMood, openGenre)
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

    override fun getHeader(context: Context): String? {
        return null
    }

    override fun clearDatabase(context: Context) {
        MoodDatabaseHelper(context).reCreateTable()
        GenreDatabaseHelper(context).reCreateTable()
    }
}