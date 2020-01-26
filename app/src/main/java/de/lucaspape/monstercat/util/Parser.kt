package de.lucaspape.monstercat.util

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.*
import de.lucaspape.monstercat.database.helper.*
import de.lucaspape.monstercat.handlers.abstract_items.AlbumItem
import de.lucaspape.monstercat.handlers.abstract_items.CatalogItem
import de.lucaspape.monstercat.handlers.abstract_items.PlaylistItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.reflect.InvocationTargetException

fun parseSongToAbstractCatalogItem(song: Song):CatalogItem{
    val songDownloadStatus:String = when {
        File(song.downloadLocation).exists() -> {
            "android.resource://de.lucaspape.monstercat/drawable/ic_check_green_24dp"
        }
        File(song.streamDownloadLocation).exists() -> {
            "android.resource://de.lucaspape.monstercat/drawable/ic_check_orange_24dp"
        }
        else -> {
            "android.resource://de.lucaspape.monstercat/drawable/ic_empty_24dp"
        }
    }

    return CatalogItem(song.title, song.version, song.artist, song.songId, song.albumId, song.isDownloadable, song.isStreamable, song.inEarlyAccess,songDownloadStatus)
}

fun parsePlaylistToAbstractPlaylistItem(context: Context, playlist: Playlist):PlaylistItem{
    val playlistTracks = PlaylistItemDatabaseHelper(context, playlist.playlistId).getAllData()

    var downloaded = true
    var streamDownloaded = true

    for(track in playlistTracks){
        val song = SongDatabaseHelper(context).getSong(context, track.songId)

        if(!File(song.downloadLocation).exists()){
            downloaded = false
        }else if(!File(song.streamDownloadLocation).exists()){
            streamDownloaded = false
        }
    }

    val playlistDownloadStatus = when {
        downloaded -> {
            "android.resource://de.lucaspape.monstercat/drawable/ic_check_green_24dp"
        }
        streamDownloaded -> {
            "android.resource://de.lucaspape.monstercat/drawable/ic_check_orange_24dp"
        }
        else -> {
            "android.resource://de.lucaspape.monstercat/drawable/ic_empty_24dp"
        }
    }

    return PlaylistItem(playlist.playlistName, playlist.playlistId, "", playlistDownloadStatus)
}

fun parseAlbumToAbstractAlbumItem(album: Album):AlbumItem{
    return AlbumItem(album.title, album.artist, album.mcID, album.albumId)
}

fun parseSongSearchToSongList(context: Context, jsonArray: JSONArray): ArrayList<Song> {
    val songList = ArrayList<Song>()

    for (i in (0 until jsonArray.length())) {
        val jsonObject = jsonArray.getJSONObject(i)

        val songId = parseSongToDB(jsonObject, context)

        songId?.let {
            val databaseHelper = SongDatabaseHelper(context)
            songList.add(databaseHelper.getSong(context, it))
        }
    }

    return songList
}

fun parseSongToDB(jsonObject: JSONObject, context: Context): Long? {
    var id = ""
    var albumId = ""
    var title = ""
    var artist = ""
    var coverUrl = ""
    var version = ""
    var downloadable = false
    var streamable = false
    var inEarlyAccess = false

    try {
        albumId = jsonObject.getJSONObject("release").getString("id")
        title = jsonObject.getString("title")
        artist = jsonObject.getString("artistsTitle")
        coverUrl = context.getString(R.string.trackContentUrl) + "$albumId/cover"
        version = jsonObject.getString("version")
        id = jsonObject.getString("id")
        downloadable = jsonObject.getBoolean("downloadable")
        streamable = jsonObject.getBoolean("streamable")
        inEarlyAccess = jsonObject.getBoolean("inEarlyAccess")
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
        artist,
        coverUrl,
        downloadable,
        streamable,
        inEarlyAccess
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

fun parsAlbumSongToDB(jsonObject: JSONObject, sAlbumId: String, context: Context): Long? {
    val songId = parseSongToDB(jsonObject, context)

    val albumItemDatabaseHelper = AlbumItemDatabaseHelper(context, sAlbumId)

    songId?.let {
        return if (albumItemDatabaseHelper.getItemFromSongId(it) == null) {
            albumItemDatabaseHelper.insertSongId(it)
        } else {
            albumItemDatabaseHelper.getItemFromSongId(it)?.id
        }
    }

    return null
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

fun parsePlaylistToDB(context: Context, jsonObject: JSONObject): Long? {
    val playlistName = jsonObject.getString("name") as String
    val playlistId = jsonObject.getString("_id") as String
    val playlistTrackCount = jsonObject.getJSONArray("tracks").length()

    val databaseHelper = PlaylistDatabaseHelper(context)
    return if (databaseHelper.getPlaylist(playlistId) == null) {
        databaseHelper.insertPlaylist(playlistId, playlistName, playlistTrackCount)
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