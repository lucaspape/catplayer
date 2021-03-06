package de.lucaspape.monstercat.request.async

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.*
import de.lucaspape.monstercat.core.database.objects.Genre
import de.lucaspape.monstercat.core.database.objects.Mood
import de.lucaspape.monstercat.core.database.objects.PublicPlaylist
import de.lucaspape.monstercat.core.music.util.*
import de.lucaspape.monstercat.core.util.*
import de.lucaspape.monstercat.request.*
import de.lucaspape.monstercat.core.util.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.collections.ArrayList

suspend fun addToPlaylist(
    context: Context,
    playlistId: String,
    songId: String,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        SongDatabaseHelper(context).getSong(songId)?.let { song ->
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
                        finishedCallback()
                    },
                    {
                        errorCallback()
                    })
            )

        }
    }
}

suspend fun changePlaylistPublicState(
    context: Context,
    playlistId: String,
    public: Boolean,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val newPlaylistVolleyQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

        newPlaylistVolleyQueue.add(
            newChangePlaylistPublicStateRequest(
                context,
                playlistId,
                public,
                {
                    finishedCallback()
                },
                {
                    errorCallback()
                })
        )
    }
}

suspend fun checkCustomApiFeatures(
    context: Context,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val newVolleyQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

        val settings = Settings.getSettings(context)

        newVolleyQueue.add(newCustomApiFeatureRequest(context, {
            try {
                settings.setBoolean(context.getString(R.string.customApiSupportsV1Setting), false)
                settings.setBoolean(context.getString(R.string.customApiSupportsV2Setting), false)
                settings.setBoolean(
                    context.getString(R.string.customApiSupportsPlayingRelatedSongsSetting),
                    false
                )

                val apiVersionsArray = it.getJSONArray("api_versions")

                for (i in (0 until apiVersionsArray.length())) {
                    when (apiVersionsArray.getString(i)) {
                        "v1" -> settings.setBoolean(
                            context.getString(R.string.customApiSupportsV1Setting),
                            true
                        )
                        "v2" -> settings.setBoolean(
                            context.getString(R.string.customApiSupportsV2Setting),
                            true
                        )
                        "related_songs" -> settings.setBoolean(
                            context.getString(R.string.customApiSupportsPlayingRelatedSongsSetting),
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

                finishedCallback()
            } catch (e: JSONException) {
                errorCallback()
            }
        }, {
            settings.setBoolean(context.getString(R.string.customApiSupportsV1Setting), false)
            settings.setBoolean(context.getString(R.string.customApiSupportsV2Setting), false)
            settings.setBoolean(
                context.getString(R.string.customApiSupportsPlayingRelatedSongsSetting),
                false
            )

            errorCallback()
        }))
    }
}

suspend fun createPlaylist(
    context: Context,
    playlistName: String,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val newPlaylistVolleyQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

        newPlaylistVolleyQueue.add(newCreatePlaylistRequest(context, playlistName, {
            finishedCallback()
        }, {
            errorCallback()
        }))
    }
}

suspend fun deletePlaylist(
    context: Context,
    playlistId: String,
    deleteRemote: Boolean,
    deleteLocal: Boolean,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
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
                finishedCallback()
            }, {
                errorCallback()
            }))
        } else {
            errorCallback()
        }
    }
}

suspend fun deletePlaylistTrack(
    context: Context,
    songId: String,
    playlistId: String,
    songDeleteIndex: Int,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        SongDatabaseHelper(context).getSong(songId)?.let { song ->
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
                        finishedCallback()
                    },
                    {
                        errorCallback()
                    })
            )
        }
    }
}

suspend fun loadAlbum(
    context: Context,
    forceReload: Boolean,
    albumId: String,
    mcId: String,
    displayLoading: () -> Unit,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val albumItemDatabaseHelper =
            ItemDatabaseHelper(context, albumId)
        val albumItems = albumItemDatabaseHelper.getAllData(false)

        if (!forceReload && albumItems.isNotEmpty()) {
            finishedCallback()
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

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

                finishedCallback()
            }, {
                errorCallback()
            }))
        }
    }
}

suspend fun loadAlbumList(
    context: Context,
    forceReload: Boolean,
    skip: Int,
    displayLoading: () -> Unit,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val albumDatabaseHelper =
            AlbumDatabaseHelper(context)
        val albumList = albumDatabaseHelper.getAlbums(skip.toLong(), 50)

        if (!forceReload && albumList.isNotEmpty()) {
            finishedCallback()
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

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

                finishedCallback()
            }, {
                errorCallback()
            }))
        }
    }
}

