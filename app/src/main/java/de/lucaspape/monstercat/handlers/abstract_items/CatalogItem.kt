package de.lucaspape.monstercat.handlers.abstract_items

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.download.downloadCoverIntoAbstractItem
import de.lucaspape.monstercat.handlers.*
import de.lucaspape.monstercat.handlers.addSongToPlaylist
import de.lucaspape.monstercat.handlers.deletePlaylistSong
import de.lucaspape.monstercat.handlers.openAlbum
import de.lucaspape.monstercat.handlers.playSongFromId

open class CatalogItem(
    val songId: String
) : AbstractItem<CatalogItem.ViewHolder>(){

    companion object {
        @JvmStatic
        fun showContextMenu(
            context: Context,
            contentList: ArrayList<String>,
            listViewPosition: Int
        ) {
            val menuItems =
                arrayOf(
                    context.getString(R.string.download),
                    context.getString(R.string.playNext),
                    context.getString(R.string.addToPlaylist),
                    context.getString(R.string.shareAlbum),
                    context.getString(R.string.openAlbumInApp)
                )

            val alertDialogBuilder = AlertDialog.Builder(context)
            alertDialogBuilder.setTitle("")
            alertDialogBuilder.setItems(menuItems) { _, which ->
                val id = contentList[listViewPosition]
                val songDatabaseHelper =
                    SongDatabaseHelper(context)
                val song = songDatabaseHelper.getSong(context, id)

                song?.let {
                    when (menuItems[which]) {
                        context.getString(R.string.download) -> addDownloadSong(
                            context,
                            song.songId
                        ) {}
                        context.getString(R.string.playNext) -> playSongFromId(
                            id,
                            false,
                            priority = true
                        )
                        context.getString(R.string.addToPlaylist) -> addSongToPlaylist(
                            context,
                            song
                        )
                        context.getString(R.string.shareAlbum) -> openAlbum(
                            context,
                            song.mcAlbumId,
                            true
                        )
                        context.getString(R.string.openAlbumInApp) -> openAlbum(
                            context,
                            song.mcAlbumId,
                            false
                        )
                    }
                }
            }

            alertDialogBuilder.create().show()
        }

        @JvmStatic
        fun showContextMenuPlaylist(
            context: Context,
            data: ArrayList<String>,
            listViewPosition: Int
        ) {
            val menuItems =
                arrayOf(
                    context.getString(R.string.download),
                    context.getString(R.string.playNext),
                    context.getString(R.string.delete),
                    context.getString(R.string.shareAlbum),
                    context.getString(R.string.openAlbumInApp)
                )

            val alertDialogBuilder = AlertDialog.Builder(context)
            alertDialogBuilder.setTitle("")
            alertDialogBuilder.setItems(menuItems) { _, which ->
                val id = data[listViewPosition]
                when (menuItems[which]) {
                    context.getString(R.string.download) -> {
                        val songDatabaseHelper =
                            SongDatabaseHelper(context)
                        val song =
                            songDatabaseHelper.getSong(context, id)

                        if (song != null) {
                            addDownloadSong(context, song.songId) {}
                        }
                    }
                    context.getString(R.string.playNext) -> {
                        playSongFromId(
                            id,
                            false,
                            priority = true
                        )
                    }
                    context.getString(R.string.delete) -> {
                        val songDatabaseHelper =
                            SongDatabaseHelper(context)
                        val song =
                            songDatabaseHelper.getSong(context, id)

                        if (song != null) {
                            PlaylistHandler.currentPlaylistId?.let { playlistId ->
                                deletePlaylistSong(
                                    context,
                                    song,
                                    playlistId,
                                    listViewPosition + 1,
                                    data.size
                                )
                            }
                        }

                    }
                    context.getString(R.string.shareAlbum) -> {
                        val songDatabaseHelper =
                            SongDatabaseHelper(context)
                        val song =
                            songDatabaseHelper.getSong(context, id)

                        if (song != null) {
                            openAlbum(context, song.mcAlbumId, true)
                        }
                    }
                    context.getString(R.string.openAlbumInApp) -> {
                        val songDatabaseHelper =
                            SongDatabaseHelper(context)
                        val song =
                            songDatabaseHelper.getSong(context, id)

                        if (song != null) {
                            openAlbum(context, song.mcAlbumId, false)
                        }

                    }
                }
            }

            alertDialogBuilder.create().show()
        }
    }

    override val type: Int = 101

    override val layoutRes: Int
        get() = R.layout.list_single

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<CatalogItem>(view), ViewHolderInterface {
        private val titleTextView: TextView = view.findViewById(R.id.title)
        private val artistTextView: TextView = view.findViewById(R.id.artist)
        val titleMenuButton: ImageButton = view.findViewById(R.id.titleMenuButton)
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        val titleDownloadButton: ImageButton =
            view.findViewById(R.id.titleDownloadButton)
        private val context = view.context

        private var albumId = ""

        override fun bindView(item: CatalogItem, payloads: MutableList<Any>) {
            val songDatabaseHelper = SongDatabaseHelper(context)
            val song = songDatabaseHelper.getSong(context, item.songId)

            song?.let {
                albumId = song.albumId

                val shownTitle = "${song.title} ${song.version}"

                titleTextView.text = shownTitle
                artistTextView.text = song.artist

                //downloadCoverIntoImageView(context, coverImageView, song.albumId, true)
                downloadCoverIntoAbstractItem(context, this, song.albumId, true)

                titleDownloadButton.setImageURI(song.getSongDownloadStatus().toUri())
            }
        }

        override fun unbindView(item: CatalogItem) {
            titleTextView.text = null
            artistTextView.text = null
            coverImageView.setImageURI(null)
            titleDownloadButton.setImageURI(null)
        }

        override fun setCoverBitmap(albumId: String, bitmap: Bitmap?) {
            if(albumId == this.albumId){
                coverImageView.setImageBitmap(bitmap)
            }
        }

        override fun setCoverDrawable(albumId: String, drawable: Drawable?) {
            if(albumId == this.albumId){
                coverImageView.setImageDrawable(drawable)
            }
        }
    }
}