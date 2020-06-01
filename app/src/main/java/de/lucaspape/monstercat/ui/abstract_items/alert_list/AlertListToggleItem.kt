package de.lucaspape.monstercat.ui.abstract_items.alert_list

import android.view.View
import android.widget.ImageView
import android.widget.Switch
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

class AlertListToggleItem(val itemText:String, val itemDrawable:String?, val enabled:Boolean): AbstractItem<AlertListToggleItem.ViewHolder>() {
    override val type: Int = 110

    override val layoutRes: Int
        get() = R.layout.alert_list_toggle_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view:View): FastAdapter.ViewHolder<AlertListToggleItem>(view){
        val alertItemSwitch: Switch = view.findViewById(R.id.alertItemSwitch)
        private val alertItemImage: ImageView = view.findViewById(R.id.alertItemImage)

        override fun bindView(item: AlertListToggleItem, payloads: List<Any>) {
            alertItemSwitch.text = item.itemText
            alertItemSwitch.isChecked = item.enabled
            alertItemImage.setImageURI(item.itemDrawable?.toUri())
        }

        override fun unbindView(item: AlertListToggleItem) {
            alertItemSwitch.text = ""
            alertItemImage.setImageURI("".toUri())
        }
    }
}