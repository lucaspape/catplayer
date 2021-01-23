package de.lucaspape.monstercat.request.async

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.*
import de.lucaspape.monstercat.core.database.objects.Mood
import de.lucaspape.monstercat.core.database.objects.Song
import de.lucaspape.monstercat.core.util.*
import de.lucaspape.monstercat.request.*
import de.lucaspape.monstercat.ui.abstract_items.content.CatalogItem
import de.lucaspape.util.BackgroundTask
import de.lucaspape.monstercat.core.util.Settings
import org.json.JSONException
import org.json.JSONObject
import java.lang.IndexOutOfBoundsException

fun addToPlaylistAsync(
    context: Context,
    playlistId: String,
    songId: String,
    finishedCallback: (playlistId: String, songId: String) -> Unit,
    errorCallback: (playlistId: String, songId: String) -> Unit
) {
    val backgroundTask = object : BackgroundTask<Boolean>() {
        override suspend fun background() {
            SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                val addToPlaylistQueue =
                    getAuthorizedRequestQueue(
                        context,
                        context.getString(R.string.connectApiHost)
                    )

                addToPlaylistQueue.add(
                    newAddToPlaylistRequest(
                        context,
                        playlistId,
                        song,
                        {
                            updateProgress(true)
                        },
                        {
                            updateProgress(false)
                        })
                )

            }
        }

        override suspend fun publishProgress(value: Boolean) {
            if (value) {
                finishedCallback(playlistId, songId)
            } else {
                errorCallback(playlistId, songId)
            }
        }
    }

    backgroundTask.execute()
}

fun retrieveTrackIntoDB(
    context: Context,
    trackId: String,
    finishedCallback: (trackId: String, song: Song) -> Unit,
    errorCallback: (trackId: String) -> Unit
) {
    val backgroundTask = object : BackgroundTask<Song?>() {
        override suspend fun background() {
            val volleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            newSearchTrackRequest(context, trackId, 0, true, {
                val jsonArray = it.getJSONArray("results")

                for (i in (0 until jsonArray.length())) {
                    parseSongToDB(jsonArray.getJSONObject(i), context)
                }

                updateProgress(SongDatabaseHelper(context).getSong(context, trackId))
            }, {
                updateProgress(SongDatabaseHelper(context).getSong(context, trackId))
            })?.let {
                volleyQueue.add(it)
            }
        }

        override suspend fun publishProgress(value: Song?) {
            if (value != null) {
                finishedCallback(trackId, value)
            } else {
                errorCallback(trackId)
            }
        }
    }

    backgroundTask.execute()
}

fun changePlaylistPublicStateAsync(
    context: Context,
    playlistId: String,
    public: Boolean,
    finishedCallback: (playlistId: String, public: Boolean) -> Unit,
    errorCallback: (playlistId: String, public: Boolean) -> Unit
) {
    val backgroundTask = object : BackgroundTask<Boolean>() {
        override suspend fun background() {
            val newPlaylistVolleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            newPlaylistVolleyQueue.add(
                newChangePlaylistPublicStateRequest(
                    context,
                    playlistId,
                    public,
                    {
                        updateProgress(true)
                    },
                    {
                        updateProgress(false)
                    })
            )
        }

        override suspend fun publishProgress(value: Boolean) {
            if (value) {
                finishedCallback(playlistId, public)
            } else {
                errorCallback(playlistId, public)
            }
        }

    }

    backgroundTask.execute()
}

