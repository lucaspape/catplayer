package de.lucaspape.monstercat.ui.pages.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.*
import de.lucaspape.monstercat.core.download.addDownloadSong
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.next
import de.lucaspape.monstercat.ui.abstract_items.alert_list.AlertListItem
import de.lucaspape.monstercat.ui.abstract_items.util.HeaderTextItem
import de.lucaspape.monstercat.request.async.*
import de.lucaspape.monstercat.request.getAuthorizedRequestQueue
import de.lucaspape.monstercat.request.newLoadAlbumRequest
import de.lucaspape.monstercat.ui.*
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.util.*
import de.lucaspape.monstercat.core.util.BackgroundAsync
import de.lucaspape.monstercat.ui.pages.HomePage.Companion.addSongsTaskId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.collections.ArrayList

private val scope = CoroutineScope(Dispatchers.Default)

/**
 * Play an entire album after the current song
 */
fun playAlbumNext(view: View, mcID: String) {
    scope.launch {
        loadAlbumTracks(
            view.context,
            mcID,
            finishedCallback = { idArray ->
                addToPriorityQueue(idArray[0])

                for (i in (1 until idArray.size)) {
                    addToPriorityQueue(idArray[i])
                }
            },
            errorCallback = {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorRetrieveAlbumData),
                    view.context.getString(R.string.retry)
                ) {
                    playAlbumNext(view, mcID)
                }
            })
    }
}

fun playPlaylistNextAsync(context: Context, playlistId: String) {
    scope.launch {
        loadPlaylistTracks(context, true, playlistId, {}, {
            val playlistItemDatabaseHelper =
                ItemDatabaseHelper(context, playlistId)
            val playlistItemList = playlistItemDatabaseHelper.getAllData(true).reversed()

            val songDatabaseHelper = SongDatabaseHelper(context)

            songDatabaseHelper.getSong(
                playlistItemList[0].songId
            )?.songId?.let { songId ->
                addToPriorityQueue(songId)
            }

            for (i in (1 until playlistItemList.size)) {
                songDatabaseHelper.getSong(
                    playlistItemList[i].songId
                )?.songId?.let { songId ->
                    addToPriorityQueue(songId)
                }
            }
        }, {})
    }
}

/**
 * Download an entire playlist
 */
fun downloadPlaylistAsync(view: View, playlistId: String, downloadFinished: () -> Unit) {
    scope.launch {
        loadPlaylistTracks(view.context, true, playlistId, {}, {
            val playlistItemDatabaseHelper =
                ItemDatabaseHelper(view.context, playlistId)
            val playlistItemList = playlistItemDatabaseHelper.getAllData(true)

            for (playlistItem in playlistItemList) {
                val songDatabaseHelper = SongDatabaseHelper(view.context)
                val song = songDatabaseHelper.getSong(playlistItem.songId)

                song?.songId?.let { addDownloadSong(view.context, it, downloadFinished) }
            }
        }, {
            displaySnackBar(
                view,
                view.context.getString(R.string.errorRetrievePlaylist),
                view.context.getString(R.string.retry)
            ) {
                downloadPlaylistAsync(
                    view,
                    playlistId,
                    downloadFinished
                )
            }
        })
    }
}

/**
 * Deletes downloaded playlist tracks
 */
fun deleteDownloadedPlaylistTracksUI(
    context: Context,
    playlistId: String,
    deleteFinished: () -> Unit
) {
    showConfirmationAlert(context, context.getString(R.string.deletePlaylistDownloadedTracksMsg)) {
        val playlistItemDatabaseHelper = ItemDatabaseHelper(context, playlistId)
        val playlistItemList = playlistItemDatabaseHelper.getAllData(true)

        val songDatabaseHelper = SongDatabaseHelper(context)

        for (playlistItem in playlistItemList) {
            val song = songDatabaseHelper.getSong(playlistItem.songId)

            song?.deleteDownload(context)
        }

        deleteFinished()
    }
}

/**
 * Download an entire album
 */
fun downloadAlbum(view: View, mcID: String) {
    scope.launch {
        loadAlbumTracks(
            view.context,
            mcID,
            finishedCallback = { idArray ->
                val databaseHelper = SongDatabaseHelper(view.context)

                for (id in idArray) {
                    val song = databaseHelper.getSong(id)
                    song?.songId?.let { addDownloadSong(view.context, it) {} }
                }
            },
            errorCallback = {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorRetrieveAlbumData),
                    view.context.getString(R.string.retry)
                ) {
                    downloadAlbum(view, mcID)
                }
            })
    }
}

