package de.lucaspape.monstercat.ui.handlers

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.*
import de.lucaspape.monstercat.core.download.addDownloadSong
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.next
import de.lucaspape.monstercat.ui.abstract_items.alert_list.AlertListItem
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.request.async.*
import de.lucaspape.monstercat.request.newLoadAlbumRequest
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.ui.addPlaylistDrawable
import de.lucaspape.monstercat.ui.createPlaylistDrawable
import de.lucaspape.monstercat.ui.handlers.home.HomeHandler
import de.lucaspape.monstercat.util.*
import de.lucaspape.util.BackgroundAsync
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.collections.ArrayList

fun loadAlbumTracks(
    context: Context,
    mcID: String,
    finishedCallback: (trackIds: ArrayList<String>) -> Unit,
    errorCallback: () -> Unit
) {
    val albumRequestQueue =
        getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

    albumRequestQueue.add(newLoadAlbumRequest(context, mcID, {
        parseAlbumToDB(it.getJSONObject("release"), context)

        val jsonArray = it.getJSONArray("tracks")

        val idArray = ArrayList<String>()

        AlbumDatabaseHelper(context).getAlbumFromMcId(mcID)?.let { album ->
            for (i in (0 until jsonArray.length())) {
                parseAlbumSongToDB(jsonArray.getJSONObject(i), album.albumId, context)?.let { id ->
                    idArray.add(id)
                }
            }
        }

        finishedCallback(idArray)
    }, {
        errorCallback()
    }))
}

/**
 * Play an entire album after the current song
 */
internal fun playAlbumNext(view: View, mcID: String) {
    loadAlbumTracks(view.context, mcID, finishedCallback = { idArray ->
        prioritySongQueue.add(idArray[0])

        for (i in (1 until idArray.size)) {
            prioritySongQueue.add(idArray[i])
        }
    }, errorCallback = {
        displaySnackBar(
            view,
            view.context.getString(R.string.errorRetrieveAlbumData),
            view.context.getString(R.string.retry)
        ) {
            playAlbumNext(view, mcID)
        }
    })
}

internal fun playPlaylistNextAsync(context: Context, playlistId: String) {
    loadPlaylistTracksAsync(context, true, playlistId, {}, { _, _, _ ->
        val playlistItemDatabaseHelper =
            PlaylistItemDatabaseHelper(context, playlistId)
        val playlistItemList = playlistItemDatabaseHelper.getAllData().reversed()

        val songDatabaseHelper = SongDatabaseHelper(context)

        songDatabaseHelper.getSong(
            context,
            playlistItemList[0].songId
        )?.songId?.let { songId ->
            prioritySongQueue.add(songId)
        }

        for (i in (1 until playlistItemList.size)) {
            songDatabaseHelper.getSong(
                context,
                playlistItemList[i].songId
            )?.songId?.let { songId ->
                prioritySongQueue.add(songId)
            }
        }
    }, { _, _, _ -> })
}

/**
 * Download an entire playlist
 */
internal fun downloadPlaylistAsync(view: View, playlistId: String, downloadFinished: () -> Unit) {
    loadPlaylistTracksAsync(view.context, true, playlistId, {} , {_,_,_->
        val playlistItemDatabaseHelper =
            PlaylistItemDatabaseHelper(view.context, playlistId)
        val playlistItemList = playlistItemDatabaseHelper.getAllData()

        for (playlistItem in playlistItemList) {
            val songDatabaseHelper = SongDatabaseHelper(view.context)
            val song = songDatabaseHelper.getSong(view.context, playlistItem.songId)

            song?.songId?.let { addDownloadSong(view.context, it, downloadFinished) }
        }
    }, {_,_,_->
        displaySnackBar(
            view,
            view.context.getString(R.string.errorRetrievePlaylist),
            view.context.getString(R.string.retry)
        ) {
            downloadPlaylistAsync(view, playlistId, downloadFinished)
        }
    })
}

/**
 * Deletes downloaded playlist tracks
 */
