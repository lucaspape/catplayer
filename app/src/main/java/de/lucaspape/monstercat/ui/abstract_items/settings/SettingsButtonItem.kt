package de.lucaspape.monstercat.ui.abstract_items.settings

import android.view.View
import android.widget.Button
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

class SettingsButtonItem(private val buttonText: String, val onClick: () -> Unit) :
    AbstractItem<SettingsButtonItem.ViewHolder>() {
    override val type: Int = 121

    override val layoutRes: Int
        get() = R.layout.settings_button_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<SettingsButtonItem>(view) {
        val button: Button = view.findViewById<Button>(R.id.settingsButton)

        override fun bindView(item: SettingsButtonItem, payloads: List<Any>) {
            button.text = item.buttonText
        }

        override fun unbindView(item: SettingsButtonItem) {
            button.text = ""
        }
    }
}