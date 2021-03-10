package de.lucaspape.monstercat.core.util

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.*
import de.lucaspape.monstercat.core.database.objects.Mood
import de.lucaspape.monstercat.core.database.objects.PublicPlaylist
import de.lucaspape.monstercat.core.database.objects.Song
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

        songId.let { it ->
            val databaseHelper = SongDatabaseHelper(context)
            databaseHelper.getSong(context, it)?.let {
                songList.add(it)
            }
        }
    }

    return songList
}

fun parseSongToDB(jsonObject: JSONObject, context: Context): String {
    var id = ""
    var albumId = ""
    var albumMcId = ""
    var title = ""
    var artist = ""
    var artistId = ""
    var version = ""
    var downloadable = false
    var streamable = false
    var inEarlyAccess = false
    var creatorFriendly = false
    var explicit = false

    try {
        albumId = jsonObject.getJSONObject("release").getString("id")
        albumMcId = jsonObject.getJSONObject("release").getString("catalogId")
        title = jsonObject.getString("title")
        artist = jsonObject.getString("artistsTitle")

        try {
            try {
                artistId = jsonObject.getJSONArray("artists").getJSONObject(0).getString("id")
            } catch (e: IndexOutOfBoundsException) {

            }
        } catch (e: JSONException) {

        }

        version = jsonObject.getString("version")
        id = jsonObject.getString("id")

        try {
            downloadable = jsonObject.getBoolean("downloadable")
            streamable = jsonObject.getBoolean("streamable")
            inEarlyAccess = jsonObject.getBoolean("inEarlyAccess")
            creatorFriendly = jsonObject.getBoolean("creatorFriendly")
            explicit = jsonObject.getBoolean("explicit")
        } catch (e: JSONException) {
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
        downloadable,
        streamable,
        inEarlyAccess,
        creatorFriendly,
        explicit
    )
}

fun parseCatalogSongToDB(jsonObject: JSONObject, context: Context): Long {
    val songId = parseSongToDB(jsonObject, context)

    val catalogSongDatabaseHelper = ItemDatabaseHelper(context, "catalog")

    return catalogSongDatabaseHelper.insertSongId(songId)
}

fun parseAlbumSongToDB(jsonObject: JSONObject, sAlbumId: String, context: Context): String {
    val songId = parseSongToDB(jsonObject, context)

    val albumItemDatabaseHelper = ItemDatabaseHelper(context, sAlbumId)

    if (albumItemDatabaseHelper.getItemFromSongId(songId) == null) {
        albumItemDatabaseHelper.insertSongId(songId)
    } else {
        albumItemDatabaseHelper.getItemFromSongId(songId)?.id
    }

    return songId
}

fun parseAlbumToDB(jsonObject: JSONObject, context: Context): Long? {
    val id = jsonObject.getString("id")
    val title = jsonObject.getString("title")
    val artist = jsonObject.getString("artistsTitle")
    val version = jsonObject.getString("version")
    val mcID = jsonObject.getString("catalogId")

    val databaseHelper = AlbumDatabaseHelper(context)
    return if (databaseHelper.getAlbum(id) == null) {
        databaseHelper.insertAlbum(id, title, artist, version, mcID)
    } else {
        databaseHelper.getAlbum(id)?.id?.toLong()
    }
}

fun parsePlaylistToDB(context: Context, jsonObject: JSONObject, ownPlaylist: Boolean): Long? {
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

    return if (title != "null") {
        val songId = parseSongToDB(jsonObject, context)

        val databaseHelper = ItemDatabaseHelper(
            context,
            playlistId
        )

        databaseHelper.insertSongId(songId)
    } else {
        null
    }
}

fun parseMoodIntoDB(context: Context, jsonObject: JSONObject): Mood? {
    val moodDatabaseHelper = MoodDatabaseHelper(context)
    
    val name = jsonObject.getString("Name")
    val id = jsonObject.getString("Id")
    val uri = jsonObject.getString("Uri")
    val coverUrl = "https://connect.monstercat.com/v2/mood/$id/tile"

    moodDatabaseHelper.insertMood(
        id,
        uri,
        coverUrl,
        name,
    )

    return moodDatabaseHelper.getMood(id)
}

fun parsePublicPlaylistIntoDB(context: Context, jsonObject: JSONObject): PublicPlaylist?{
    val publicPlaylistDatabaseHelper = PublicPlaylistDatabaseHelper(context)

    val name = jsonObject.getString("Label")
    val id = jsonObject.getString("Link").replace("mcat://playlist:", "")

    publicPlaylistDatabaseHelper.insertPlaylist(
        id,
        name
    )

    return publicPlaylistDatabaseHelper.getPlaylist(id)
}