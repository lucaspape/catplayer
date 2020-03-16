package de.lucaspape.monstercat.handlers.abstract_items

import android.app.AlertDialog
import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.activities.downloadDrawable
import de.lucaspape.monstercat.activities.offlineDrawable
import de.lucaspape.monstercat.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.database.helper.PlaylistItemDatabaseHelper
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.request.async.BackgroundAsync
import de.lucaspape.monstercat.handlers.deletePlaylist
import de.lucaspape.monstercat.handlers.downloadPlaylist
import de.lucaspape.monstercat.handlers.playPlaylistNext
import java.io.File

open class PlaylistItem(
    val playlistId: String
) : AbstractItem<PlaylistItem.ViewHolder>() {

    companion object {
        @JvmStatic
        fun showContextMenu(
            context: Context,
            data: ArrayList<String>,
            listViewPosition: Int
        ) {
            val menuItems = arrayOf(
                context.getString(R.string.download),
                context.getString(R.string.addToQueue),
                context.getString(R.string.delete)
            )

            val alertDialogBuilder = AlertDialog.Builder(context)
            alertDialogBuilder.setTitle("")
            alertDialogBuilder.setItems(menuItems) { _, which ->
                val id = data[listViewPosition]

                when (menuItems[which]) {
                    context.getString(R.string.download) -> {
                        downloadPlaylist(
                            context,
                            id
                        ) {}
                    }
                    context.getString(R.string.addToQueue) -> {
                        playPlaylistNext(context, id)
                    }
                    context.getString(R.string.delete) -> {
                        deletePlaylist(context, id)
                    }
                }
            }

            alertDialogBuilder.create().show()
        }
    }

    fun getDownloadStatus(context: Context): String {
        val playlistTracks =
            PlaylistItemDatabaseHelper(context, playlistId).getAllData()

        var downloaded = true

        for (track in playlistTracks) {
            val song = SongDatabaseHelper(context).getSong(context, track.songId)

            song?.let {
                if (!File(song.downloadLocation).exists()) {
                    downloaded = false
                }
            }
        }

        return when {
            downloaded -> {
                offlineDrawable
            }
            else -> {
                downloadDrawable
            }
        }
    }

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
        val titleDownloadButton: ImageButton =
            view.findViewById(R.id.titleDownloadButton)
        private val context = view.context

        override fun bindView(item: PlaylistItem, payloads: MutableList<Any>) {
            val playlistDatabaseHelper = PlaylistDatabaseHelper(context)

            val playlist = playlistDatabaseHelper.getPlaylist(item.playlistId)

            playlist?.let {
                titleTextView.text = playlist.playlistName
                coverImageView.setImageURI("".toUri())

                var downloadStatus = downloadDrawable.toUri()

                titleDownloadButton.setImageURI(downloadStatus)

                BackgroundAsync({
                    downloadStatus = item.getDownloadStatus(context).toUri()
                }, {
                    titleDownloadButton.setImageURI(downloadStatus)
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }

        override fun unbindView(item: PlaylistItem) {
            titleTextView.text = null
            coverImageView.setImageURI(null)
            titleDownloadButton.setImageURI(null)
        }
    }
}