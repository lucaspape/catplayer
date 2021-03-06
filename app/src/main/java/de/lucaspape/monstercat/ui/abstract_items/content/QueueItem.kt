package de.lucaspape.monstercat.ui.abstract_items.content

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Target
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.download.downloadCoverIntoImageReceiver
import de.lucaspape.monstercat.core.download.ImageReceiverInterface
import de.lucaspape.monstercat.ui.*

open class QueueItem(
    val songId: String
) : AbstractItem<QueueItem.ViewHolder>() {
    override val type: Int = 1007

    override val layoutRes: Int
        get() = R.layout.list_single

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<QueueItem>(view) {
        private val titleTextView: TextView = view.findViewById(R.id.title)
        private val artistTextView: TextView = view.findViewById(R.id.artist)
        val titleMenuButton: ImageButton = view.findViewById(R.id.titleMenuButton)
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        private val titleDownloadButton: ImageButton =
            view.findViewById(R.id.titleDownloadButton)
        private val context = view.context

        private var albumId = ""

        override fun bindView(item: QueueItem, payloads: List<Any>) {
            val songDatabaseHelper = SongDatabaseHelper(context)
            val song = songDatabaseHelper.getSong(item.songId)

            song?.let {
                albumId = song.albumId

                val shownTitle = song.shownTitle

                titleTextView.text = shownTitle
                artistTextView.text = song.artist

                downloadCoverIntoImageReceiver(context, object : ImageReceiverInterface {
                    override fun setBitmap(id: String, bitmap: Bitmap?) {
                        if (id == albumId) {
                            coverImageView.setImageBitmap(bitmap)
                        }
                    }

                    override fun setDrawable(id: String, drawable: Drawable?) {
                        if (id == albumId) {
                            coverImageView.setImageDrawable(drawable)
                        }
                    }

                    override fun setTag(target: Target) {
                        coverImageView.tag = target
                    }
                }, song.albumId, true)

                titleMenuButton.setImageURI(deleteDrawable.toUri())
                titleDownloadButton.setImageURI(emptyDrawable.toUri())
            }
        }

        override fun unbindView(item: QueueItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
            titleDownloadButton.setImageURI(null)
        }
    }
}