fun checkCustomApiFeaturesAsync(
    context: Context,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    val backgroundTask = object : BackgroundTask<Boolean>() {
        override suspend fun background() {
            val newVolleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            val settings = Settings.getSettings(context)

            settings.setBoolean(context.getString(R.string.customApiSupportsV1Setting), false)
            settings.setBoolean(context.getString(R.string.customApiSupportsV2Setting), false)

            newVolleyQueue.add(newCustomApiFeatureRequest(context, {
                try {
                    val apiVersionsArray = it.getJSONArray("api_versions")

                    for (i in (0 until apiVersionsArray.length())) {
                        when(apiVersionsArray.getString(i)){
                            "v1" -> settings.setBoolean(context.getString(R.string.customApiSupportsV1Setting), true)
                            "v2" -> settings.setBoolean(context.getString(R.string.customApiSupportsV2Setting), true)
                            "liveinfo" -> settings.setBoolean(context.getString(R.string.customApiSupportsLoadingLiveInfoSetting),true)
                            "related_songs" -> settings.setBoolean(context.getString(R.string.customApiSupportsPlayingRelatedSongsSetting),true)
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

                    updateProgress(true)
                } catch (e: JSONException) {
                    updateProgress(false)
                }
            }, {
                updateProgress(false)
            }))
        }

        override suspend fun publishProgress(value: Boolean) {
            if (value) {
                finishedCallback()
            } else {
                errorCallback()
            }
        }
    }

    backgroundTask.execute()
}

fun createPlaylistAsync(
    context: Context,
    playlistName: String,
    finishedCallback: (playlistName: String) -> Unit,
    errorCallback: (playlistName: String) -> Unit
) {
    val backgroundTask = object : BackgroundTask<Boolean>() {
        override suspend fun background() {
            val newPlaylistVolleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            newPlaylistVolleyQueue.add(newCreatePlaylistRequest(context, playlistName, {
                updateProgress(true)
            }, {
                updateProgress(false)
            }))
        }

        override suspend fun publishProgress(value: Boolean) {
            if (value) {
                finishedCallback(playlistName)
            } else {
                errorCallback(playlistName)
            }
        }

    }

    backgroundTask.execute()
}

fun deletePlaylistAsync(
    context: Context,
    playlistId: String,
    deleteRemote: Boolean,
    deleteLocal: Boolean,
    finishedCallback: (playlistId: String) -> Unit,
    errorCallback: (playlistId: String) -> Unit
) {
    val backgroundTask = object : BackgroundTask<Boolean>() {
        override suspend fun background() {
            if (deleteLocal) {
                val playlistDatabaseHelper = PlaylistDatabaseHelper(context)
                playlistDatabaseHelper.removePlaylist(playlistId)

                val manualPlaylistDatabaseHelper = ManualPlaylistDatabaseHelper(context)
                manualPlaylistDatabaseHelper.removePlaylist(playlistId)
            }

            if (deleteRemote) {
                val deletePlaylistVolleyQueue =
                    getAuthorizedRequestQueue(
                        context,
                        context.getString(R.string.connectApiHost)
                    )

                deletePlaylistVolleyQueue.add(newDeletePlaylistRequest(context, playlistId, {
                    updateProgress(true)
                }, {
                    updateProgress(false)
                }))
            } else {
                updateProgress(true)
            }
        }

        override suspend fun publishProgress(value: Boolean) {
            if (value) {
                finishedCallback(playlistId)
            } else {
                errorCallback(playlistId)
            }
        }
    }

    backgroundTask.execute()
}

fun deletePlaylistTrackAsync(
    context: Context,
    songId: String,
    playlistId: String,
    songDeleteIndex: Int,
    finishedCallback: (songId: String, playlistId: String, songDeleteIndex: Int) -> Unit,
    errorCallback: (songId: String, playlistId: String, songDeleteIndex: Int) -> Unit
) {
    val backgroundTask = object : BackgroundTask<Boolean>() {
        override suspend fun background() {
            SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                val deleteSongVolleyQueue =
                    getAuthorizedRequestQueue(
                        context,
                        context.getString(R.string.connectApiHost)
                    )

                deleteSongVolleyQueue.add(
                    newDeletePlaylistTrackRequest(
                        context,
                        playlistId,
                        song,
                        songDeleteIndex,
                        {
                            updateProgress(true)
                        },
                        {
                            updateProgress(false)
                        })
                )
            }
        }

        override suspend fun publishProgress(value: Boolean) {
            if (value) {
                finishedCallback(songId, playlistId, songDeleteIndex)
            } else {
                errorCallback(songId, playlistId, songDeleteIndex)
            }
        }
    }

    backgroundTask.execute()
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
    val albumItemDatabaseHelper =
        AlbumItemDatabaseHelper(context, albumId)
    val albumItems = albumItemDatabaseHelper.getAllData()

    if (!forceReload && albumItems.isNotEmpty()) {
        finishedCallback(forceReload, albumId, mcId, displayLoading)
    } else {
        displayLoading()
        //continue to background task

        val backgroundTask = object : BackgroundTask<Boolean>() {
            override suspend fun background() {
                val requestQueue =
                    getAuthorizedRequestQueue(
                        context,
                        context.getString(R.string.connectApiHost)
                    )

                displayLoading()

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

                    updateProgress(true)
                }, {
                    updateProgress(false)
                }))
            }

            override suspend fun publishProgress(value: Boolean) {
                if (value) {
                    finishedCallback(forceReload, albumId, mcId, displayLoading)
                } else {
                    errorCallback(forceReload, albumId, mcId, displayLoading)
                }
            }
        }

        backgroundTask.execute()
    }
}

