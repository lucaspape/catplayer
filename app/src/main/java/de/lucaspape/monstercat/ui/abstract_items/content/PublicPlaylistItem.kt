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
import de.lucaspape.monstercat.core.database.helper.PublicPlaylistDatabaseHelper
import de.lucaspape.monstercat.core.download.ImageReceiverInterface
import de.lucaspape.monstercat.core.download.downloadCoverIntoImageReceiver
import de.lucaspape.monstercat.core.download.downloadImageUrlIntoImageReceiver

open class PublicPlaylistItem(
    val publicPlaylistId: String
) : AbstractItem<PublicPlaylistItem.ViewHolder>() {

    override val type: Int = 1009

    override val layoutRes = R.layout.list_tile

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<PublicPlaylistItem>(view) {
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        private val context = view.context

        private var publicPlaylistid = ""

        override fun bindView(item: PublicPlaylistItem, payloads: List<Any>) {
            val publicPlaylistDatabaseHelper = PublicPlaylistDatabaseHelper(context)

            val playlist = publicPlaylistDatabaseHelper.getPlaylist(item.publicPlaylistId)

            playlist?.let {
                publicPlaylistid = playlist.playlistId

                downloadImageUrlIntoImageReceiver(context, object : ImageReceiverInterface {
                    override fun setBitmap(id: String, bitmap: Bitmap?) {
                        if (id == publicPlaylistid) {
                            coverImageView.setImageBitmap(bitmap)
                        }
                    }

                    override fun setDrawable(id: String, drawable: Drawable?) {
                        if (id == publicPlaylistid) {
                            coverImageView.setImageDrawable(drawable)
                        }
                    }

                    override fun setTag(target: Target) {
                        coverImageView.tag = target
                    }
                },  false, publicPlaylistid, context.getString(R.string.playlistUrl) + "$publicPlaylistid/tile")
            }
        }

        override fun unbindView(item: PublicPlaylistItem) {
            coverImageView.setImageURI(null)
        }
    }
}