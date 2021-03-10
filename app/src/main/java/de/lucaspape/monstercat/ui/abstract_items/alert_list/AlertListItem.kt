package de.lucaspape.monstercat.ui.abstract_items.alert_list

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

class AlertListItem(val itemText: String, val itemDrawable: String?) :
    AbstractItem<AlertListItem.ViewHolder>() {
    override val type: Int = 2002

    override val layoutRes: Int
        get() = R.layout.alert_list_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<AlertListItem>(view) {
        private val alertItemText: TextView = view.findViewById(R.id.alertItemText)
        private val alertItemImage: ImageView = view.findViewById(R.id.alertItemImage)

        override fun bindView(item: AlertListItem, payloads: List<Any>) {
            alertItemText.text = item.itemText
            alertItemImage.setImageURI(item.itemDrawable?.toUri())
        }

        override fun unbindView(item: AlertListItem) {
            alertItemText.text = ""
            alertItemImage.setImageURI("".toUri())
        }

    }
}