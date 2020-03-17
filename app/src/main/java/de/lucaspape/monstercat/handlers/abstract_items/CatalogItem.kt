package de.lucaspape.monstercat.handlers.abstract_items

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.*
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.download.downloadCoverIntoImageReceiver
import de.lucaspape.monstercat.handlers.addSongToPlaylist
import de.lucaspape.monstercat.handlers.deletePlaylistSong
import de.lucaspape.monstercat.handlers.openAlbum
import de.lucaspape.monstercat.handlers.playSongFromId
import de.lucaspape.monstercat.download.ImageReceiverInterface
import de.lucaspape.monstercat.request.async.BackgroundAsync
import de.lucaspape.monstercat.util.displayAlertDialogList

open class CatalogItem(
    val songId: String
) : AbstractItem<CatalogItem.ViewHolder>() {

    companion object {
        @JvmStatic
        fun showContextMenu(
            view: View,
            contentList: ArrayList<String>,
            listViewPosition: Int
        ) {
            val context = view.context

            val itemList = arrayListOf(
                AlertListItem(context.getString(R.string.download), downloadDrawable),
                AlertListItem(context.getString(R.string.addToQueue), addToQueueDrawable),
                AlertListItem(context.getString(R.string.addToPlaylist), addToPlaylistDrawable),
                AlertListItem(context.getString(R.string.shareAlbum), shareDrawable),
                AlertListItem(context.getString(R.string.openAlbumInApp), openInAppDrawable)
            )

            displayAlertDialogList(context, "", itemList) { _, item ->
                val id = contentList[listViewPosition]
                val songDatabaseHelper =
                    SongDatabaseHelper(context)
                val song = songDatabaseHelper.getSong(context, id)

                song?.let {
                    when (item.itemText) {
                        context.getString(R.string.download) -> addDownloadSong(
                            context,
                            song.songId
                        ) {}
                        context.getString(R.string.addToQueue) -> playSongFromId(
                            id,
                            false,
                            priority = true
                        )
                        context.getString(R.string.addToPlaylist) -> addSongToPlaylist(
                            view,
                            song.songId
                        )
                        context.getString(R.string.shareAlbum) -> openAlbum(
                            view,
                            song.mcAlbumId,
                            true
                        )
                        context.getString(R.string.openAlbumInApp) -> openAlbum(
                            view,
                            song.mcAlbumId,
                            false
                        )
                    }
                }
            }
        }

        @JvmStatic
        fun showContextMenuPlaylist(
            view: View,
            data: ArrayList<String>,
            listViewPosition: Int,
            playlistId: String
        ) {
            val context = view.context

            val itemList = arrayListOf(
                AlertListItem(context.getString(R.string.download), downloadDrawable),
                AlertListItem(context.getString(R.string.addToQueue), addToQueueDrawable),
                AlertListItem(context.getString(R.string.delete), deleteDrawable),
                AlertListItem(context.getString(R.string.shareAlbum), shareDrawable),
                AlertListItem(context.getString(R.string.openAlbumInApp), openInAppDrawable)
            )

            displayAlertDialogList(context, "", itemList) { _, item ->
                val id = data[listViewPosition]
                when (item.itemText) {
                    context.getString(R.string.download) -> {
                        val songDatabaseHelper =
                            SongDatabaseHelper(context)
                        val song =
                            songDatabaseHelper.getSong(context, id)

                        if (song != null) {
                            addDownloadSong(context, song.songId) {}
                        }
                    }
                    context.getString(R.string.addToQueue) -> {
                        playSongFromId(
                            id,
                            false,
                            priority = true
                        )
                    }
                    context.getString(R.string.delete) -> {
                        deletePlaylistSong(
                            view,
                            id,
                            playlistId,
                            listViewPosition + 1,
                            data.size
                        )
                    }
                    context.getString(R.string.shareAlbum) -> {
                        val songDatabaseHelper =
                            SongDatabaseHelper(context)
                        val song =
                            songDatabaseHelper.getSong(context, id)

                        if (song != null) {
                            openAlbum(view, song.mcAlbumId, true)
                        }
                    }
                    context.getString(R.string.openAlbumInApp) -> {
                        val songDatabaseHelper =
                            SongDatabaseHelper(context)
                        val song =
                            songDatabaseHelper.getSong(context, id)

                        if (song != null) {
                            openAlbum(view, song.mcAlbumId, false)
                        }

                    }
                }
            }
        }
    }

    override val type: Int = 101

    override val layoutRes: Int
        get() = R.layout.list_single

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<CatalogItem>(view) {
        private val titleTextView: TextView = view.findViewById(R.id.title)
        private val artistTextView: TextView = view.findViewById(R.id.artist)
        val titleMenuButton: ImageButton = view.findViewById(R.id.titleMenuButton)
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        val titleDownloadButton: ImageButton =
            view.findViewById(R.id.titleDownloadButton)
        private val context = view.context

        private var albumId = ""

        override fun bindView(item: CatalogItem, payloads: MutableList<Any>) {
            val songDatabaseHelper = SongDatabaseHelper(context)
            val song = songDatabaseHelper.getSong(context, item.songId)

            song?.let {
                albumId = song.albumId

                val shownTitle = "${song.title} ${song.version}"

                titleTextView.text = shownTitle
                artistTextView.text = song.artist

                downloadCoverIntoImageReceiver(context, object : ImageReceiverInterface {
                    override fun setBitmap(id: String, bitmap: Bitmap?) {
                        if (id == albumId) {
                            coverImageView.setImageBitmap(bitmap)
                        }
                    }

                    override fun setDrawable(id: String, drawable: Drawable?) {
                        if (id == albumId) {
                            coverImageView.setImageDrawable(drawable)
                        }
                    }
                }, song.albumId, true)

                var downloadStatus =
                    downloadDrawable.toUri()

                titleDownloadButton.setImageURI(downloadStatus)

                BackgroundAsync({
                    downloadStatus = song.getSongDownloadStatus().toUri()
                }, {
                    titleDownloadButton.setImageURI(downloadStatus)
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }

        override fun unbindView(item: CatalogItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
            titleDownloadButton.setImageURI(null)
        }
    }
}