suspend fun loadPlaylists(
    context: Context,
    forceReload: Boolean,
    loadManual: Boolean,
    displayLoading: () -> Unit,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val playlistDatabaseHelper =
            PlaylistDatabaseHelper(context)
        val playlists = playlistDatabaseHelper.getAllPlaylists()

        if (!forceReload && playlists.isNotEmpty()) {
            finishedCallback()
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

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
                                        if (success) {
                                            finishedCallback()
                                        } else {
                                            errorCallback()
                                        }
                                    }
                                },
                                {
                                    success = false

                                    errorCallback()
                                })
                        )
                    }

                    try {
                        val task = taskList[i]
                        i++
                        task.execute()
                    } catch (e: IndexOutOfBoundsException) {
                        if (success) {
                            finishedCallback()
                        } else {
                            errorCallback()
                        }
                    }
                } else {
                    if (success) {
                        finishedCallback()
                    } else {
                        errorCallback()
                    }
                }
            }, {
                success = false
                if (success) {
                    finishedCallback()
                } else {
                    errorCallback()
                }
            }))
        }
    }
}

suspend fun loadPlaylistTracks(
    context: Context,
    forceReload: Boolean,
    playlistId: String,
    displayLoading: () -> Unit,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val playlistItemDatabaseHelper =
            ItemDatabaseHelper(
                context,
                playlistId
            )
        val playlistItems = playlistItemDatabaseHelper.getAllData(true)

        if (!forceReload && playlistItems.isNotEmpty()) {
            finishedCallback()
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

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

                        finishedCallback()
                    },
                    { errorCallback() })
            )
        }
    }
}

suspend fun loadRelatedTracks(
    context: Context,
    trackIdArray: ArrayList<String>,
    excludeIdArray: ArrayList<String>,
    finishedCallback: (relatedIdArray: ArrayList<String>?) -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val result: ArrayList<String> = ArrayList()

        val volleyQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

        newLoadRelatedTracksRequest(context, trackIdArray, excludeIdArray, {
            try {
                val relatedJsonArray = it.getJSONArray("results")

                for (i in (0 until relatedJsonArray.length())) {
                    val trackObject = relatedJsonArray.getJSONObject(i)

                    parseSongToDB(
                        trackObject,
                        context
                    )

                    result.add(trackObject.getString("id"))
                }
            } catch (e: JSONException) {
                errorCallback()
            }

            finishedCallback(result)
        }, {
            errorCallback()
        })?.let {
            volleyQueue.add(it)
        }
    }
}

suspend fun loadSongList(
    context: Context,
    forceReload: Boolean,
    skip: Int,
    displayLoading: () -> Unit,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    val catalogSongDatabaseHelper =
        ItemDatabaseHelper(context, "catalog")
    val songIdList = catalogSongDatabaseHelper.getItems(skip.toLong(), 50)

    if (!forceReload && songIdList.isNotEmpty()) {
        finishedCallback()
    } else {
        withContext(Dispatchers.Main) {
            displayLoading()
        }

        withContext(Dispatchers.Default) {
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

                finishedCallback()
            }, {
                errorCallback()
            }))
        }
    }
}

suspend fun loadTitleSearch(
    context: Context,
    searchString: String,
    skip: Int,
    finishedCallback: (searchResults: ArrayList<String>) -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val searchResults: ArrayList<String> = ArrayList()

        val searchQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

        val searchTrackRequest = newSearchTrackRequest(context, searchString, skip, {
            val jsonArray = it.getJSONArray("results")

            val songList =
                parseSongSearchToSongList(context, jsonArray)

            for (song in songList) {
                searchResults.add(
                    song.songId
                )
            }

            finishedCallback(searchResults)
        }, {
            errorCallback()
        })

        if (searchTrackRequest != null) {
            searchQueue.add(searchTrackRequest)
        } else {
            errorCallback()
        }
    }

}

suspend fun renamePlaylist(
    context: Context,
    playlistId: String,
    playlistName: String,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val newPlaylistVolleyQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

        newPlaylistVolleyQueue.add(
            newRenamePlaylistRequest(
                context,
                playlistId,
                playlistName,
                {
                    finishedCallback()
                },
                {
                    errorCallback()
                })
        )
    }
}

suspend fun loadMoods(
    context: Context,
    forceReload: Boolean,
    displayLoading: () -> Unit,
    finishedCallback: (results: ArrayList<Mood>) -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val moodDatabaseHelper = MoodDatabaseHelper(context)
        val moods = moodDatabaseHelper.getAllMoods()

        if (!forceReload && moods.isNotEmpty()) {
            finishedCallback(moods.reversed() as ArrayList<Mood>)
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

            val queue =
                getAuthorizedRequestQueue(
                    context,
                    context.getString(R.string.connectApiHost)
                )

            moodDatabaseHelper.reCreateTable()

            newLoadMoodsRequest(context, { jsonObject ->
                val moodsArray = jsonObject.getJSONObject("Moods").getJSONArray("results")

                for (i in (0 until moodsArray.length())) {
                    parseMoodIntoDB(
                        context,
                        moodsArray.getJSONObject(i)
                    )?.let {
                        moods.add(it)
                    }
                }

                finishedCallback(moods)
            }, {
                errorCallback()
            }).let {
                queue.add(it)
            }
        }
    }
}

