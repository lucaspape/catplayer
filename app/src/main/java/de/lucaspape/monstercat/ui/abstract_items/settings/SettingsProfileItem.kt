package de.lucaspape.monstercat.ui.abstract_items.settings

import android.view.View
import android.widget.Button
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

class SettingsProfileItem(val username: String, val onLogout: () -> Unit) :
    AbstractItem<SettingsProfileItem.ViewHolder>() {
    override val type: Int = 3004

    override val layoutRes: Int
        get() = R.layout.settings_user_profile

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(val view: View) : FastAdapter.ViewHolder<SettingsProfileItem>(view) {
        private val usernameTextView: TextView = view.findViewById(R.id.settings_username_textview)
        val button: Button = view.findViewById(R.id.settings_logout_button)

        override fun bindView(item: SettingsProfileItem, payloads: List<Any>) {
            usernameTextView.text = item.username
        }

        override fun unbindView(item: SettingsProfileItem) {
        }
    }
}