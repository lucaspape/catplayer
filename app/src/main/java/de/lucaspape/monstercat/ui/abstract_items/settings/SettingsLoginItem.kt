package de.lucaspape.monstercat.ui.abstract_items.settings

import android.view.View
import android.widget.Button
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

class SettingsLoginItem(val onLogin: (username: String, password: String) -> Unit) :
    AbstractItem<SettingsLoginItem.ViewHolder>() {
    override val type: Int = 122

    override val layoutRes: Int
        get() = R.layout.settings_login_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(val view: View) : FastAdapter.ViewHolder<SettingsLoginItem>(view) {
        val button: Button = view.findViewById(R.id.settings_add_account)

        override fun bindView(item: SettingsLoginItem, payloads: List<Any>) {
        }

        override fun unbindView(item: SettingsLoginItem) {
        }
    }
}