suspend fun loadMood(
    context: Context,
    forceReload: Boolean,
    moodId: String,
    skip: Int,
    limit: Int,
    displayLoading: () -> Unit,
    finishedCallback: (moodId: String) -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val playlistItemDatabaseHelper =
            ItemDatabaseHelper(
                context,
                moodId
            )
        val playlistItems = playlistItemDatabaseHelper.getAllData(true)

        if (!forceReload && playlistItems.isNotEmpty()) {
            finishedCallback(moodId)
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

            val queue =
                getAuthorizedRequestQueue(
                    context,
                    context.getString(R.string.connectApiHost)
                )

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

                        finishedCallback(moodId)
                    },
                    { errorCallback() })
            )
        }
    }
}

suspend fun loadGenres(
    context: Context,
    forceReload: Boolean, displayLoading: () -> Unit,
    finishedCallback: (results: ArrayList<Genre>) -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val genreDatabaseHelper =
            GenreDatabaseHelper(
                context
            )

        val genres = genreDatabaseHelper.getAllGenres()

        if (!forceReload && genres.isNotEmpty()) {
            finishedCallback(genres.reversed() as ArrayList<Genre>)
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

            val queue =
                getAuthorizedRequestQueue(
                    context,
                    context.getString(R.string.connectApiHost)
                )

            genreDatabaseHelper.reCreateTable()

            queue.add(
                newLoadFiltersRequest(
                    context,
                    {
                        val jsonArray = it.getJSONArray("genres")

                        for (k in (0 until jsonArray.length())) {
                            val genreId = UUID.randomUUID().toString()

                            genreDatabaseHelper.insertGenre(
                                genreId,
                                jsonArray.getString(k)
                            )

                            genreDatabaseHelper.getGenre(genreId)?.let { genre ->
                                genres.add(genre)
                            }
                        }

                        finishedCallback(genres)
                    },
                    { errorCallback() })
            )
        }
    }
}

suspend fun loadGenre(
    context: Context,
    forceReload: Boolean,
    genreName: String,
    genreId: String,
    skip: Int,
    limit: Int,
    displayLoading: () -> Unit,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val playlistItemDatabaseHelper =
            ItemDatabaseHelper(
                context,
                genreName
            )
        val playlistItems = playlistItemDatabaseHelper.getAllData(true)

        if (!forceReload && playlistItems.isNotEmpty()) {
            finishedCallback()
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

            val queue =
                getAuthorizedRequestQueue(
                    context,
                    context.getString(R.string.connectApiHost)
                )

            val jsonObjectList = ArrayList<JSONObject?>()

            queue.add(
                newLoadGenreRequest(
                    context,
                    genreName,
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
                                    genreId,
                                    playlistObject,
                                    context
                                )
                            }

                        }

                        finishedCallback()
                    },
                    { errorCallback() })
            )
        }
    }
}

suspend fun loadGreatestHits(
    context: Context,
    forceReload: Boolean,
    skip: Int,
    limit: Int,
    displayLoading: () -> Unit,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val playlistItemDatabaseHelper =
            ItemDatabaseHelper(
                context,
                "greatest-hits"
            )
        val playlistItems = playlistItemDatabaseHelper.getAllData(true)

        if (!forceReload && playlistItems.isNotEmpty()) {
            finishedCallback()
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

            val queue =
                getAuthorizedRequestQueue(
                    context,
                    context.getString(R.string.connectApiHost)
                )

            val jsonObjectList = ArrayList<JSONObject?>()

            queue.add(
                newLoadGreatestHitsRequest(
                    context,
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
                                    "greatest-hits",
                                    playlistObject,
                                    context
                                )
                            }

                        }

                        finishedCallback()
                    },
                    { errorCallback() })
            )
        }
    }
}