internal fun deleteDownloadedPlaylistTracks(
    context: Context,
    playlistId: String,
    deleteFinished: () -> Unit
) {
    val alertDialogBuilder = MaterialAlertDialogBuilder(context)
    alertDialogBuilder.setTitle(context.getString(R.string.deletePlaylistDownloadedTracksMsg))
    alertDialogBuilder.setPositiveButton(context.getString(R.string.yes)) { _, _ ->
        val playlistItemDatabaseHelper = PlaylistItemDatabaseHelper(context, playlistId)
        val playlistItemList = playlistItemDatabaseHelper.getAllData()

        val songDatabaseHelper = SongDatabaseHelper(context)

        for (playlistItem in playlistItemList) {
            val song = songDatabaseHelper.getSong(context, playlistItem.songId)

            song?.let {
                File(song.downloadLocation).delete()
            }
        }

        deleteFinished()
    }

    alertDialogBuilder.setNegativeButton(context.getString(R.string.no)) { _, _ -> }

    val dialog = alertDialogBuilder.create()
    dialog.show()

    val typedValue = TypedValue()
    context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

    val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
    positiveButton.setTextColor(typedValue.data)

    val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
    negativeButton.setTextColor(typedValue.data)
}

/**
 * Download an entire album
 */
internal fun downloadAlbum(view: View, mcID: String) {
    loadAlbumTracks(view.context, mcID, finishedCallback = { idArray ->
        val databaseHelper = SongDatabaseHelper(view.context)

        for (id in idArray) {
            val song = databaseHelper.getSong(view.context, id)
            song?.songId?.let { addDownloadSong(view.context, it) {} }
        }
    }, errorCallback = {
        displaySnackBar(
            view,
            view.context.getString(R.string.errorRetrieveAlbumData),
            view.context.getString(R.string.retry)
        ) {
            downloadAlbum(view, mcID)
        }
    })
}

/**
 * Add single song to playlist, will ask for playlist with alertDialog TODO check if logged in
 */
internal fun addSongToPlaylist(view: View, songId: String) {
    loadPlaylistAsync(view.context,
        forceReload = true,
        loadManual = false,
        displayLoading = {},
        finishedCallback = { _, _ ->
            val playlistDatabaseHelper =
                PlaylistDatabaseHelper(view.context)
            val playlistList = playlistDatabaseHelper.getAllPlaylists()

            if (playlistList.isNotEmpty()) {
                val playlistNames = arrayOfNulls<String>(playlistList.size)
                val playlistIds = arrayOfNulls<String>(playlistList.size)

                for (i in playlistList.indices) {
                    playlistNames[i] = playlistList[i].playlistName
                    playlistIds[i] = playlistList[i].playlistId
                }

                val alertListItems = ArrayList<AlertListItem>()

                for (name in playlistNames) {
                    name?.let {
                        alertListItems.add(
                            AlertListItem(
                                name,
                                ""
                            )
                        )
                    }
                }

                displayAlertDialogList(
                    view.context,
                    HeaderTextItem(
                        view.context.getString(R.string.pickPlaylistMsg)
                    ),
                    alertListItems
                ) { position, _ ->
                    playlistIds[position]?.let { playlistId ->
                        addSongToPlaylistAsync(view, playlistId, songId)
                    }
                }

            } else {
                displayInfo(view.context, view.context.getString(R.string.noPlaylistFound))
            }
        },
        errorCallback = { _, _ ->
            displaySnackBar(
                view,
                view.context.getString(R.string.errorRetrievePlaylist),
                view.context.getString(R.string.retry)
            ) {
                addSongToPlaylist(view, songId)
            }
        })
}

private fun addSongToPlaylistAsync(view: View, playlistId: String, songId: String) {
    addToPlaylistAsync(view.context, playlistId, songId, { _, _ ->
        displaySnackBar(view, view.context.getString(R.string.songAddedToPlaylistMsg), null) {}
    }, { _, _ ->
        displaySnackBar(
            view,
            view.context.getString(R.string.errorRetrievePlaylist),
            view.context.getString(R.string.retry)
        ) {
            addSongToPlaylistAsync(view, playlistId, songId)
        }
    })
}

