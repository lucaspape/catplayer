package de.lucaspape.monstercat.ui.abstract_items.content

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Target
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.download.addDownloadSong
import de.lucaspape.monstercat.core.download.downloadCoverIntoImageReceiver
import de.lucaspape.monstercat.ui.handlers.addSongToPlaylist
import de.lucaspape.monstercat.ui.handlers.deletePlaylistSong
import de.lucaspape.monstercat.ui.handlers.openAlbum
import de.lucaspape.monstercat.core.download.ImageReceiverInterface
import de.lucaspape.monstercat.core.download.preDownloadCallbacks
import de.lucaspape.monstercat.core.music.prioritySongQueue
import de.lucaspape.monstercat.ui.*
import de.lucaspape.monstercat.ui.abstract_items.alert_list.AlertListHeaderItem
import de.lucaspape.monstercat.ui.abstract_items.alert_list.AlertListItem
import de.lucaspape.util.BackgroundAsync
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
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

            val id = contentList[listViewPosition]

            SongDatabaseHelper(view.context).getSong(view.context, id)?.let { song ->
                val itemList = arrayListOf(
                    AlertListItem(
                        view.context.getString(R.string.download),
                        downloadDrawable
                    ),
                    AlertListItem(
                        view.context.getString(R.string.addToQueue),
                        addToQueueDrawable
                    ),
                    AlertListItem(
                        view.context.getString(R.string.addToPlaylist),
                        addToPlaylistDrawable
                    ),
                    AlertListItem(
                        view.context.getString(R.string.shareAlbum),
                        shareDrawable
                    ),
                    AlertListItem(
                        view.context.getString(R.string.openAlbumInApp),
                        openInAppDrawable
                    )
                )

                displayAlertDialogList(
                    view.context,
                    AlertListHeaderItem(
                        song.shownTitle,
                        song.albumId
                    ),
                    itemList
                ) { _, item ->
                    when (item.itemText) {
                        view.context.getString(R.string.download) -> addDownloadSong(
                            view.context,
                            song.songId
                        ) {}
                        view.context.getString(R.string.addToQueue) -> prioritySongQueue.add(id)
                        view.context.getString(R.string.addToPlaylist) -> addSongToPlaylist(
                            view,
                            song.songId
                        )
                        view.context.getString(R.string.shareAlbum) -> openAlbum(
                            view,
                            song.mcAlbumId,
                            true
                        )
                        view.context.getString(R.string.openAlbumInApp) -> openAlbum(
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
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

            val itemList = arrayListOf(
                AlertListItem(
                    view.context.getString(R.string.download),
                    downloadDrawable
                ),
                AlertListItem(
                    view.context.getString(R.string.addToQueue),
                    addToQueueDrawable
                ),
                AlertListItem(
                    view.context.getString(R.string.delete),
                    deleteDrawable
                ),
                AlertListItem(
                    view.context.getString(R.string.shareAlbum),
                    shareDrawable
                ),
                AlertListItem(
                    view.context.getString(R.string.openAlbumInApp),
                    openInAppDrawable
                )
            )

            val id = data[listViewPosition]

            SongDatabaseHelper(view.context).getSong(view.context, id)?.let { song ->
                displayAlertDialogList(
                    view.context,
                    AlertListHeaderItem(
                        song.shownTitle,
                        song.albumId
                    ),
                    itemList
                ) { _, item ->

                    when (item.itemText) {
                        view.context.getString(R.string.download) -> {
                            addDownloadSong(view.context, song.songId) {}
                        }
                        view.context.getString(R.string.addToQueue) -> {
                            prioritySongQueue.add(id)
                        }
                        view.context.getString(R.string.delete) -> {
                            deletePlaylistSong(
                                view,
                                id,
                                playlistId,
                                listViewPosition + 1,
                                data.size
                            )
                        }
                        view.context.getString(R.string.shareAlbum) -> {

                            openAlbum(view, song.mcAlbumId, true)

                        }
                        view.context.getString(R.string.openAlbumInApp) -> {

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

        override fun bindView(item: CatalogItem, payloads: List<Any>) {
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

                    override fun setTag(target: Target) {
                        coverImageView.tag = target
                    }
                }, song.albumId, true)

                var downloadStatus =
                    downloadDrawable.toUri()

                titleDownloadButton.setImageURI(downloadStatus)
                
                preDownloadCallbacks[song.songId] = {
                    titleDownloadButton.setImageURI(downloadingDrawable.toUri())
                }

                BackgroundAsync({
                    downloadStatus = song.downloadStatus
                }, {
                    titleDownloadButton.setImageURI(downloadStatus)
                }).execute()
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