suspend fun loadLiveStreams(
    context: Context,
    forceReload: Boolean, displayLoading: () -> Unit,
    finishedCallback: () -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val streamDatabaseHelper = StreamDatabaseHelper(context)

        val streams = streamDatabaseHelper.getAllStreams()

        if (!forceReload && streams.isNotEmpty()) {
            finishedCallback()
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

            val queue =
                getAuthorizedRequestQueue(
                    context,
                    context.getString(R.string.connectApiHost)
                )

            val request = newLoadLivestreamsRequest(context,
                {
                    val songDatabaseHelper = SongDatabaseHelper(context)
                    val results = it.getJSONObject("results")

                    val streamIds = results.keys()

                    for (id in streamIds) {
                        val streamObject = results.getJSONObject(id)

                        val name = streamObject.getString("name")
                        val streamUrl = streamObject.getString("url")

                        streamDatabaseHelper.insertStream(streamUrl, "", name)
                        songDatabaseHelper.insertSong(
                            name,
                            "Livestream - $name",
                            "",
                            "live",
                            "live",
                            "Monstercat",
                            "",
                            downloadable = false,
                            streamable = true,
                            inEarlyAccess = false,
                            creatorFriendly = false,
                            explicit = true
                        )
                    }

                    finishedCallback()
                },
                {
                    errorCallback()
                })

            if (request != null) {
                queue.add(request)
            } else {
                errorCallback()
            }
        }
    }
}

suspend fun loadPublicPlaylists(
    context: Context,
    forceReload: Boolean,
    displayLoading: () -> Unit,
    finishedCallback: (results: ArrayList<PublicPlaylist>) -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val publicPlaylistDatabaseHelper = PublicPlaylistDatabaseHelper(context)
        val playlists = publicPlaylistDatabaseHelper.getAllPlaylists()

        if (!forceReload && playlists.isNotEmpty()) {
            finishedCallback(playlists.reversed() as ArrayList<PublicPlaylist>)
        } else {
            withContext(Dispatchers.Main) {
                displayLoading()
            }

            val queue =
                getAuthorizedRequestQueue(
                    context,
                    context.getString(R.string.connectApiHost)
                )

            publicPlaylistDatabaseHelper.reCreateTable(context, false)

            newLoadPublicPlaylistsRequest(context, { jsonObject ->
                val playlistsArray =
                    jsonObject.getJSONObject("Menu").getJSONArray("Sections").getJSONObject(0)
                        .getJSONArray("Items")

                for (i in (0 until playlistsArray.length())) {
                    parsePublicPlaylistIntoDB(
                        context,
                        playlistsArray.getJSONObject(i)
                    )?.let {
                        playlists.add(it)
                    }
                }

                finishedCallback(playlists)
            }, {
                errorCallback()
            }).let {
                queue.add(it)
            }
        }
    }
}

suspend fun loadAlbumTracks(
    context: Context,
    mcID: String,
    finishedCallback: (trackIds: ArrayList<String>) -> Unit,
    errorCallback: () -> Unit
) {
    withContext(Dispatchers.Default) {
        val albumRequestQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

        albumRequestQueue.add(newLoadAlbumRequest(context, mcID, {
            parseAlbumToDB(it.getJSONObject("release"), context)

            val jsonArray = it.getJSONArray("tracks")

            val idArray = ArrayList<String>()

            AlbumDatabaseHelper(context).getAlbumFromMcId(mcID)?.let { album ->
                for (i in (0 until jsonArray.length())) {
                    idArray.add(
                        parseAlbumSongToDB(
                            jsonArray.getJSONObject(i),
                            album.albumId,
                            context
                        )
                    )
                }
            }

            finishedCallback(idArray)
        }, {
            errorCallback()
        }))
    }
}

suspend fun loadLyrics(
    context: Context,
    songId: String,
    finishedCallback: (songId:String) -> Unit,
    errorCallback: (songId:String) -> Unit
) {
    withContext(Dispatchers.Main) {
        val requestQueue =
            getAuthorizedRequestQueue(context, context.getString(R.string.connectApiHost))

        requestQueue.add(newLoadLyricsRequest(context, songId,
            {
                try {
                    val jsonObject = JSONObject(getCharacterFromUnicode(it, true))

                    val result = jsonObject.getJSONObject("result")

                    var keys = result.keys()

                    //idk how to get length, so ima do this

                    var size = 0

                    for(key in keys){
                        size++
                    }

                    val newLyricTimeCodes = Array(size){0}
                    val newLyricText = Array(size){""}

                    var index = 0

                    keys = result.keys()

                    for(key in keys){
                        newLyricTimeCodes[index] = key.toInt()
                        newLyricText[index] = result.getString(key)

                        index++
                    }

                    lyricTimeCodesArray = newLyricTimeCodes
                    lyricTextArray = newLyricText
                }catch (e: JSONException){
                    lyricTimeCodesArray = arrayOf(0)
                    lyricTextArray = arrayOf(getCharacterFromUnicode(it, true))
                }

                finishedCallback(songId)
            }, {
            errorCallback(songId)
            }))
    }
}