internal fun renamePlaylist(view: View, playlistId: String) {
    if (loggedIn) {
        val context = view.context

        MaterialAlertDialogBuilder(context).apply {
            val layoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            val playlistNameInputLayout =
                layoutInflater.inflate(R.layout.playlistname_input_layout, null)

            val playlistNameEditText =
                playlistNameInputLayout.findViewById<EditText>(R.id.playlistNameInput)

            setTitle(context.getString(R.string.renamePlaylist))

            setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                val playlistName = playlistNameEditText.text.toString()
                renamePlaylistAsync(view, playlistId, playlistName)
            }

            setView(playlistNameInputLayout)
            setCancelable(true)
        }.create().run {
            show()

            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

            val positiveButton = getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setTextColor(typedValue.data)

            val negativeButton = getButton(DialogInterface.BUTTON_NEGATIVE)
            negativeButton.setTextColor(typedValue.data)
        }
    } else {
        displaySnackBar(view, view.context.getString(R.string.errorNotLoggedIn), null) {}
    }
}

private fun renamePlaylistAsync(view: View, playlistId: String, playlistName: String) {
        renamePlaylistAsync(view.context,
            playlistId,
            playlistName,
            finishedCallback = { _, _ ->
                displaySnackBar(view, view.context.getString(R.string.playlistRenamedMsg), null) {}
            },
            errorCallback = { _, _ ->
                displaySnackBar(
                    view,
                    view.context.getString(R.string.renamePlaylistError),
                    view.context.getString(R.string.retry)
                ) {
                    renamePlaylistAsync(view, playlistName, playlistId)
                }
            })
}

internal fun togglePlaylistPublicStateAsync(view: View, playlistId: String) {
    PlaylistDatabaseHelper(view.context).getPlaylist(playlistId)?.let { playlist ->
        changePlaylistPublicStateAsync(view.context, playlistId, !playlist.public, { _, _ ->
            val msg = if (playlist.public) {
                view.context.getString(R.string.playlistNowPrivateMsg)
            } else {
                view.context.getString(R.string.playlistNowPublicMsg)
            }

            displaySnackBar(
                view,
                msg,
                null
            ) {}
        }, { _, _ ->
            displaySnackBar(
                view,
                view.context.getString(R.string.playlistStateChangeError),
                view.context.getString(R.string.retry)
            ) {
                togglePlaylistPublicStateAsync(view, playlistId)
            }
        })
    }
}

/**
 * Create a new playlist, will ask for name with alertDialog
 */
internal fun createPlaylist(view: View) {
    if (loggedIn) {
        val context = view.context

        val alertListItem = arrayListOf(
            AlertListItem(
                context.getString(R.string.createPlaylist),
                createPlaylistDrawable
            ),
            AlertListItem(
                context.getString(R.string.addPlaylistId),
                addPlaylistDrawable
            )
        )

        displayAlertDialogList(
            context,
            HeaderTextItem(""),
            alertListItem
        ) { _: Int, item: AlertListItem ->
            val addPlaylist = item.itemText == context.getString(R.string.addPlaylistId)

            MaterialAlertDialogBuilder(context).apply {
                val layoutInflater =
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                val playlistNameInputLayout =
                    layoutInflater.inflate(R.layout.playlistname_input_layout, null)

                val playlistNameEditText =
                    playlistNameInputLayout.findViewById<EditText>(R.id.playlistNameInput)

                if (addPlaylist) {
                    playlistNameEditText.hint = context.getString(R.string.playlistId)
                }

                if (addPlaylist) {
                    setTitle(context.getString(R.string.addPlaylistId))
                } else {
                    setTitle(context.getString(R.string.createPlaylist))
                }

                setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                    val playlistName = playlistNameEditText.text.toString()

                    if (addPlaylist) {
                        addPlaylist(view, playlistName)
                    } else {
                        createPlaylistAsync(view, playlistName)
                    }
                }

                setView(playlistNameInputLayout)
                setCancelable(true)
            }.create().run {
                show()

                val typedValue = TypedValue()
                context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

                val positiveButton = getButton(DialogInterface.BUTTON_POSITIVE)
                positiveButton.setTextColor(typedValue.data)

                val negativeButton = getButton(DialogInterface.BUTTON_NEGATIVE)
                negativeButton.setTextColor(typedValue.data)
            }
        }
    } else {
        displaySnackBar(view, view.context.getString(R.string.errorNotLoggedIn), null) {}
    }
}

