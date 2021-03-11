package de.lucaspape.monstercat.ui.abstract_items.content

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Target
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.ItemDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.PlaylistDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.download.ImageReceiverInterface
import de.lucaspape.monstercat.core.download.downloadCoverIntoImageReceiver
import de.lucaspape.monstercat.ui.*
import de.lucaspape.monstercat.ui.abstract_items.alert_list.AlertListHeaderItem
import de.lucaspape.monstercat.ui.abstract_items.alert_list.AlertListItem
import de.lucaspape.monstercat.core.util.BackgroundAsync
import de.lucaspape.monstercat.ui.pages.util.deletePlaylist
import de.lucaspape.monstercat.ui.pages.util.downloadPlaylistAsync
import de.lucaspape.monstercat.ui.pages.util.openPlaylist
import de.lucaspape.monstercat.ui.pages.util.playPlaylistNextAsync
import de.lucaspape.monstercat.ui.pages.util.renamePlaylist
import de.lucaspape.monstercat.ui.pages.util.togglePlaylistPublicStateAsync
import java.io.File

open class PlaylistItem(
    val playlistId: String
) : AbstractItem<PlaylistItem.ViewHolder>() {

    companion object {
        @JvmStatic
        fun showContextMenu(
            view: View,
            data: ArrayList<String>,
            listViewPosition: Int,
            deletePlaylistCallback:() -> Unit
        ) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

            val id = data[listViewPosition]

            PlaylistDatabaseHelper(view.context).getPlaylist(id)?.let { playlist ->
                val itemList = arrayListOf(
                    AlertListItem(
                        view.context.getString(R.string.download),
                        downloadDrawable
                    ),
                    AlertListItem(
                        view.context.getString(R.string.addToQueue),
                        addToQueueDrawable
                    ),
                    AlertListItem(
                        view.context.getString(R.string.delete),
                        deleteDrawable
                    ),
                    AlertListItem(
                        view.context.getString(R.string.renamePlaylist),
                        editDrawable
                    ),
                    AlertListItem(
                        view.context.getString(R.string.sharePlaylist),
                        shareDrawable
                    ),
                    AlertListItem(
                        view.context.getString(R.string.openPlaylistInApp),
                        openInAppDrawable
                    )
                )

                if (playlist.public) {
                    itemList.add(
                        AlertListItem(
                            view.context.getString(R.string.makePlaylistPrivate),
                            playlistPrivateDrawable
                        )
                    )
                } else {
                    itemList.add(
                        AlertListItem(
                            view.context.getString(R.string.makePlaylistPublic),
                            playlistPublicDrawable
                        )
                    )
                }

                displayAlertDialogList(
                    view.context,
                    AlertListHeaderItem(
                        playlist.playlistName,
                        ""
                    ),
                    itemList
                ) { _, item ->
                    when (item.itemText) {
                        view.context.getString(R.string.download) -> {
                            downloadPlaylistAsync(
                                view,
                                id
                            ) {}
                        }
                        view.context.getString(R.string.addToQueue) -> {
                            playPlaylistNextAsync(
                                view.context,
                                id
                            )
                        }
                        view.context.getString(R.string.delete) -> {
                            deletePlaylist(view, id, deletePlaylistCallback)
                        }
                        view.context.getString(R.string.renamePlaylist) -> {
                            renamePlaylist(view, id)
                        }
                        view.context.getString(R.string.makePlaylistPrivate) -> {
                            togglePlaylistPublicStateAsync(
                                view,
                                id
                            )
                        }

                        view.context.getString(R.string.makePlaylistPublic) -> {
                            togglePlaylistPublicStateAsync(
                                view,
                                id
                            )
                        }

                        view.context.getString(R.string.sharePlaylist) -> {
                            openPlaylist(
                                view.context,
                                id,
                                true
                            )
                        }

                        view.context.getString(R.string.openPlaylistInApp) -> {
                            openPlaylist(
                                view.context,
                                id,
                                false
                            )
                        }
                    }
                }
            }
        }
    }

    fun getDownloadStatus(context: Context): String {
        val playlistTracks =
            ItemDatabaseHelper(context, playlistId).getAllData(true)

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

    override val type: Int = 1006

    override val layoutRes: Int
        get() = R.layout.list_single

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(
            v
        )
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<PlaylistItem>(view) {
        private val titleTextView: TextView = view.findViewById(R.id.title)
        private val coverImageView: ImageView = view.findViewById(R.id.cover)
        val titleDownloadButton: ImageButton =
            view.findViewById(R.id.titleDownloadButton)
        val titleMenuButton: ImageButton = view.findViewById(R.id.titleMenuButton)
        private val context = view.context

        override fun bindView(item: PlaylistItem, payloads: List<Any>) {
            val playlistDatabaseHelper = PlaylistDatabaseHelper(context)

            val playlist = playlistDatabaseHelper.getPlaylist(item.playlistId)

            playlist?.let {
                titleTextView.text = playlist.playlistName

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
                }, "", true)

                var downloadStatus = downloadDrawable.toUri()

                titleDownloadButton.setImageURI(downloadStatus)

                BackgroundAsync({
                    downloadStatus = item.getDownloadStatus(context).toUri()
                }, {
                    titleDownloadButton.setImageURI(downloadStatus)
                }).execute()
            }
        }

        override fun unbindView(item: PlaylistItem) {
            titleTextView.text = null
            coverImageView.setImageURI(null)
            titleDownloadButton.setImageURI(null)
        }
    }
}