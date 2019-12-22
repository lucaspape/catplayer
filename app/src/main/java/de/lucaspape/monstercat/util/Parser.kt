package de.lucaspape.monstercat.util

import android.content.Context
import de.lucaspape.monstercat.database.*
import de.lucaspape.monstercat.database.helper.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.InvocationTargetException

fun parseSongToHashMap(context: Context, song: Song): HashMap<String, Any?> {
    val settings = Settings(context)

    val primaryResolution = settings.getSetting("primaryCoverResolution")
    val secondaryResolution = settings.getSetting("secondaryCoverResolution")

    val hashMap = HashMap<String, Any?>()
    hashMap["title"] = song.title
    hashMap["version"] = song.version
    hashMap["id"] = song.songId
    hashMap["albumId"] = song.albumId
    hashMap["artist"] = song.artist
    hashMap["shownTitle"] = song.title + " " + song.version
    hashMap["coverUrl"] = song.coverUrl
    hashMap["coverLocation"] = context.filesDir.toString() + "/" + song.albumId + ".png"
    hashMap["primaryRes"] = primaryResolution
    hashMap["secondaryRes"] = secondaryResolution
    hashMap["primaryImage"] =
        context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution.toString()
    hashMap["secondaryImage"] =
        context.filesDir.toString() + "/" + song.albumId + ".png" + secondaryResolution.toString()

    return hashMap
}

fun parseAlbumToHashMap(context: Context, album: Album): HashMap<String, Any?> {
    val settings = Settings(context)

    val primaryResolution = settings.getSetting("primaryCoverResolution")
    val secondaryResolution = settings.getSetting("secondaryCoverResolution")

    val hashMap = HashMap<String, Any?>()
    hashMap["mcID"] = album.mcID
    hashMap["title"] = album.title
    hashMap["id"] = album.albumId
    hashMap["artist"] = album.artist
    hashMap["coverUrl"] = album.coverUrl
    hashMap["coverLocation"] = context.filesDir.toString() + "/" + album.albumId + ".png"
    hashMap["primaryRes"] = primaryResolution
    hashMap["secondaryRes"] = secondaryResolution
    hashMap["primaryImage"] =
        context.filesDir.toString() + "/" + album.albumId + ".png" + primaryResolution.toString()
    hashMap["secondaryImage"] =
        context.filesDir.toString() + "/" + album.albumId + ".png" + secondaryResolution.toString()

    return hashMap
}

fun parseSongSearchToSongList(context: Context, jsonArray: JSONArray): ArrayList<Song> {
    val songList = ArrayList<Song>()

    for (i in (0 until jsonArray.length())) {
        val jsonObject = jsonArray.getJSONObject(i)

        val songId = parseSongToDB(jsonObject, context)

        songId?.let {
            val databaseHelper = SongDatabaseHelper(context)
            songList.add(databaseHelper.getSong(it))
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
    var mcID = ""

    try {
        albumId = jsonObject.getJSONObject("release").getString("id")
        title = jsonObject.getString("title")
        artist = jsonObject.getString("artistsTitle")
        //TODO
        coverUrl =
            "https://connect.monstercat.com/v2/release/$albumId/cover"
        version = jsonObject.getString("version")
        id = jsonObject.getString("id")
    } catch (e: InvocationTargetException) {
    }

    if (version == "null") {
        version = ""
    }

    val databaseHelper = SongDatabaseHelper(context)
    return if (databaseHelper.getSong(id) == null) {
        databaseHelper.insertSong(id, title, version, albumId, artist, coverUrl)
    } else {
        databaseHelper.getSong(id)?.id?.toLong()
    }
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
    //TODO
    val coverUrl =
        "https://connect.monstercat.com/v2/release/$id/cover"

    val mcID = jsonObject.getString("catalogId")

    val databaseHelper = AlbumDatabaseHelper(context)
    return if (databaseHelper.getAlbum(id) == null) {
        databaseHelper.insertAlbum(id, title, artist, coverUrl, mcID)
    } else {
        databaseHelper.getAlbum(id)?.id?.toLong()
    }
}

fun parsePatchedPlaylist(trackArray: JSONArray, song: Song): Array<JSONObject?> {
    val patchedArray = arrayOfNulls<JSONObject>(trackArray.length() + 1)

    for (k in (0 until trackArray.length())) {
        patchedArray[k] = trackArray[k] as JSONObject
    }

    val songJsonObject = JSONObject()
    songJsonObject.put("releaseId", song.albumId)
    songJsonObject.put("trackId", song.songId)

    patchedArray[trackArray.length()] = songJsonObject

    return patchedArray
}

fun parsePlaylistItemToJSONArray(
    context: Context,
    playlistItemList: List<PlaylistItem>
): Array<JSONObject?> {
    val jsonArray = arrayOfNulls<JSONObject>(playlistItemList.size)

    val songDatabaseHelper = SongDatabaseHelper(context)

    for (i in playlistItemList.indices) {
        val song = songDatabaseHelper.getSong(playlistItemList[i].songId)

        val songJsonObject = JSONObject()
        songJsonObject.put("releaseId", song.albumId)
        songJsonObject.put("trackId", song.songId)

        jsonArray[i] = songJsonObject
    }

    return jsonArray
}

fun parsePlaylistToHashMap(playlist: Playlist): HashMap<String, Any?> {
    val playlistHashMap = HashMap<String, Any?>()
    playlistHashMap["playlistName"] = playlist.playlistName
    playlistHashMap["coverUrl"] = ""
    playlistHashMap["titles"] = null
    playlistHashMap["playlistId"] = playlist.playlistId
    playlistHashMap["type"] = "playlist"
    playlistHashMap["trackCount"] = playlist.trackCount

    return playlistHashMap
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
            return if (databaseHelper.getItemFromSongId(it) == null) {
                databaseHelper.insertSongId(it)
            } else {
                databaseHelper.getItemFromSongId(it)?.songId
            }
        }

        return null
    } else {
        return null
    }
}