fun loadAlbumListAsync(
    context: Context,
    forceReload: Boolean,
    skip: Int,
    displayLoading: () -> Unit,
    finishedCallback: (forceReload: Boolean, skip: Int, displayLoading: () -> Unit) -> Unit,
    errorCallback: (forceReload: Boolean, skip: Int, displayLoading: () -> Unit) -> Unit
) {
    val albumDatabaseHelper =
        AlbumDatabaseHelper(context)
    val albumList = albumDatabaseHelper.getAlbums(skip.toLong(), 50)

    if (!forceReload && albumList.isNotEmpty()) {
        finishedCallback(forceReload, skip, displayLoading)
    } else {
        displayLoading()


        val backgroundTask = object : BackgroundTask<Boolean>() {
            override suspend fun background() {
                val requestQueue =
                    getAuthorizedRequestQueue(
                        context,
                        context.getString(R.string.connectApiHost)
                    )

                requestQueue.add(newLoadAlbumListRequest(context, skip, {
                    val jsonArray = it.getJSONArray("results")

                    for (i in (0 until jsonArray.length())) {
                        parseAlbumToDB(jsonArray.getJSONObject(i), context)
                    }

                    updateProgress(true)
                }, {
                    updateProgress(false)
                }))
            }

            override suspend fun publishProgress(value: Boolean) {
                if (value) {
                    finishedCallback(forceReload, skip, displayLoading)
                } else {
                    errorCallback(forceReload, skip, displayLoading)
                }
            }
        }

        backgroundTask.execute()
    }
}

fun loadManualPlaylistAsync(
    context: Context,
    playlistId: String,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    val backgroundTask = object : BackgroundTask<Boolean>() {
        override suspend fun background() {
            val getManualPlaylistsRequestQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            getManualPlaylistsRequestQueue.add(newLoadPlaylistRequest(context, playlistId, {
                parsePlaylistToDB(
                    context,
                    it,
                    false
                )

                updateProgress(true)
            }, {
                updateProgress(false)
            }))
        }

        override suspend fun publishProgress(value: Boolean) {
            if (value) {
                finishedCallback()
            } else {
                errorCallback()
            }
        }
    }

    backgroundTask.execute()
}

fun loadPlaylistAsync(
    context: Context,
    forceReload: Boolean,
    loadManual: Boolean,
    displayLoading: () -> Unit,
    finishedCallback: (forceReload: Boolean, displayLoading: () -> Unit) -> Unit,
    errorCallback: (forceReload: Boolean, displayLoading: () -> Unit) -> Unit
) {
    val playlistDatabaseHelper =
        PlaylistDatabaseHelper(context)
    val playlists = playlistDatabaseHelper.getAllPlaylists()

    if (!forceReload && playlists.isNotEmpty()) {
        finishedCallback(forceReload, displayLoading)
    } else {
        displayLoading()
        val backgroundTask = object : BackgroundTask<Boolean>() {
            override suspend fun background() {
                val playlistRequestQueue =
                    getAuthorizedRequestQueue(
                        context,
                        context.getString(R.string.connectApiHost)
                    )

                var success = true

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
                        val manualPlaylists =
                            ManualPlaylistDatabaseHelper(context).getAllPlaylists()

                        //LOAD MANUAL ADDED PLAYLISTS
                        val taskList = ArrayList<LoadManualPlaylist>()
                        var i = 0

                        for (playlist in manualPlaylists) {
                            taskList.add(
                                LoadManualPlaylist(
                                    context,
                                    playlist.playlistId,
                                    {
                                        try {
                                            val task = taskList[i]
                                            i++
                                            task.execute()
                                        } catch (e: IndexOutOfBoundsException) {
                                            updateProgress(success)
                                        }
                                    },
                                    {
                                        success = false

                                        updateProgress(success)
                                    })
                            )
                        }

                        try {
                            val task = taskList[i]
                            i++
                            task.execute()
                        } catch (e: IndexOutOfBoundsException) {
                            updateProgress(success)
                        }
                    } else {
                        updateProgress(success)
                    }
                }, {
                    success = false
                    updateProgress(success)
                }))
            }

            override suspend fun publishProgress(value: Boolean) {
                if (value) {
                    finishedCallback(forceReload, displayLoading)
                } else {
                    errorCallback(forceReload, displayLoading)
                }
            }

        }

        backgroundTask.execute()
    }
}