internal fun addPlaylist(view: View, playlistId: String) {
    val manualPlaylistDatabaseHelper = ManualPlaylistDatabaseHelper(view.context)
    manualPlaylistDatabaseHelper.insertPlaylist(playlistId)
}

private fun createPlaylistAsync(view: View, playlistName: String) {
    createPlaylistAsync(view.context, playlistName, {
        displaySnackBar(view, view.context.getString(R.string.playlistCreatedMsg), null) {}
    }, {
        displaySnackBar(
            view,
            view.context.getString(R.string.errorCreatingPlaylist),
            view.context.getString(R.string.retry)
        ) {
            createPlaylistAsync(view, playlistName)
        }
    })
}

/**
 * Delete playlist with given playlistId
 */
internal fun deletePlaylist(view: View, playlistId: String) {
    val context = view.context
    val alertDialogBuilder = MaterialAlertDialogBuilder(context)
    alertDialogBuilder.setTitle(context.getString(R.string.deletePlaylistMsg))
    alertDialogBuilder.setPositiveButton(context.getString(R.string.yes)) { _, _ ->
        deletePlaylistAsync(view, playlistId, true)
    }
    alertDialogBuilder.setNegativeButton(context.getString(R.string.no)) { _, _ ->
        deletePlaylistAsync(view, playlistId, false)
    }

    val dialog = alertDialogBuilder.create()
    dialog.show()

    val typedValue = TypedValue()
    context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)

    val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
    positiveButton.setTextColor(typedValue.data)

    val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
    negativeButton.setTextColor(typedValue.data)
}

private fun deletePlaylistAsync(view: View, playlistId: String, force: Boolean) {
    if (force) {
        PlaylistDatabaseHelper(view.context).getPlaylist(playlistId)?.ownPlaylist?.let { deleteRemote ->
            deletePlaylistAsync(view.context, playlistId, deleteRemote, true, {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.playlistDeletedMsg),
                    null
                ) {}
            }, {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorDeletingPlaylist),
                    view.context.getString(R.string.retry)
                ) {
                    deletePlaylistAsync(view, playlistId, true)
                }
            })
        }
    }
}

/**
 * Delete one song from playlist, index required because double songs can exist in one playlist
 */
internal fun deletePlaylistSong(
    view: View,
    songId: String,
    playlistId: String,
    index: Int,
    playlistMax: Int
) {
    //for playlist sorting features
    val playlistSortedByNew = true

    if (playlistSortedByNew) {
        //required because api sorts by oldest songs added
        val songDeleteIndex = playlistMax - index

        deletePlaylistTrackAsync(view.context, songId, playlistId, songDeleteIndex, { _, _, _ ->
            displaySnackBar(
                view,
                view.context.getString(R.string.removedSongFromPlaylistMsg),
                null
            ) {}
        }, { _, _, _ ->
            displaySnackBar(
                view,
                view.context.getString(R.string.errorRemovingSongFromPlaylist),
                view.context.getString(R.string.retry)
            ) {
                deletePlaylistSong(view, songId, playlistId, index, playlistMax)
            }
        })
    }
}