/**
 * Add single song to playlist, will ask for playlist with alertDialog TODO check if logged in
 */
fun addSongToPlaylistUI(view: View, songId: String) {
    scope.launch {
        loadPlaylists(view.context,
            forceReload = false,
            loadManual = false,
            displayLoading = {},
            finishedCallback = {
                scope.launch {
                    withContext(Dispatchers.Main) {
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
                                    addSongToPlaylistAsync(
                                        view,
                                        playlistId,
                                        songId
                                    )
                                }
                            }

                        } else {
                            displayInfo(
                                view.context,
                                view.context.getString(R.string.noPlaylistFound)
                            )
                        }
                    }
                }
            },
            errorCallback = {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorLoadingPlaylists),
                    view.context.getString(R.string.retry)
                ) {
                    addSongToPlaylistUI(
                        view,
                        songId
                    )
                }
            })
    }
}

private fun addSongToPlaylistAsync(view: View, playlistId: String, songId: String) {
    scope.launch {
        addToPlaylist(view.context, playlistId, songId, {
            displaySnackBar(view, view.context.getString(R.string.songAddedToPlaylistMsg), null) {}
            ItemDatabaseHelper(view.context, playlistId).reCreateTable()
        }, {
            displaySnackBar(
                view,
                view.context.getString(R.string.errorRetrievePlaylist),
                view.context.getString(R.string.retry)
            ) {
                addSongToPlaylistAsync(
                    view,
                    playlistId,
                    songId
                )
            }
        })
    }
}

fun renamePlaylistUI(view: View, playlistId: String) {
    if (loggedIn) {
        showInputAlert(
            view.context,
            true,
            R.layout.playlistname_input_layout,
            R.id.playlistNameInput,
            view.findViewById(R.id.recyclerView),
            view.context.getString(R.string.renamePlaylist),
            PlaylistDatabaseHelper(view.context).getPlaylist(playlistId)?.playlistName,
            view.context.getString(R.string.playlistName)
        ) {
            renamePlaylistAsync(
                view,
                playlistId,
                it
            )
        }
    } else {
        displaySnackBar(view, view.context.getString(R.string.errorNotLoggedIn), null) {}
    }
}

private fun renamePlaylistAsync(view: View, playlistId: String, playlistName: String) {
    scope.launch {
        renamePlaylist(view.context,
            playlistId,
            playlistName,
            finishedCallback = {
                displaySnackBar(view, view.context.getString(R.string.playlistRenamedMsg), null) {}
            },
            errorCallback = {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.renamePlaylistError),
                    view.context.getString(R.string.retry)
                ) {
                    renamePlaylistAsync(
                        view,
                        playlistName,
                        playlistId
                    )
                }
            })
    }
}

fun togglePlaylistPublicStateAsync(view: View, playlistId: String) {
    scope.launch {
        PlaylistDatabaseHelper(view.context).getPlaylist(playlistId)?.let { playlist ->
            changePlaylistPublicState(view.context, playlistId, !playlist.public, {
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
            }, {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.playlistStateChangeError),
                    view.context.getString(R.string.retry)
                ) {
                    togglePlaylistPublicStateAsync(
                        view,
                        playlistId
                    )
                }
            })
        }
    }
}

/**
 * Create a new playlist, will ask for name with alertDialog
 */
fun createPlaylistUI(view: View) {
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

            val title = if (addPlaylist) {
                context.getString(R.string.addPlaylistId)
            } else {
                context.getString(R.string.createPlaylist)
            }

            showInputAlert(
                context,
                true,
                R.layout.playlistname_input_layout,
                R.id.playlistNameInput,
                view.findViewById(R.id.recyclerView),
                title,
                null,
                context.getString(R.string.playlistName)
            ) {
                if (addPlaylist) {
                    addPlaylist(
                        view,
                        it
                    )
                } else {
                    createPlaylistAsync(
                        view,
                        it
                    )
                }
            }
        }
    } else {
        displaySnackBar(view, view.context.getString(R.string.errorNotLoggedIn), null) {}
    }
}

fun addPlaylist(view: View, playlistId: String) {
    val manualPlaylistDatabaseHelper = ManualPlaylistDatabaseHelper(view.context)
    manualPlaylistDatabaseHelper.insertPlaylist(playlistId)
}

private fun createPlaylistAsync(view: View, playlistName: String) {
    scope.launch {
        createPlaylist(view.context, playlistName, {
            displaySnackBar(view, view.context.getString(R.string.playlistCreatedMsg), null) {}
        }, {
            displaySnackBar(
                view,
                view.context.getString(R.string.errorCreatingPlaylist),
                view.context.getString(R.string.retry)
            ) {
                createPlaylistAsync(
                    view,
                    playlistName
                )
            }
        })
    }
}

