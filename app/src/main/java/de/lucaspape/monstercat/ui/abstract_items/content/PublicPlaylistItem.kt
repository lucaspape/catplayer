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

    override val layoutRes = R.layout.list_album_horizontal

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<PublicPlaylistItem>(view) {
        private val titleTextView: TextView = view.findViewById(R.id.albumTitle)
        private val artistTextView: TextView = view.findViewById(R.id.albumArtist)
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        private val context = view.context

        private var publicPlaylistid = ""

        override fun bindView(item: PublicPlaylistItem, payloads: List<Any>) {
            val publicPlaylistDatabaseHelper = PublicPlaylistDatabaseHelper(context)

            val playlist = publicPlaylistDatabaseHelper.getPlaylist(item.publicPlaylistId)

            //TODO cover image

            playlist?.let {
                publicPlaylistid = playlist.playlistId

                titleTextView.text = playlist.playlistName

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
                },  false, publicPlaylistid, "https://connect.monstercat.com/v2/playlist/$publicPlaylistid/tile")
            }
        }

        override fun unbindView(item: PublicPlaylistItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
        }
    }
}