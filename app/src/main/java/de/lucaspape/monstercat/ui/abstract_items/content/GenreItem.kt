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
import de.lucaspape.monstercat.core.database.helper.GenreDatabaseHelper
import de.lucaspape.monstercat.core.download.ImageReceiverInterface
import de.lucaspape.monstercat.core.download.downloadCoverIntoImageReceiver

open class GenreItem(
    val genreId: String
) : AbstractItem<GenreItem.ViewHolder>() {

    override val type: Int = 190

    override val layoutRes = R.layout.list_album_horizontal

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<GenreItem>(view) {
        private val titleTextView: TextView = view.findViewById(R.id.albumTitle)
        private val artistTextView: TextView = view.findViewById(R.id.albumArtist)
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        private val context = view.context

        private var genreId = ""

        override fun bindView(item: GenreItem, payloads: List<Any>) {
            val genreDatabaseHelper = GenreDatabaseHelper(context)

            val genre = genreDatabaseHelper.getGenre(item.genreId)

            genre?.let {
                genreId = genre.genreId

                titleTextView.text = genre.name

                downloadCoverIntoImageReceiver(context, object : ImageReceiverInterface {
                    override fun setBitmap(id: String, bitmap: Bitmap?) {
                        if (id == "") {
                            coverImageView.setImageBitmap(bitmap)
                        }
                    }

                    override fun setDrawable(id: String, drawable: Drawable?) {
                        if (id == "") {
                            coverImageView.setImageDrawable(drawable)
                        }
                    }

                    override fun setTag(target: Target) {
                        coverImageView.tag = target
                    }
                }, "", false)
            }
        }

        override fun unbindView(item: GenreItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
        }
    }
}