internal fun openAlbum(view: View, albumMcId: String, share: Boolean) {
    val context = view.context
    val volleyRequestQueue =
        getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

    volleyRequestQueue.add(newLoadAlbumRequest(context, albumMcId, {
        val releaseObject = it.getJSONObject("release")
        val linksArray = releaseObject.getJSONArray("links")

        val itemArray = ArrayList<AlertListItem>()
        val urlArray = ArrayList<String>()

        itemArray.add(
            AlertListItem(
                "monstercat.com",
                ""
            )
        )
        urlArray.add(context.getString(R.string.shareReleaseUrl) + "/$albumMcId")

        for (i in (0 until linksArray.length())) {
            val linkObject = linksArray.getJSONObject(i)

            itemArray.add(
                AlertListItem(
                    linkObject.getString("platform"),
                    ""
                )
            )
            urlArray.add(linkObject.getString("original"))
        }

        val title = if (share) {
            context.getString(R.string.pickLinkToShare)
        } else {
            context.getString(R.string.pickApp)
        }

        displayAlertDialogList(context,
            HeaderTextItem(title), itemArray) { position, _ ->
            urlArray[position].let { url ->
                if (share) {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, url)
                        type = "text/plain"
                    }

                    val shareIntent = Intent.createChooser(sendIntent, null)
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(shareIntent)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        }
    }, {
        displaySnackBar(
            view,
            context.getString(R.string.errorRetrieveAlbumData),
            context.getString(R.string.retry)
        ) {
            openAlbum(view, albumMcId, share)
        }
    }))
}

internal fun openPlaylist(context: Context, playlistId: String, share: Boolean) {
    val url = context.getString(R.string.playlistShareUrl) + playlistId

    if (share) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    } else {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

internal fun playSongsFromCatalogDbAsync(
    context: Context,
    skipMonstercatSongs: Boolean,
    firstSongId: String
) {
    HomeHandler.addSongsTaskId = ""

    clearPlaylist()
    clearQueue()

    songQueue.add(firstSongId)
    skipPreviousInPlaylist()
    next(context)

    //add next songs from database
    val songDatabaseHelper = SongDatabaseHelper(context)
    val catalogSongDatabaseHelper = CatalogSongDatabaseHelper(context)

    BackgroundAsync({
        val id = UUID.randomUUID().toString()
        HomeHandler.addSongsTaskId = id

        catalogSongDatabaseHelper.getIndexFromSongId(firstSongId)?.toLong()
            ?.let { skip ->
                val nextSongs = catalogSongDatabaseHelper.getSongs(skip)

                nextSongs.reverse()

                for (catalogSong in nextSongs) {

                    if (skipMonstercatSongs) {
                        val song =
                            songDatabaseHelper.getSong(
                                context,
                                catalogSong.songId
                            )
                        if (!song?.artist.equals("monstercat", true)) {
                            if (id == HomeHandler.addSongsTaskId) {
                                songQueue.add(catalogSong.songId)
                            } else {
                                break
                            }
                        }
                    } else {
                        if (id == HomeHandler.addSongsTaskId) {
                            songQueue.add(catalogSong.songId)
                        } else {
                            break
                        }
                    }

                }
            }
    }, {}).execute()
}

internal fun playSongsFromViewDataAsync(
    context: Context,
    skipMonstercatSongs: Boolean,
    catalogViewData: ArrayList<CatalogItem>,
    itemIndex: Int
) {
    HomeHandler.addSongsTaskId = ""

    clearPlaylist()
    clearQueue()

    songQueue.add(catalogViewData[itemIndex].songId)
    skipPreviousInPlaylist()
    next(context)

    //add visible next songs
    BackgroundAsync({
        val id = UUID.randomUUID().toString()
        HomeHandler.addSongsTaskId = id

        val songDatabaseHelper = SongDatabaseHelper(context)

        for (i in (itemIndex + 1 until catalogViewData.size)) {
            try {
                if (skipMonstercatSongs) {
                    val song =
                        songDatabaseHelper.getSong(
                            context,
                            catalogViewData[i].songId
                        )

                    if (!song?.artist.equals("monstercat", true)) {
                        if (id == HomeHandler.addSongsTaskId) {
                            songQueue.add(catalogViewData[i].songId)
                        } else {
                            break
                        }
                    }
                } else {
                    if (id == HomeHandler.addSongsTaskId) {
                        songQueue.add(catalogViewData[i].songId)
                    } else {
                        break
                    }
                }

            } catch (e: IndexOutOfBoundsException) {

            }

        }
    }, {}).execute()
}