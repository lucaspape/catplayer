package de.lucaspape.monstercat.ui.abstract_items.settings

import android.view.View
import com.google.android.material.switchmaterial.SwitchMaterial
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.util.Settings

class SettingsToggleItem(
    val setting: String,
    val trueValue: Any,
    val itemText: String,
    val requiredApiFeature:String?,
    val onSwitchChange: (setting: String, value: Boolean, switch: SwitchMaterial) -> Boolean
) : AbstractItem<SettingsToggleItem.ViewHolder>() {
    override val type: Int = 120

    override val layoutRes: Int
        get() = R.layout.settings_switch_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<SettingsToggleItem>(view) {
        val alertItemSwitch: SwitchMaterial = view.findViewById(R.id.alertItemSwitch)
        private val context = view.context

        override fun bindView(item: SettingsToggleItem, payloads: List<Any>) {
            alertItemSwitch.text = item.itemText

            if(item.requiredApiFeature != null && Settings.getSettings(context).getBoolean(item.requiredApiFeature) != true){
                alertItemSwitch.isEnabled = false
                item.onSwitchChange(item.setting, false, alertItemSwitch)
            }else{
                alertItemSwitch.isEnabled = true
            }

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