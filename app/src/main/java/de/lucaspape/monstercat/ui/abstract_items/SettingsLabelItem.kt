package de.lucaspape.monstercat.ui.abstract_items

import android.view.View
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

class SettingsLabelItem(private val title: String) : AbstractItem<SettingsLabelItem.ViewHolder>() {
    override val type: Int = 126

    override val layoutRes: Int
        get() = R.layout.list_text_header

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<SettingsLabelItem>(view) {
        private val textView: TextView = view.findViewById(R.id.textHeader)

        override fun bindView(item: SettingsLabelItem, payloads: List<Any>) {
            textView.text = item.title
        }

        override fun unbindView(item: SettingsLabelItem) {
            textView.text = ""
        }
    }
}