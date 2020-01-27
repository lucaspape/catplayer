package de.lucaspape.monstercat.handlers.abstract_items

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import java.io.File

open class PlaylistItem(
    val playlistId: String
) : AbstractItem<PlaylistItem.ViewHolder>() {
    override val type: Int = 102

    override val layoutRes: Int
        get() = R.layout.list_single

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<PlaylistItem>(view) {
        private val titleTextView: TextView = view.findViewById(R.id.title)
        val titleMenuButton: ImageButton = view.findViewById(R.id.titleMenuButton)
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        private val titleDownloadStatusImageView: ImageView =
            view.findViewById(R.id.titleDownloadStatus)
        private val context = view.context

        override fun bindView(item: PlaylistItem, payloads: MutableList<Any>) {
            val playlistDatabaseHelper = PlaylistDatabaseHelper(context)

            val playlist = playlistDatabaseHelper.getPlaylist(item.playlistId)

            playlist?.let {
                titleTextView.text = playlist.playlistName
                coverImageView.setImageURI("".toUri())

                val playlistTracks =
                    PlaylistItemDatabaseHelper(context, playlist.playlistId).getAllData()

                var downloaded = true
                var streamDownloaded = true

                for (track in playlistTracks) {
                    val song = SongDatabaseHelper(context).getSong(context, track.songId)

                    song?.let {
                        if (!File(song.downloadLocation).exists()) {
                            downloaded = false
                        } else if (!File(song.streamDownloadLocation).exists()) {
                            streamDownloaded = false
                        }
                    }
                }

                val playlistDownloadStatus = when {
                    downloaded -> {
                        "android.resource://de.lucaspape.monstercat/drawable/ic_check_green_24dp"
                    }
                    streamDownloaded -> {
                        "android.resource://de.lucaspape.monstercat/drawable/ic_check_orange_24dp"
                    }
                    else -> {
                        "android.resource://de.lucaspape.monstercat/drawable/ic_empty_24dp"
                    }
                }

                titleDownloadStatusImageView.setImageURI(playlistDownloadStatus.toUri())
            }
        }

        override fun unbindView(item: PlaylistItem) {
            titleTextView.text = null
            coverImageView.setImageURI(null)
            titleDownloadStatusImageView.setImageURI(null)
        }
    }
}