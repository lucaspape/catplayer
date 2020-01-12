package de.lucaspape.monstercat.handlers.abstract_items

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.okhttp.OkHttpClient
import com.squareup.picasso.OkHttpDownloader
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
        private val titleTextView:TextView = view.findViewById(R.id.albumTitle)
        private val artistTextView:TextView = view.findViewById(R.id.albumArtist)
        private val coverImageView:ImageView = view.findViewById(R.id.cover)
        private val context = view.context

        override fun bindView(item: AlbumItem, payloads: MutableList<Any>) {
            titleTextView.text = item.title
            artistTextView.text = item.artist

            val settings = Settings(context)

            Picasso.Builder(context)
                .downloader(OkHttpDownloader(context, Long.MAX_VALUE))
                .build()
                .load(item.coverUrl + "?image_width=" + settings.getSetting("primaryResolution"))
                .placeholder(Drawable.createFromPath(context.dataDir.toString() + "/fallback.jpg"))
                .into(coverImageView)
        }

        override fun unbindView(item: AlbumItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
        }
    }
}