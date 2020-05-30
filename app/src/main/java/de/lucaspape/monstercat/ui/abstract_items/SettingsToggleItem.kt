package de.lucaspape.monstercat.ui.abstract_items

import android.view.View
import android.widget.Switch
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.util.Settings

class SettingsToggleItem(
    val setting: String,
    val trueValue: Any,
    val itemText: String,
    val onSwitchChange: (setting: String, value: Boolean, switch: Switch) -> Boolean
) : AbstractItem<SettingsToggleItem.ViewHolder>() {
    override val type: Int = 120

    override val layoutRes: Int
        get() = R.layout.alert_list_toggle_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<SettingsToggleItem>(view) {
        val alertItemSwitch: Switch = view.findViewById(R.id.alertItemSwitch)
        private val context = view.context

        override fun bindView(item: SettingsToggleItem, payloads: List<Any>) {
            alertItemSwitch.text = item.itemText

            if (item.trueValue is Boolean) {
                alertItemSwitch.isChecked =
                    Settings.getSettings(context).getBoolean(item.setting) == item.trueValue
            } else if (item.trueValue is String) {
                alertItemSwitch.isChecked =
                    Settings.getSettings(context).getString(item.setting) == item.trueValue
            }
        }

        override fun unbindView(item: SettingsToggleItem) {
            alertItemSwitch.text = ""
        }
    }
}