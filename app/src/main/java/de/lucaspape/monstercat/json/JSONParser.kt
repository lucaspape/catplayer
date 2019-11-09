package de.lucaspape.monstercat.json

import android.content.Context
import de.lucaspape.monstercat.database.*
import de.lucaspape.monstercat.database.helper.*
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.InvocationTargetException

/**
 * Parses Stuff, yes this class is not great
 */
class JSONParser {

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

            var id = ""
            var albumId = ""
            var title = ""
            var artist = ""
            var coverUrl = ""
            var version = ""

            try {
                albumId = jsonObject.getJSONObject("albums").getString("albumId")
                title = jsonObject.getString("title")
                artist = jsonObject.getString("artistsTitle")
                coverUrl = jsonObject.getJSONObject("release").getString("coverUrl")
                version = jsonObject.getString("version")
                id = jsonObject.getString("_id")
            } catch (e: InvocationTargetException) {
            }

            if (version == "null") {
                version = ""
            }

            val songId: Long

            val databaseHelper = SongDatabaseHelper(context)
            songId = if (databaseHelper.getSong(id) == null) {
                databaseHelper.insertSong(id, title, version, albumId, artist, coverUrl)
            } else {
                databaseHelper.getSong(id)!!.id.toLong()
            }

            songList.add(databaseHelper.getSong(songId))
        }

        return songList
    }

    fun parseSongToDB(jsonObject: JSONObject, context: Context):Long{
        var id = ""
        var albumId = ""
        var title = ""
        var artist = ""
        var coverUrl = ""
        var version = ""

        try {
            albumId = jsonObject.getJSONObject("albums").getString("albumId")
            title = jsonObject.getString("title")
            artist = jsonObject.getString("artistsTitle")
            coverUrl = jsonObject.getJSONObject("release").getString("coverUrl")
            version = jsonObject.getString("version")
            id = jsonObject.getString("_id")
        } catch (e: InvocationTargetException) {
        }

        if (version == "null") {
            version = ""
        }

        val databaseHelper = SongDatabaseHelper(context)
        return if (databaseHelper.getSong(id) == null) {
            databaseHelper.insertSong(id, title, version, albumId, artist, coverUrl)
        } else {
            databaseHelper.getSong(id)!!.id.toLong()
        }
    }

    fun parseCatalogSongToDB(jsonObject: JSONObject, context: Context): Long {
        val songId = parseSongToDB(jsonObject, context)

        val catalogSongDatabaseHelper = CatalogSongDatabaseHelper(context)

        return if (catalogSongDatabaseHelper.getCatalogSong(songId) == null) {
            catalogSongDatabaseHelper.insertSong(songId)
        } else {
            catalogSongDatabaseHelper.getCatalogSong(songId)!!.id.toLong()
        }
    }

    fun parsAlbumSongToDB(jsonObject: JSONObject, sAlbumId: String, context: Context): Long {
        var id = ""
        var albumId = ""
        var title = ""
        var artist = ""
        var coverUrl = ""
        var version = ""

        try {
            albumId = jsonObject.getJSONObject("albums").getString("albumId")
            title = jsonObject.getString("title")
            artist = jsonObject.getString("artistsTitle")
            coverUrl = jsonObject.getJSONObject("release").getString("coverUrl")
            version = jsonObject.getString("version")
            id = jsonObject.getString("_id")
        } catch (e: InvocationTargetException) {
        }

        if (version == "null") {
            version = ""
        }

        val songId: Long

        val databaseHelper = SongDatabaseHelper(context)
        songId = if (databaseHelper.getSong(id) == null) {
            databaseHelper.insertSong(id, title, version, albumId, artist, coverUrl)
        } else {
            databaseHelper.getSong(id)!!.id.toLong()
        }

        val albumItemDatabaseHelper = AlbumItemDatabaseHelper(context, sAlbumId)

        return if (albumItemDatabaseHelper.getItemFromSongId(songId) == null) {
            albumItemDatabaseHelper.insertSongId(songId)
        } else {
            albumItemDatabaseHelper.getItemFromSongId(songId)!!.id
        }
    }

    fun parseAlbumToDB(jsonObject: JSONObject, context: Context): Long {
        val id = jsonObject.getString("_id")
        val title = jsonObject.getString("title")
        val artist = jsonObject.getString("renderedArtists")
        val coverUrl = jsonObject.getString("coverUrl")

        val databaseHelper = AlbumDatabaseHelper(context)
        return if (databaseHelper.getAlbum(id) == null) {
            databaseHelper.insertAlbum(id, title, artist, coverUrl)
        } else {
            databaseHelper.getAlbum(id)!!.id.toLong()
        }
    }

    fun parseObjectToStreamHash(jsonObject: JSONObject, song: Song): String? {
        val jsonArray = jsonObject.getJSONArray("results")

        // println(jsonArray)

        var streamHash = ""
        val searchSong = song.artist + song.title + song.version

        for (i in (0 until jsonArray.length())) {
            val sArtist = jsonArray.getJSONObject(i).getString("artistsTitle")
            val sTitle = jsonArray.getJSONObject(i).getString("title")
            var sVersion = jsonArray.getJSONObject(i).getString(
                "version"
            )

            if(sVersion == "null"){
               sVersion = ""
            }

            val sString = sArtist + sTitle + sVersion

            if (sString == searchSong) {
                streamHash =
                    jsonArray.getJSONObject(i).getJSONObject("albums").getString("streamHash")
            }
        }

        return if (streamHash == "") {
            null
        } else {
            streamHash
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

    fun parsePlaylistDataToJSONArray(
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

    fun parsePlaylistToDB(context: Context, jsonObject: JSONObject): Long {
        val playlistName = jsonObject.getString("name") as String
        val playlistId = jsonObject.getString("_id") as String
        val playlistTrackCount = jsonObject.getJSONArray("tracks").length()

        val databaseHelper = PlaylistDatabaseHelper(context)
        return if (databaseHelper.getPlaylist(playlistId) == null) {
            databaseHelper.insertPlaylist(playlistId, playlistName, playlistTrackCount)
        } else {
            databaseHelper.getPlaylist(playlistId)!!.id.toLong()
        }
    }

    fun parsePlaylistTrackToDB(
        playlistId: String,
        jsonObject: JSONObject,
        context: Context
    ): Long? {
        val title = jsonObject.getString("title")

        if (title != "null") {
            var version = jsonObject.getString("version")
            val artist = jsonObject.getString("artistsTitle")
            val coverUrl = jsonObject.getJSONObject("release").getString("coverUrl")
            val id = jsonObject.getString("_id")
            val albumId = jsonObject.getJSONObject("albums").getString("albumId")

            if (version == "null") {
                version = ""
            }

            val songDatabaseHelper =
                SongDatabaseHelper(context)

            val songId: Long

            songId = if (songDatabaseHelper.getSong(id) == null) {
                songDatabaseHelper.insertSong(id, title, version, albumId, artist, coverUrl)
            } else {
                songDatabaseHelper.getSong(id)!!.id.toLong()
            }

            val databaseHelper = PlaylistItemDatabaseHelper(
                context,
                playlistId
            )

            return if (databaseHelper.getItemFromSongId(songId) == null) {
                databaseHelper.insertSongId(songId)
            } else {
                databaseHelper.getItemFromSongId(songId)!!.songId
            }
        } else {
            return null
        }
    }

}