package de.lucaspape.monstercat.ui.abstract_items.content

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.objects.Filter
import de.lucaspape.monstercat.ui.deleteDrawable
import de.lucaspape.monstercat.ui.emptyDrawable

class FilterItem(val filter:Filter): AbstractItem<FilterItem.ViewHolder>()  {
    override val type: Int = 1010

    override val layoutRes: Int
        get() = R.layout.list_single

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<FilterItem>(view) {
        private val titleTextView: TextView = view.findViewById(R.id.title)
        private val artistTextView: TextView = view.findViewById(R.id.artist)
        val titleMenuButton: ImageButton = view.findViewById(R.id.titleMenuButton)
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        private val titleDownloadButton: ImageButton =
            view.findViewById(R.id.titleDownloadButton)

        override fun bindView(item: FilterItem, payloads: List<Any>) {
            titleTextView.text = item.filter.filter
            artistTextView.text = item.filter.filterType

            titleMenuButton.setImageURI(deleteDrawable.toUri())
            titleDownloadButton.setImageURI(emptyDrawable.toUri())
        }

        override fun unbindView(item: FilterItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
            titleDownloadButton.setImageURI(null)
        }
    }
}