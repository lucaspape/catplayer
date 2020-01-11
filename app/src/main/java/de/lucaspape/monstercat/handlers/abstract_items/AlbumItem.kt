package de.lucaspape.monstercat.handlers.abstract_items

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.Settings

open class AlbumItem(private var title:String?, private var artist:String?, private var coverUrl:String?) : AbstractItem<AlbumItem.ViewHolder>(){
    override val type: Int = 100

    override val layoutRes: Int
        get() = R.layout.list_album_view

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view:View) : FastAdapter.ViewHolder<AlbumItem>(view){
        val titleTextView = view.findViewById<TextView>(R.id.albumTitle)
        val artistTextView = view.findViewById<TextView>(R.id.albumArtist)
        val coverImageView = view.findViewById<ImageView>(R.id.cover)
        val context = view.context

        override fun bindView(item: AlbumItem, payloads: MutableList<Any>) {
            titleTextView.text = item.title
            artistTextView.text = item.artist

            val settings = Settings(context)
            Picasso.with(context).load(item.coverUrl + "?image_width=" + settings.getSetting("primaryResolution")).into(coverImageView)
        }

        override fun unbindView(item: AlbumItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
        }
    }
}