package de.lucaspape.monstercat.handlers.abstract_items

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.download.downloadCoverIntoImageView

open class AlbumItem(
    val title: String,
    val artist: String,
    val mcID: String,
    val albumId: String
) : AbstractItem<AlbumItem.ViewHolder>() {
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
            titleTextView.text = item.title
            artistTextView.text = item.artist

            downloadCoverIntoImageView(context, coverImageView, item.albumId, false)
        }

        override fun unbindView(item: AlbumItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
        }
    }
}