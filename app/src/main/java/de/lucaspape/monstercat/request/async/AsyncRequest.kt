package de.lucaspape.monstercat.request.async

import android.content.Context
import android.os.AsyncTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.*
import de.lucaspape.monstercat.database.objects.Song
import de.lucaspape.monstercat.request.*
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.monstercat.util.*
import de.lucaspape.util.BackgroundAsync
import de.lucaspape.util.Settings
import org.json.JSONException
import org.json.JSONObject
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference

fun addToPlaylistAsync(
    context: Context,
    playlistId: String,
    songId: String,
    finishedCallback: (playlistId: String, songId: String) -> Unit,
    errorCallback: (playlistId: String, songId: String) -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                val addToPlaylistQueue =
                    getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

                var success = true
                val syncObject = Object()

                addToPlaylistQueue.add(
                    newAddToPlaylistRequest(
                        context,
                        playlistId,
                        song,
                        {
                            synchronized(syncObject) {
                                syncObject.notify()
                            }
                        },
                        {
                            success = false
                            synchronized(syncObject) {
                                syncObject.notify()
                            }
                        })
                )

                synchronized(syncObject) {
                    syncObject.wait()

                    return@BackgroundAsync success
                }
            }
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(playlistId, songId)
        } else {
            errorCallback(playlistId, songId)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun addTrackToDBAsync(
    context: Context,
    trackId: String,
    finishedCallback: (trackId: String, song: Song) -> Unit,
    errorCallback: (trackId: String) -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            val syncObject = Object()

            val volleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            newSearchTrackRequest(context, trackId, 0, true, {
                val jsonArray = it.getJSONArray("results")

                for (i in (0 until jsonArray.length())) {
                    parseSongToDB(jsonArray.getJSONObject(i), context)
                }

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }, {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            })?.let {
                volleyQueue.add(it)

                synchronized(syncObject) {
                    syncObject.wait()

                    return@BackgroundAsync SongDatabaseHelper(context).getSong(context, trackId)
                }
            }
        }

        return@BackgroundAsync null
    }, {
        if (it != null) {
            finishedCallback(trackId, it)
        } else {
            errorCallback(trackId)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun changePlaylistPublicStateAsync(
    context: Context,
    playlistId: String,
    public: Boolean,
    finishedCallback: (playlistId: String, public: Boolean) -> Unit,
    errorCallback: (playlistId: String, public: Boolean) -> Unit
) {

    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            val newPlaylistVolleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            var success = true
            val syncObject = Object()

            newPlaylistVolleyQueue.add(
                newChangePlaylistPublicStateRequest(
                    context,
                    playlistId,
                    public,
                    {
                        synchronized(syncObject) {
                            syncObject.notify()
                        }
                    },
                    {
                        success = false
                        synchronized(syncObject) {
                            syncObject.notify()
                        }
                    })
            )

            synchronized(syncObject) {
                syncObject.wait()

                return@BackgroundAsync success
            }
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(playlistId, public)
        } else {
            errorCallback(playlistId, public)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun checkCustomApiFeaturesAsync(
    context: Context,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            val newVolleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            var success = true
            val syncObject = Object()

            val settings = Settings.getSettings(context)

            settings.setBoolean(context.getString(R.string.customApiSupportsV1Setting), false)
            settings.setBoolean(context.getString(R.string.customApiSupportsV2Setting), false)

            newVolleyQueue.add(newCustomApiFeatureRequest(context, {
                try {
                    val apiVersionsArray = it.getJSONArray("api_versions")

                    for (i in (0 until apiVersionsArray.length())) {
                        if (apiVersionsArray.getString(i) == "v1") {
                            settings.setBoolean(
                                context.getString(R.string.customApiSupportsV1Setting),
                                true
                            )
                        } else if (apiVersionsArray.getString(i) == "v2") {
                            settings.setBoolean(
                                context.getString(R.string.customApiSupportsV2Setting),
                                true
                            )
                        }
                    }

                    settings.setString(
                        context.getString(R.string.customDownloadUrlSetting),
                        it.getString("download_base_url")
                    )
                    settings.setString(
                        context.getString(R.string.customStreamUrlSetting),
                        it.getString("stream_base_url")
                    )

                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                } catch (e: JSONException) {
                    success = false

                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }
            }, {
                success = false

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }))

            synchronized(syncObject) {
                syncObject.wait()

                return@BackgroundAsync success
            }
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback()
        } else {
            errorCallback()
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun createPlaylistAsync(
    context: Context,
    playlistName: String,
    finishedCallback: (playlistName: String) -> Unit,
    errorCallback: (playlistName: String) -> Unit
) {

    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            val newPlaylistVolleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            var success = true
            val syncObject = Object()

            newPlaylistVolleyQueue.add(newCreatePlaylistRequest(context, playlistName, {
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }, {
                success = false
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }))

            synchronized(syncObject) {
                syncObject.wait()

                return@BackgroundAsync success
            }
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(playlistName)
        } else {
            errorCallback(playlistName)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun deletePlaylistAsync(
    context: Context,
    playlistId: String,
    deleteRemote: Boolean,
    deleteLocal: Boolean,
    finishedCallback: (playlistId: String) -> Unit,
    errorCallback: (playlistId: String) -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            var success = true

            if (deleteLocal) {
                val playlistDatabaseHelper = PlaylistDatabaseHelper(context)
                playlistDatabaseHelper.removePlaylist(playlistId)

                val manualPlaylistDatabaseHelper = ManualPlaylistDatabaseHelper(context)
                manualPlaylistDatabaseHelper.removePlaylist(playlistId)
            }

            if (deleteRemote) {
                val deletePlaylistVolleyQueue =
                    getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))
                val syncObject = Object()

                deletePlaylistVolleyQueue.add(newDeletePlaylistRequest(context, playlistId, {
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }, {
                    success = false
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }))

                synchronized(syncObject) {
                    syncObject.wait()

                    return@BackgroundAsync success
                }
            }

            return@BackgroundAsync success
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(playlistId)
        } else {
            errorCallback(playlistId)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun deletePlaylistTrackAsync(
    context: Context,
    songId: String,
    playlistId: String,
    songDeleteIndex: Int,
    finishedCallback: (songId: String, playlistId: String, songDeleteIndex: Int) -> Unit,
    errorCallback: (songId: String, playlistId: String, songDeleteIndex: Int) -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                val deleteSongVolleyQueue =
                    getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

                var success = true
                val syncObject = Object()

                deleteSongVolleyQueue.add(
                    newDeletePlaylistTrackRequest(
                        context,
                        playlistId,
                        song,
                        songDeleteIndex,
                        {
                            synchronized(syncObject) {
                                syncObject.notify()
                            }
                        },
                        {
                            success = false
                            synchronized(syncObject) {
                                syncObject.notify()
                            }
                        })
                )

                synchronized(syncObject) {
                    syncObject.wait()

                    return@BackgroundAsync success
                }
            }
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(songId, playlistId, songDeleteIndex)
        } else {
            errorCallback(songId, playlistId, songDeleteIndex)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun loadAlbumAsync(
    context: Context,
    forceReload: Boolean,
    albumId: String,
    mcId: String,
    displayLoading: () -> Unit,
    finishedCallback: (forceReload: Boolean, albumId: String, mcId: String, displayLoading: () -> Unit) -> Unit,
    errorCallback: (forceReload: Boolean, albumId: String, mcId: String, displayLoading: () -> Unit) -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let {
            val albumItemDatabaseHelper =
                AlbumItemDatabaseHelper(context, albumId)
            val albumItems = albumItemDatabaseHelper.getAllData()

            if (!forceReload && albumItems.isNotEmpty()) {
                finishedCallback(forceReload, albumId, mcId, displayLoading)
                return@BackgroundAsync false
            } else {
                displayLoading()
                //continue to background task
                return@BackgroundAsync true
            }
        }

        return@BackgroundAsync false
    }, {
        contextReference.get()?.let { context ->
            val requestQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            val albumItemDatabaseHelper =
                AlbumItemDatabaseHelper(context, albumId)

            displayLoading()

            var success = true
            val syncObject = Object()

            requestQueue.add(newLoadAlbumRequest(context, mcId, {
                val jsonArray = it.getJSONArray("tracks")

                albumItemDatabaseHelper.reCreateTable()

                //parse every single song into list
                for (k in (0 until jsonArray.length())) {
                    parseAlbumSongToDB(
                        jsonArray.getJSONObject(k),
                        albumId,
                        context
                    )
                }

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }, {
                success = false
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }))
            synchronized(syncObject) {
                syncObject.wait()

                return@BackgroundAsync success
            }

        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(forceReload, albumId, mcId, displayLoading)
        } else {
            errorCallback(forceReload, albumId, mcId, displayLoading)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun loadAlbumListAsync(
    context: Context,
    forceReload: Boolean,
    skip: Int,
    displayLoading: () -> Unit,
    finishedCallback: (forceReload: Boolean, skip: Int, displayLoading: () -> Unit) -> Unit,
    errorCallback: (forceReload: Boolean, skip: Int, displayLoading: () -> Unit) -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            val albumDatabaseHelper =
                AlbumDatabaseHelper(context)
            val albumList = albumDatabaseHelper.getAlbums(skip.toLong(), 50)

            if (!forceReload && albumList.isNotEmpty()) {
                finishedCallback(forceReload, skip, displayLoading)
                return@BackgroundAsync false
            } else {
                displayLoading()
                return@BackgroundAsync true
            }
        }

        return@BackgroundAsync false
    }, {
        contextReference.get()?.let { context ->
            var success = true
            val syncObject = Object()

            val requestQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            requestQueue.add(newLoadAlbumListRequest(context, skip, {
                val jsonArray = it.getJSONArray("results")

                for (i in (0 until jsonArray.length())) {
                    parseAlbumToDB(jsonArray.getJSONObject(i), context)
                }

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }, {
                success = false
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }))

            synchronized(syncObject) {
                syncObject.wait()

                return@BackgroundAsync success
            }

        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(forceReload, skip, displayLoading)
        } else {
            errorCallback(forceReload, skip, displayLoading)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun loadManualPlaylistAsync(
    context: Context,
    playlistId: String,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            var success = true
            val syncObject = Object()

            val getManualPlaylistsRequestQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            getManualPlaylistsRequestQueue.add(newLoadPlaylistRequest(context, playlistId, {
                parsePlaylistToDB(
                    context,
                    it,
                    false
                )

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }, {
                success = false
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }))

            synchronized(syncObject) {
                syncObject.wait()
                return@BackgroundAsync success
            }
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback()
        } else {
            errorCallback()
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun loadPlaylistAsync(
    context: Context,
    forceReload: Boolean,
    loadManual: Boolean,
    displayLoading: () -> Unit,
    finishedCallback: (forceReload: Boolean, displayLoading: () -> Unit) -> Unit,
    errorCallback: (forceReload: Boolean, displayLoading: () -> Unit) -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            val playlistDatabaseHelper =
                PlaylistDatabaseHelper(context)
            val playlists = playlistDatabaseHelper.getAllPlaylists()

            if (!forceReload && playlists.isNotEmpty()) {
                finishedCallback(forceReload, displayLoading)
                return@BackgroundAsync false
            } else {
                displayLoading()
                return@BackgroundAsync true
            }
        }

        return@BackgroundAsync false
    }, {
        contextReference.get()?.let { context ->
            val playlistDatabaseHelper =
                PlaylistDatabaseHelper(context)

            val playlistRequestQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            var success = true
            val syncObject = Object()

            playlistDatabaseHelper.reCreateTable(context, false)

            playlistRequestQueue.add(newLoadPlaylistsRequest(context, {
                try {
                    val jsonArray = it.getJSONArray("results")

                    for (i in (0 until jsonArray.length())) {
                        parsePlaylistToDB(
                            context,
                            jsonArray.getJSONObject(i),
                            true
                        )
                    }
                } catch (e: JSONException) {

                }

                if (loadManual) {
                    val manualPlaylists = ManualPlaylistDatabaseHelper(context).getAllPlaylists()

                    //LOAD MANUAL ADDED PLAYLISTS
                    val taskList = ArrayList<LoadManualPlaylist>()
                    var i = 0

                    for (playlist in manualPlaylists) {
                        taskList.add(LoadManualPlaylist(contextReference, playlist.playlistId, {
                            try {
                                val task = taskList[i]
                                i++
                                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                            } catch (e: IndexOutOfBoundsException) {
                                synchronized(syncObject) {
                                    syncObject.notify()
                                }
                            }
                        }, {
                            success = false

                            synchronized(syncObject) {
                                syncObject.notify()
                            }
                        }))
                    }

                    try {
                        val task = taskList[i]
                        i++
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    } catch (e: IndexOutOfBoundsException) {
                        synchronized(syncObject) {
                            syncObject.notify()
                        }
                    }
                } else {
                    synchronized(syncObject) {
                        syncObject.notify()
                    }
                }
            }, {
                success = false
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }))

            synchronized(syncObject) {
                syncObject.wait()

                return@BackgroundAsync success
            }
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(forceReload, displayLoading)
        } else {
            errorCallback(forceReload, displayLoading)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun loadPlaylistTracksAsync(
    context: Context,
    forceReload: Boolean,
    playlistId: String,
    displayLoading: () -> Unit,
    finishedCallback: (forceReload: Boolean, playlistId: String, displayLoading: () -> Unit) -> Unit,
    errorCallback: (forceReload: Boolean, playlistId: String, displayLoading: () -> Unit) -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            val playlistItemDatabaseHelper =
                PlaylistItemDatabaseHelper(
                    context,
                    playlistId
                )
            val playlistItems = playlistItemDatabaseHelper.getAllData()

            if (!forceReload && playlistItems.isNotEmpty()) {
                finishedCallback(forceReload, playlistId, displayLoading)
                return@BackgroundAsync false
            } else {
                displayLoading()
                return@BackgroundAsync true
            }
        }

        return@BackgroundAsync false
    }, {
        contextReference.get()?.let { context ->
            val playlistItemDatabaseHelper =
                PlaylistItemDatabaseHelper(
                    context,
                    playlistId
                )

            val syncObject = Object()

            val jsonObjectList = ArrayList<JSONObject?>()

            var success = true

            var skip = 0
            var nextEmpty = false

            val trackRequestQueue =
                newAuthorizedRequestQueue(
                    context,
                    context.getString(R.string.connectApiHost)
                ) { requestQueue ->
                    if (!nextEmpty && success) {
                        skip += 50

                        requestQueue.add(newLoadPlaylistTracksRequest(context, playlistId, skip, {
                            val jsonArray = it.getJSONArray("results")

                            for (k in (0 until jsonArray.length())) {
                                jsonObjectList.add(jsonArray.getJSONObject(k))
                            }

                            nextEmpty = jsonArray.length() != 50
                        }, { success = false }))
                    } else {
                        synchronized(syncObject) {
                            syncObject.notify()
                        }
                    }
                }

            trackRequestQueue.add(newLoadPlaylistTracksRequest(context, playlistId, skip, {
                val jsonArray = it.getJSONArray("results")

                for (k in (0 until jsonArray.length())) {
                    jsonObjectList.add(jsonArray.getJSONObject(k))
                }

                nextEmpty = jsonArray.length() != 50
            }, {
                success = false
            }))

            synchronized(syncObject) {
                syncObject.wait()

                if (success) {
                    playlistItemDatabaseHelper.reCreateTable()

                    for (playlistObject in jsonObjectList) {
                        if (playlistObject != null) {
                            parsePlaylistTrackToDB(
                                playlistId,
                                playlistObject,
                                context
                            )
                        }

                    }
                }

                return@BackgroundAsync success
            }
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(forceReload, playlistId, displayLoading)
        } else {
            errorCallback(forceReload, playlistId, displayLoading)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun loadRelatedTracksAsync(
    context: Context,
    trackIdArray: ArrayList<String>,
    skipMC: Boolean,
    finishedCallback: (trackIdArray: ArrayList<String>, relatedIdArray: ArrayList<String>) -> Unit,
    errorCallback: (trackIdArray: ArrayList<String>) -> Unit
) {

    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            val syncObject = Object()

            var result: ArrayList<String>? = ArrayList()

            val volleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            newLoadRelatedTracksRequest(context, trackIdArray, skipMC, {
                try {
                    val relatedJsonArray = it.getJSONArray("results")

                    for (i in (0 until relatedJsonArray.length())) {
                        val trackObject = relatedJsonArray.getJSONObject(i)
                        result?.add(trackObject.getString("id"))
                    }
                } catch (e: JSONException) {
                    result = null
                }

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }, {
                result = null
                synchronized(syncObject) {
                    syncObject.notify()
                }
            })?.let {
                volleyQueue.add(it)
            }

            synchronized(syncObject) {
                syncObject.wait()

                return@BackgroundAsync result
            }
        }

        return@BackgroundAsync null
    }, {
        if (it != null) {
            finishedCallback(trackIdArray, it)
        } else {
            errorCallback(trackIdArray)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun loadSongListAsync(
    context: Context,
    forceReload: Boolean,
    skip: Int,
    displayLoading: () -> Unit,
    finishedCallback: (forceReload: Boolean, skip: Int, displayLoading: () -> Unit) -> Unit,
    errorCallback: (forceReload: Boolean, skip: Int, displayLoading: () -> Unit) -> Unit
) {
    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            val catalogSongDatabaseHelper =
                CatalogSongDatabaseHelper(context)
            val songIdList = catalogSongDatabaseHelper.getSongs(skip.toLong(), 50)

            if (!forceReload && songIdList.isNotEmpty()) {
                finishedCallback(forceReload, skip, displayLoading)
                return@BackgroundAsync false
            } else {
                displayLoading()
                return@BackgroundAsync true
            }
        }

        return@BackgroundAsync false
    }, {
        contextReference.get()?.let { context ->
            var success = true
            val syncObject = Object()

            val requestQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            requestQueue.add(newLoadSongListRequest(context, skip, {
                val jsonArray = it.getJSONArray("results")

                for (i in (0 until jsonArray.length())) {
                    parseCatalogSongToDB(
                        jsonArray.getJSONObject(i),
                        context
                    )
                }

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }, {
                success = false
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }))

            synchronized(syncObject) {
                syncObject.wait()

                return@BackgroundAsync success
            }
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(forceReload, skip, displayLoading)
        } else {
            errorCallback(forceReload, skip, displayLoading)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun loadTitleSearchAsync(
    context: Context,
    searchString: String,
    skip: Int,
    finishedCallback: (searchString: String, skip: Int, searchResults: ArrayList<CatalogItem>) -> Unit,
    errorCallback: (searchString: String, skip: Int) -> Unit
) {

    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            var searchResults: ArrayList<CatalogItem>? = ArrayList()

            val syncObject = Object()
            val searchQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            newSearchTrackRequest(context, searchString, skip, false, {
                val jsonArray = it.getJSONArray("results")

                val songList =
                    parseSongSearchToSongList(context, jsonArray)

                for (song in songList) {
                    searchResults?.add(
                        CatalogItem(
                            song.songId
                        )
                    )
                }

                synchronized(syncObject) {
                    syncObject.notify()
                }
            }, {
                searchResults = null
                synchronized(syncObject) {
                    syncObject.notify()
                }
            })?.let {
                searchQueue.add(it)

                synchronized(syncObject) {
                    syncObject.wait()

                    return@BackgroundAsync searchResults
                }
            }
        }

        return@BackgroundAsync null
    }, {
        if (it != null) {
            finishedCallback(searchString, skip, it)
        } else {
            errorCallback(searchString, skip)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}

fun renamePlaylistAsync(
    context: Context,
    playlistId: String,
    playlistName: String,
    finishedCallback: (playlistName: String, playlistId: String) -> Unit,
    errorCallback: (playlistName: String, playlistId: String) -> Unit
) {

    val contextReference = WeakReference(context)

    BackgroundAsync({
        contextReference.get()?.let { context ->
            val newPlaylistVolleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            var success = true
            val syncObject = Object()

            newPlaylistVolleyQueue.add(
                newRenamePlaylistRequest(
                    context,
                    playlistId,
                    playlistName,
                    {
                        synchronized(syncObject) {
                            syncObject.notify()
                        }
                    },
                    {
                        success = false
                        synchronized(syncObject) {
                            syncObject.notify()
                        }
                    })
            )

            synchronized(syncObject) {
                syncObject.wait()

                return@BackgroundAsync success
            }
        }

        return@BackgroundAsync false
    }, {
        if (it == true) {
            finishedCallback(playlistName, playlistId)
        } else {
            errorCallback(playlistName, playlistId)
        }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
}