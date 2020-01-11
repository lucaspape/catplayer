package de.lucaspape.monstercat.handlers.abstract_items

import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

open class ProgressItem : AbstractItem<ProgressItem.ViewHolder>() {
    override val type: Int = 103

    override val layoutRes: Int
        get() = R.layout.list_single

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ProgressItem>(view) {

        override fun bindView(item: ProgressItem, payloads: MutableList<Any>) {

        }

        override fun unbindView(item: ProgressItem) {

        }
    }
}