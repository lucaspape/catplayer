package de.lucaspape.monstercat.util

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.*
import de.lucaspape.monstercat.database.objects.Song
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.IndexOutOfBoundsException
import java.lang.reflect.InvocationTargetException

fun parseSongSearchToSongList(context: Context, jsonArray: JSONArray): ArrayList<Song> {
    val songList = ArrayList<Song>()

    for (i in (0 until jsonArray.length())) {
        val jsonObject = jsonArray.getJSONObject(i)

        val songId = parseSongToDB(jsonObject, context)

        songId?.let { it ->
            val databaseHelper = SongDatabaseHelper(context)
            databaseHelper.getSong(context, it)?.let {
                songList.add(it)
            }
        }
    }

    return songList
}

fun parseSongToDB(jsonObject: JSONObject, context: Context): String? {
    var id = ""
    var albumId = ""
    var albumMcId = ""
    var title = ""
    var artist = ""
    var artistId = ""
    var coverUrl = ""
    var version = ""
    var downloadable = false
    var streamable = false
    var inEarlyAccess = false
    var creatorFriendly = false

    try {
        albumId = jsonObject.getJSONObject("release").getString("id")
        albumMcId = jsonObject.getJSONObject("release").getString("catalogId")
        title = jsonObject.getString("title")
        artist = jsonObject.getString("artistsTitle")

        try{
            try {
                artistId = jsonObject.getJSONArray("artists").getJSONObject(0).getString("id")
            }catch (e: IndexOutOfBoundsException){

            }
        }catch (e: JSONException){

        }

        coverUrl = context.getString(R.string.trackContentUrl) + "$albumId/cover"
        version = jsonObject.getString("version")
        id = jsonObject.getString("id")

        try{
            downloadable = jsonObject.getBoolean("downloadable")
            streamable = jsonObject.getBoolean("streamable")
            inEarlyAccess = jsonObject.getBoolean("inEarlyAccess")
            creatorFriendly = jsonObject.getBoolean("creatorFriendly")
        }catch (e: JSONException){

        }
    } catch (e: InvocationTargetException) {
    }

    if (version == "null") {
        version = ""
    }

    val databaseHelper = SongDatabaseHelper(context)

    return databaseHelper.insertSong(
        context,
        id,
        title,
        version,
        albumId,
        albumMcId,
        artist,
        artistId,
        coverUrl,
        downloadable,
        streamable,
        inEarlyAccess,
        creatorFriendly
    )
}

fun parseCatalogSongToDB(jsonObject: JSONObject, context: Context): Long? {
    val songId = parseSongToDB(jsonObject, context)

    val catalogSongDatabaseHelper = CatalogSongDatabaseHelper(context)

    songId?.let {
        return if (catalogSongDatabaseHelper.getCatalogSong(it) == null) {
            catalogSongDatabaseHelper.insertSong(it)
        } else {
            catalogSongDatabaseHelper.getCatalogSong(it)?.id?.toLong()
        }
    }

    return null
}

fun parseAlbumSongToDB(jsonObject: JSONObject, sAlbumId: String, context: Context): String? {
    val songId = parseSongToDB(jsonObject, context)

    val albumItemDatabaseHelper = AlbumItemDatabaseHelper(context, sAlbumId)

    songId?.let {
        if (albumItemDatabaseHelper.getItemFromSongId(it) == null) {
            albumItemDatabaseHelper.insertSongId(it)
        } else {
            albumItemDatabaseHelper.getItemFromSongId(it)?.id
        }
    }

    return songId
}

fun parseAlbumToDB(jsonObject: JSONObject, context: Context): Long? {
    val id = jsonObject.getString("id")
    val title = jsonObject.getString("title")
    val artist = jsonObject.getString("artistsTitle")
    val coverUrl = context.getString(R.string.trackContentUrl) + "$id/cover"
    val mcID = jsonObject.getString("catalogId")

    val databaseHelper = AlbumDatabaseHelper(context)
    return if (databaseHelper.getAlbum(id) == null) {
        databaseHelper.insertAlbum(id, title, artist, coverUrl, mcID)
    } else {
        databaseHelper.getAlbum(id)?.id?.toLong()
    }
}

fun parsePlaylistToDB(context: Context, jsonObject: JSONObject, ownPlaylist:Boolean): Long? {
    val playlistName = jsonObject.getString("name") as String
    val playlistId = jsonObject.getString("id") as String
    val public = jsonObject.getBoolean("public")

    val databaseHelper = PlaylistDatabaseHelper(context)
    return if (databaseHelper.getPlaylist(playlistId) == null) {
        databaseHelper.insertPlaylist(playlistId, playlistName, ownPlaylist, public)
    } else {
        databaseHelper.getPlaylist(playlistId)?.id?.toLong()
    }
}

fun parsePlaylistTrackToDB(
    playlistId: String,
    jsonObject: JSONObject,
    context: Context
): Long? {
    val title = jsonObject.getString("title")

    if (title != "null") {
        val songId = parseSongToDB(jsonObject, context)

        val databaseHelper = PlaylistItemDatabaseHelper(
            context,
            playlistId
        )

        songId?.let {
            return databaseHelper.insertSongId(it)
        }

        return null
    } else {
        return null
    }
}