/**
 * Delete playlist with given playlistId
 */
fun deletePlaylistUI(view: View, playlistId: String, callback: () -> Unit) {
    showConfirmationAlert(
        view.context, view.context.getString(R.string.deletePlaylistMsg),
    ) {
        deletePlaylistAsync(
            view,
            playlistId,
            callback,
        )
    }
}

private fun deletePlaylistAsync(view: View, playlistId: String, callback: () -> Unit) {
    scope.launch {
        PlaylistDatabaseHelper(view.context).getPlaylist(playlistId)?.ownPlaylist?.let { deleteRemote ->
            deletePlaylist(view.context, playlistId, deleteRemote, true, {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.playlistDeletedMsg),
                    null
                ) {}

                callback()
            }, {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorDeletingPlaylist),
                    view.context.getString(R.string.retry)
                ) {
                    deletePlaylistAsync(
                        view,
                        playlistId,
                        callback
                    )
                }
            })
        }
    }
}

/**
 * Delete one song from playlist, index required because double songs can exist in one playlist
 */
fun deletePlaylistSong(
    view: View,
    songId: String,
    playlistId: String,
    index: Int,
    playlistMax: Int,
    callback: () -> Unit
) {
    scope.launch {
        //for playlist sorting features
        val playlistSortedByNew = true

        if (playlistSortedByNew) {
            //required because api sorts by oldest songs added
            val songDeleteIndex = playlistMax - index

            deletePlaylistTrack(view.context, songId, playlistId, songDeleteIndex, {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.removedSongFromPlaylistMsg),
                    null
                ) {}

                callback()
            }, {
                displaySnackBar(
                    view,
                    view.context.getString(R.string.errorRemovingSongFromPlaylist),
                    view.context.getString(R.string.retry)
                ) {
                    deletePlaylistSong(
                        view,
                        songId,
                        playlistId,
                        index,
                        playlistMax,
                        callback
                    )
                }
            })
        }
    }
}

fun openAlbumUI(view: View, albumMcId: String, share: Boolean) {
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

        displayAlertDialogList(
            context,
            HeaderTextItem(title), itemArray
        ) { position, _ ->
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
            openAlbumUI(
                view,
                albumMcId,
                share
            )
        }
    }))
}

fun openPlaylist(context: Context, playlistId: String, share: Boolean) {
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

fun playSongsFromViewDataAsync(
    context: Context,
    catalogViewData: ArrayList<CatalogItem>,
    itemIndex: Int
) {
    addSongsTaskId = ""

    clearPlaylist()
    clearQueue()

    pushToPriorityQueue(catalogViewData[itemIndex].songId)
    skipPreviousInPlaylist()
    next(context)

    //add visible next songs
    BackgroundAsync({
        val id = UUID.randomUUID().toString()
        addSongsTaskId = id

        for (i in (itemIndex + 1 until catalogViewData.size)) {
            try {
                if (id == addSongsTaskId) {
                    addToQueue(context, catalogViewData[i].songId)
                } else {
                    break
                }

            } catch (e: IndexOutOfBoundsException) {

            }

        }
    }, {}).execute()
}

fun addFilterUI(view: View, callback: () -> Unit) {
    val alertListItem = arrayListOf(
        AlertListItem(
            view.context.getString(R.string.addArtistFilter),
            emptyDrawable
        ),
        AlertListItem(
            view.context.getString(R.string.addTitleFilter),
            emptyDrawable
        )
    )

    displayAlertDialogList(
        view.context,
        HeaderTextItem(""),
        alertListItem
    ) { _: Int, item: AlertListItem ->
        val artistFilter = item.itemText == view.context.getString(R.string.addArtistFilter)

        val title = if (artistFilter) {
            view.context.getString(R.string.addArtistFilter)
        } else {
            view.context.getString(R.string.addTitleFilter)
        }

        val hint = if (artistFilter) {
            "Artist"
        } else {
            "Title"
        }

        showInputAlert(
            view.context,
            true,
            R.layout.filter_input_layout,
            R.id.filter_input,
            view.findViewById(R.id.recyclerView),
            title,
            null,
            hint
        ) {
            if (artistFilter) {
                FilterDatabaseHelper(view.context).insertFilter("artist", it)
            } else {
                FilterDatabaseHelper(view.context).insertFilter("title", it)
            }

            applyFilterSettings(view.context)

            callback()
        }
    }
}