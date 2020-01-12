package de.lucaspape.monstercat.handlers.abstract_items

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

open class PlaylistItem(
    private var name:String?,
    private var cover: String?,
    private var titleDownloadStatus: String?
) : AbstractItem<PlaylistItem.ViewHolder>() {
    override val type: Int = 102

    override val layoutRes: Int
        get() = R.layout.list_single

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<PlaylistItem>(view) {
        private val titleTextView: TextView = view.findViewById(R.id.title)
        val titleMenuButton:ImageButton = view.findViewById(R.id.titleMenuButton)
        private val coverImageView:ImageView = view.findViewById(R.id.cover)
        private val titleDownloadStatusImageView:ImageView = view.findViewById(R.id.titleDownloadStatus)

        override fun bindView(item: PlaylistItem, payloads: MutableList<Any>) {
            titleTextView.text = item.name
            coverImageView.setImageURI(item.cover?.toUri())
            titleDownloadStatusImageView.setImageURI(item.titleDownloadStatus?.toUri())
        }

        override fun unbindView(item: PlaylistItem) {
            titleTextView.text = null
            coverImageView.setImageURI(null)
            titleDownloadStatusImageView.setImageURI(null)
        }
    }
}