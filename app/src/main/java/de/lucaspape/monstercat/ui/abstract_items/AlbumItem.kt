package de.lucaspape.monstercat.ui.abstract_items

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.activities.addToQueueDrawable
import de.lucaspape.monstercat.ui.activities.downloadDrawable
import de.lucaspape.monstercat.ui.activities.openInAppDrawable
import de.lucaspape.monstercat.ui.activities.shareDrawable
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.download.downloadCoverIntoImageReceiver
import de.lucaspape.monstercat.ui.handlers.downloadAlbum
import de.lucaspape.monstercat.ui.handlers.openAlbum
import de.lucaspape.monstercat.ui.handlers.playAlbumNext
import de.lucaspape.monstercat.download.ImageReceiverInterface
import de.lucaspape.monstercat.util.displayAlertDialogList

open class AlbumItem(
    val albumId: String
) : AbstractItem<AlbumItem.ViewHolder>() {

    companion object {
        @JvmStatic
        fun showContextMenu(
            view: View,
            contentList: ArrayList<String>,
            listViewPosition: Int
        ) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

            val itemList = arrayListOf(
                AlertListItem(view.context.getString(R.string.downloadAlbum), downloadDrawable),
                AlertListItem(view.context.getString(R.string.addAlbumToQueue), addToQueueDrawable),
                AlertListItem(view.context.getString(R.string.shareAlbum), shareDrawable),
                AlertListItem(view.context.getString(R.string.openAlbumInApp), openInAppDrawable)
            )

            val id = contentList[listViewPosition]

            AlbumDatabaseHelper(view.context).getAlbumFromMcId(id)?.let { album ->
                displayAlertDialogList(
                    view.context,
                    AlertListHeaderItem(album.title, album.albumId),
                    itemList
                ) { _, item ->
                    when (item.itemText) {
                        view.context.getString(R.string.downloadAlbum) -> downloadAlbum(
                            view,
                            id
                        )
                        view.context.getString(R.string.addAlbumToQueue) -> playAlbumNext(
                            view,
                            id
                        )
                        view.context.getString(R.string.shareAlbum) -> openAlbum(view, id, true)
                        view.context.getString(R.string.openAlbumInApp) -> openAlbum(view, id, false)
                    }
                }
            }
        }
    }

    override val type: Int = 100

    override val layoutRes: Int
        get() = R.layout.list_album

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<AlbumItem>(view) {
        private val titleTextView: TextView = view.findViewById(R.id.albumTitle)
        private val artistTextView: TextView = view.findViewById(R.id.albumArtist)
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        private val context = view.context

        private var albumId = ""

        override fun bindView(item: AlbumItem, payloads: MutableList<Any>) {
            val albumDatabaseHelper = AlbumDatabaseHelper(context)

            val album = albumDatabaseHelper.getAlbum(item.albumId)

            album?.let {
                albumId = album.albumId

                titleTextView.text = album.title
                artistTextView.text = album.artist
            }

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
            }, item.albumId, false)
        }

        override fun unbindView(item: AlbumItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
        }
    }
}