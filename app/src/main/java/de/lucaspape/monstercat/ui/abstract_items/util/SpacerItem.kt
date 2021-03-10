package de.lucaspape.monstercat.ui.abstract_items.util

import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

open class SpacerItem() :
    AbstractItem<SpacerItem.ViewHolder>() {
    override val type: Int = 4003

    override val layoutRes: Int
        get() = R.layout.list_spacer

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<SpacerItem>(view) {

        override fun bindView(item: SpacerItem, payloads: List<Any>) {
        }

        override fun unbindView(item: SpacerItem) {
        }
    }
}