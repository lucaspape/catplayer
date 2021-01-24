package de.lucaspape.monstercat.ui.abstract_items.settings

import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

class SettingsSeekBarItem(
    private val title: String,
    private val max: Int,
    private val value: Int,
    private val shownStart: Int,
    private val onChange: (value: Int, shownValueView: TextView) -> Unit
) : AbstractItem<SettingsSeekBarItem.ViewHolder>() {
    override val type: Int = 125

    override val layoutRes: Int
        get() = R.layout.settings_seekbar_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(val view: View) : FastAdapter.ViewHolder<SettingsSeekBarItem>(view) {
        private val seekBarLabel = view.findViewById<TextView>(R.id.seekbar_label)
        val seekBarShownValue: TextView = view.findViewById<TextView>(R.id.seekbar_shown_value)
        private val seekBar: SeekBar = view.findViewById<SeekBar>(R.id.settings_seekbar)

        override fun bindView(item: SettingsSeekBarItem, payloads: List<Any>) {
            seekBarLabel.text = item.title
            seekBar.max = item.max
            seekBar.progress = item.value
            seekBarShownValue.text = item.shownStart.toString()

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        item.onChange(progress, seekBarShownValue)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }

            })
        }

        override fun unbindView(item: SettingsSeekBarItem) {
            seekBarLabel.text = ""
        }

    }
}