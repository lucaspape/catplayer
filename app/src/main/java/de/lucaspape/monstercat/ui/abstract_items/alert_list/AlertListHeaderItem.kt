package de.lucaspape.monstercat.ui.abstract_items.alert_list

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Target
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.download.ImageReceiverInterface
import de.lucaspape.monstercat.core.download.downloadCoverIntoImageReceiver

open class AlertListHeaderItem(private val headerText: String, private val albumId: String) :
    AbstractItem<AlertListHeaderItem.ViewHolder>() {
    override val type: Int = 106

    override val layoutRes: Int
        get() = R.layout.alert_list_header_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<AlertListHeaderItem>(view) {
        private val headerItemImage: ImageView = view.findViewById(R.id.headerItemImage)
        private val headerItemText: TextView = view.findViewById(R.id.headerItemText)
        private val context = view.context

        override fun bindView(item: AlertListHeaderItem, payloads: List<Any>) {
            headerItemText.text = item.headerText

            downloadCoverIntoImageReceiver(context, object : ImageReceiverInterface {
                override fun setBitmap(id: String, bitmap: Bitmap?) {
                    if (id == item.albumId) {
                        headerItemImage.setImageBitmap(bitmap)
                    }
                }

                override fun setDrawable(id: String, drawable: Drawable?) {
                    if (id == item.albumId) {
                        headerItemImage.setImageDrawable(drawable)
                    }
                }

                override fun setTag(target: Target) {
                    headerItemImage.tag = target
                }
            }, item.albumId, false)
        }

        override fun unbindView(item: AlertListHeaderItem) {
        }
    }
}