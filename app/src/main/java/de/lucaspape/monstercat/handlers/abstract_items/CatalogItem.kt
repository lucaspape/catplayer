package de.lucaspape.monstercat.handlers.abstract_items

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

open class CatalogItem(
    private var title: String?,
    private var artist: String?,
    private var cover: String?,
    private var titleDownloadStatus: String?
) : AbstractItem<CatalogItem.ViewHolder>() {
    override val type: Int = 101

    override val layoutRes: Int
        get() = R.layout.list_single

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<CatalogItem>(view) {
        val titleTextView = view.findViewById<TextView>(R.id.title)
        val artistTextView = view.findViewById<TextView>(R.id.artist)
        val titleMenuButton = view.findViewById<ImageButton>(R.id.titleMenuButton)
        val coverImageView = view.findViewById<ImageView>(R.id.cover)
        val titleDownloadStatusImageView = view.findViewById<ImageView>(R.id.titleDownloadStatus)

        override fun bindView(item: CatalogItem, payloads: MutableList<Any>) {
            titleTextView.text = item.title
            artistTextView.text = item.artist
            coverImageView.setImageURI(item.cover?.toUri())
            titleDownloadStatusImageView.setImageURI(item.titleDownloadStatus?.toUri())
        }

        override fun unbindView(item: CatalogItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
            titleDownloadStatusImageView.setImageURI(null)
        }
    }
}