package de.lucaspape.monstercat.ui.abstract_items.util

import android.view.View
import android.widget.ProgressBar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

open class ProgressItem : AbstractItem<ProgressItem.ViewHolder>() {
    override val type: Int = 4002

    override val layoutRes: Int
        get() = R.layout.list_loading

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ProgressItem>(view) {
        private val loadingBar = view.findViewById<ProgressBar>(R.id.loadingBar)

        override fun bindView(item: ProgressItem, payloads: List<Any>) {
            loadingBar.visibility = View.VISIBLE
        }

        override fun unbindView(item: ProgressItem) {
            loadingBar.visibility = View.GONE
        }
    }
}