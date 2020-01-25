package de.lucaspape.monstercat.handlers.abstract_items

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.download.downloadCoverIntoImageView

open class CatalogItem(
    private var title: String?,
    private var version:String?,
    private var artist: String?,
    private var albumId:String?,
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
        private val titleTextView:TextView = view.findViewById(R.id.title)
        private val artistTextView:TextView = view.findViewById(R.id.artist)
        val titleMenuButton:ImageButton = view.findViewById(R.id.titleMenuButton)
        private val coverImageView:ImageView = view.findViewById(R.id.cover)
        private val titleDownloadStatusImageView:ImageView = view.findViewById(R.id.titleDownloadStatus)
        private val context = view.context

        override fun bindView(item: CatalogItem, payloads: MutableList<Any>) {
            titleTextView.text = "${item.title} ${item.version}"
            artistTextView.text = item.artist

            item.albumId?.let {
                downloadCoverIntoImageView(context, coverImageView, it, true)
            }

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