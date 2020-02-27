package de.lucaspape.monstercat.handlers.abstract_items

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.AlbumDatabaseHelper
import de.lucaspape.monstercat.download.downloadCoverIntoImageView
import de.lucaspape.monstercat.handlers.downloadAlbum
import de.lucaspape.monstercat.handlers.openAlbum
import de.lucaspape.monstercat.handlers.playAlbumNext

open class AlbumItem(
    val albumId: String
) : AbstractItem<AlbumItem.ViewHolder>() {

    companion object {
        @JvmStatic
        fun showContextMenu(
            context: Context,
            contentList: ArrayList<String>,
            listViewPosition: Int
        ) {
            val menuItems: Array<String> = arrayOf(
                context.getString(R.string.downloadAlbum),
                context.getString(R.string.playAlbumNext),
                context.getString(R.string.shareAlbum),
                context.getString(R.string.openAlbumInApp)
            )

            val alertDialogBuilder = AlertDialog.Builder(context)
            alertDialogBuilder.setTitle("")
            alertDialogBuilder.setItems(menuItems) { _, which ->
                val id = contentList[listViewPosition]

                when (menuItems[which]) {
                    context.getString(R.string.downloadAlbum) -> downloadAlbum(
                        context,
                        id
                    )
                    context.getString(R.string.playAlbumNext) -> playAlbumNext(
                        context,
                        id
                    )
                    context.getString(R.string.shareAlbum) -> openAlbum(context, id, true)
                    context.getString(R.string.openAlbumInApp) -> openAlbum(context, id, false)
                }
            }

            alertDialogBuilder.show()
        }
    }

    override val type: Int = 100

    override val layoutRes: Int
        get() = R.layout.list_album_view

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

        override fun bindView(item: AlbumItem, payloads: MutableList<Any>) {
            val albumDatabaseHelper = AlbumDatabaseHelper(context)

            val album = albumDatabaseHelper.getAlbum(item.albumId)

            album?.let {
                titleTextView.text = album.title
                artistTextView.text = album.artist
            }

            downloadCoverIntoImageView(context, coverImageView, item.albumId, false)
        }

        override fun unbindView(item: AlbumItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
        }
    }
}