package de.lucaspape.monstercat.ui.abstract_items.util

import android.view.View
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

open class HeaderTextItem(private val headerText:String): AbstractItem<HeaderTextItem.ViewHolder>(){
    override val type: Int = 104

    override val layoutRes: Int
        get() = R.layout.list_text_header

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<HeaderTextItem>(view) {
        private val playlistNameTextView: TextView = view.findViewById(R.id.textHeader)

        override fun bindView(item: HeaderTextItem, payloads: List<Any>) {
            playlistNameTextView.text = item.headerText
        }

        override fun unbindView(item: HeaderTextItem) {
            playlistNameTextView.text = ""
        }
    }
}