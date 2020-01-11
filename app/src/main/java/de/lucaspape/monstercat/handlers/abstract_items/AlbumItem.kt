package de.lucaspape.monstercat.handlers.abstract_items

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R

open class AlbumItem(private var title:String?, private var artist:String?, private var cover:String?) : AbstractItem<AlbumItem.ViewHolder>(){
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

        override fun bindView(item: AlbumItem, payloads: MutableList<Any>) {
            titleTextView.text = item.title
            artistTextView.text = item.artist
            coverImageView.setImageURI(item.cover?.toUri())
        }

        override fun unbindView(item: AlbumItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
        }
    }
}