fun loadPlaylistTracksAsync(
    context: Context,
    forceReload: Boolean,
    playlistId: String,
    displayLoading: () -> Unit,
    finishedCallback: (forceReload: Boolean, playlistId: String, displayLoading: () -> Unit) -> Unit,
    errorCallback: (forceReload: Boolean, playlistId: String, displayLoading: () -> Unit) -> Unit
) {
    val playlistItemDatabaseHelper =
        PlaylistItemDatabaseHelper(
            context,
            playlistId
        )
    val playlistItems = playlistItemDatabaseHelper.getAllData(true)

    if (!forceReload && playlistItems.isNotEmpty()) {
        finishedCallback(forceReload, playlistId, displayLoading)
    } else {
        displayLoading()
        val backgroundTask = object : BackgroundTask<Boolean>() {
            override suspend fun background() {
                val jsonObjectList = ArrayList<JSONObject?>()

                val trackRequestQueue =
                    getAuthorizedRequestQueue(
                        context,
                        context.getString(R.string.connectApiHost)
                    )

                trackRequestQueue.add(
                    newLoadPlaylistTracksRequest(
                        context,
                        playlistId,
                        0,
                        -1,
                        {
                            val jsonArray = it.getJSONArray("results")

                            for (k in (0 until jsonArray.length())) {
                                jsonObjectList.add(jsonArray.getJSONObject(k))
                            }

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

                            updateProgress(true)
                        },
                        { updateProgress(false) })
                )
            }

            override suspend fun publishProgress(value: Boolean) {
                if (value) {
                    finishedCallback(forceReload, playlistId, displayLoading)
                } else {
                    errorCallback(forceReload, playlistId, displayLoading)
                }
            }
        }

        backgroundTask.execute()
    }
}

fun loadRelatedTracksAsync(
    context: Context,
    trackIdArray: ArrayList<String>,
    skipMC: Boolean,
    finishedCallback: (trackIdArray: ArrayList<String>, relatedIdArray: ArrayList<String>) -> Unit,
    errorCallback: (trackIdArray: ArrayList<String>) -> Unit
) {
    val backgroundTask = object : BackgroundTask<ArrayList<String>?>() {
        override suspend fun background() {
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

                updateProgress(result)
            }, {
                updateProgress(null)
            })?.let {
                volleyQueue.add(it)
            }
        }

        override suspend fun publishProgress(value: ArrayList<String>?) {
            if (value != null) {
                finishedCallback(trackIdArray, value)
            } else {
                errorCallback(trackIdArray)
            }
        }

    }

    backgroundTask.execute()
}

fun loadSongListAsync(
    context: Context,
    forceReload: Boolean,
    skip: Int,
    displayLoading: () -> Unit,
    finishedCallback: (forceReload: Boolean, skip: Int, displayLoading: () -> Unit) -> Unit,
    errorCallback: (forceReload: Boolean, skip: Int, displayLoading: () -> Unit) -> Unit
) {
    val catalogSongDatabaseHelper =
        CatalogSongDatabaseHelper(context)
    val songIdList = catalogSongDatabaseHelper.getSongs(skip.toLong(), 50)

    if (!forceReload && songIdList.isNotEmpty()) {
        finishedCallback(forceReload, skip, displayLoading)
    } else {
        displayLoading()

        val backgroundTask = object : BackgroundTask<Boolean>() {
            override suspend fun background() {
                val requestQueue =
                    getAuthorizedRequestQueue(
                        context,
                        context.getString(R.string.connectApiHost)
                    )

                requestQueue.add(newLoadSongListRequest(context, skip, {
                    val jsonArray = it.getJSONArray("results")

                    for (i in (0 until jsonArray.length())) {
                        parseCatalogSongToDB(
                            jsonArray.getJSONObject(i),
                            context
                        )
                    }

                    updateProgress(true)
                }, {
                    updateProgress(false)
                }))
            }

            override suspend fun publishProgress(value: Boolean) {
                if (value) {
                    finishedCallback(forceReload, skip, displayLoading)
                } else {
                    errorCallback(forceReload, skip, displayLoading)
                }
            }

        }

        backgroundTask.execute()
    }
}

