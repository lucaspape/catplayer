package de.lucaspape.monstercat.ui.abstract_items.content

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Target
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.MoodDatabaseHelper
import de.lucaspape.monstercat.core.download.ImageReceiverInterface
import de.lucaspape.monstercat.core.download.downloadImageUrlIntoImageReceiver

open class MoodItem(
    val moodId: String
) : AbstractItem<MoodItem.ViewHolder>() {

    override val type: Int = 1005

    override val layoutRes = R.layout.list_tile

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<MoodItem>(view) {
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        private val context = view.context

        private var moodId = ""

        override fun bindView(item: MoodItem, payloads: List<Any>) {
            val moodDatabaseHelper = MoodDatabaseHelper(context)

            val mood = moodDatabaseHelper.getMood(item.moodId)

            mood?.let {
                moodId = mood.moodId

                downloadImageUrlIntoImageReceiver(context, object : ImageReceiverInterface {
                    override fun setBitmap(id: String, bitmap: Bitmap?) {
                        if (id == moodId) {
                            coverImageView.setImageBitmap(bitmap)
                        }
                    }

                    override fun setDrawable(id: String, drawable: Drawable?) {
                        if (id == moodId) {
                            coverImageView.setImageDrawable(drawable)
                        }
                    }

                    override fun setTag(target: Target) {
                        coverImageView.tag = target
                    }
                }, false, mood.moodId, mood.coverUrl)
            }
        }

        override fun unbindView(item: MoodItem) {
            coverImageView.setImageURI(null)
        }
    }
}