fun loadTitleSearchAsync(
    context: Context,
    searchString: String,
    skip: Int,
    finishedCallback: (searchString: String, skip: Int, searchResults: ArrayList<CatalogItem>) -> Unit,
    errorCallback: (searchString: String, skip: Int) -> Unit
) {
    val backgroundTask = object : BackgroundTask<ArrayList<CatalogItem>?>() {
        override suspend fun background() {
            val searchResults: ArrayList<CatalogItem>? = ArrayList()

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

                updateProgress(searchResults)
            }, {
                updateProgress(null)
            })?.let {
                searchQueue.add(it)
            }
        }

        override suspend fun publishProgress(value: ArrayList<CatalogItem>?) {
            if (value != null) {
                finishedCallback(searchString, skip, value)
            } else {
                errorCallback(searchString, skip)
            }
        }
    }

    backgroundTask.execute()
}

fun renamePlaylistAsync(
    context: Context,
    playlistId: String,
    playlistName: String,
    finishedCallback: (playlistName: String, playlistId: String) -> Unit,
    errorCallback: (playlistName: String, playlistId: String) -> Unit
) {
    val backgroundTask = object : BackgroundTask<Boolean>() {
        override suspend fun background() {
            val newPlaylistVolleyQueue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            newPlaylistVolleyQueue.add(
                newRenamePlaylistRequest(
                    context,
                    playlistId,
                    playlistName,
                    {
                        updateProgress(true)
                    },
                    {
                        updateProgress(false)
                    })
            )
        }

        override suspend fun publishProgress(value: Boolean) {
            if (value) {
                finishedCallback(playlistName, playlistId)
            } else {
                errorCallback(playlistName, playlistId)
            }
        }
    }

    backgroundTask.execute()
}

fun loadMoodsAsync(context: Context, finishedCallback: (results: ArrayList<Mood>) -> Unit, errorCallback: () -> Unit){
    val backgroundTask = object : BackgroundTask<ArrayList<Mood>?>() {
        override suspend fun background() {
            val moodResults: ArrayList<Mood>? = ArrayList()

            val queue =
                getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

            newLoadMoodsRequest(context, {
                val moodDatabaseHelper = MoodDatabaseHelper(context)

                val moods = it.keys()

                for(mood in moods){
                    val moodObject = it.getJSONObject(mood)
                    moodDatabaseHelper.insertMood(mood, moodObject.getString("image"), moodObject.getString("name"))

                    moodDatabaseHelper.getMood(mood)?.let {
                        moodResults?.add(it)
                    }
                }

                updateProgress(moodResults)
            }, {
                updateProgress(null)
            }).let {
                queue.add(it)
            }
        }

        override suspend fun publishProgress(value: ArrayList<Mood>?) {
            if (value != null) {
                finishedCallback(value)
            } else {
                errorCallback()
            }
        }
    }

    backgroundTask.execute()
}

fun loadMoodAsync(context: Context, forceReload: Boolean, moodId:String, skip:Int, limit:Int, displayLoading: () -> Unit, finishedCallback: (moodId:String) -> Unit, errorCallback: () -> Unit){
    val playlistItemDatabaseHelper =
        PlaylistItemDatabaseHelper(
            context,
            moodId
        )
    val playlistItems = playlistItemDatabaseHelper.getAllData(true)

    if (!forceReload && playlistItems.isNotEmpty()) {
        finishedCallback(moodId)
    } else {
        val backgroundTask = object : BackgroundTask<Boolean?>() {
            override suspend fun background() {
                val queue =
                    getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

                val jsonObjectList = ArrayList<JSONObject?>()

                queue.add(
                    newLoadMoodRequest(
                        context,
                        moodId,
                        skip,
                        limit,
                        {
                            val jsonArray = it.getJSONArray("results")

                            for (k in (0 until jsonArray.length())) {
                                jsonObjectList.add(jsonArray.getJSONObject(k))
                            }

                            playlistItemDatabaseHelper.reCreateTable()

                            for (playlistObject in jsonObjectList) {
                                if (playlistObject != null) {
                                    parsePlaylistTrackToDB(
                                        moodId,
                                        playlistObject,
                                        context
                                    )
                                }

                            }

                            updateProgress(true)
                        },
                        { updateProgress(false) })
                )
            }

            override suspend fun publishProgress(value: Boolean?) {
                if (value == true) {
                    finishedCallback(moodId)
                } else {
                    errorCallback()
                }
            }
        }

        backgroundTask.execute()
    }
}