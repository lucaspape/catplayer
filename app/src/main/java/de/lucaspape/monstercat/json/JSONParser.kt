package de.lucaspape.monstercat.json

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.*
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.InvocationTargetException

class JSONParser {

    fun parseSongToHashMap(context: Context, song:Song):HashMap<String,Any?>{
        val settings = Settings(context)

        val primaryResolution = settings.getSetting("primaryCoverResolution")
        val secondaryResolution = settings.getSetting("secondaryCoverResolution")

        val hashMap = HashMap<String, Any?>()
        hashMap["title"] = song.title
        hashMap["version"] = song.version
        hashMap["id"] = song.songId
        hashMap["albumId"] = song.albumId
        hashMap["artist"] = song.artist
        hashMap["shownTitle"] = song.title + song.version
        hashMap["coverUrl"] = song.coverUrl
        hashMap["coverLocation"] = context.filesDir.toString() + "/" + song.albumId + ".png"
        hashMap["primaryRes"] = primaryResolution
        hashMap["secondaryRes"] = secondaryResolution
        hashMap["primaryImage"] = context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution.toString()
        hashMap["secondaryImage"] = context.filesDir.toString() + "/" + song.albumId + ".png" + secondaryResolution.toString()

        return hashMap
    }

    fun parseAlbumToHashMap(context: Context, album:Album):HashMap<String, Any?>{
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
        hashMap["primaryImage"] = context.filesDir.toString() + "/" + album.albumId + ".png" + primaryResolution.toString()
        hashMap["secondaryImage"] = context.filesDir.toString() + "/" + album.albumId + ".png" + secondaryResolution.toString()

        return hashMap
    }

    fun parseCatalogSongToDB(jsonObject: JSONObject, context: Context):Long{
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
        return if(databaseHelper.getSong(id) == null){
            databaseHelper.insertSong(id, title, version, albumId, artist, coverUrl)
        }else{
            databaseHelper.getSong(id)!!.id.toLong()
        }
    }

    fun parseAlbumToDB(jsonObject: JSONObject, context: Context): Long{
        val id = jsonObject.getString("_id")
        val title = jsonObject.getString("title")
        val artist = jsonObject.getString("renderedArtists")
        val coverUrl= jsonObject.getString("coverUrl")

        val databaseHelper = AlbumDatabaseHelper(context)
        return if(databaseHelper.getAlbum(id) == null){
            databaseHelper.insertAlbum(id, title, artist, coverUrl)
        }else{
            databaseHelper.getAlbum(id)!!.id.toLong()
        }
    }

    fun parseObjectToStreamHash(jsonObject: JSONObject, song: Song): String? {
        val jsonArray = jsonObject.getJSONArray("results")

        var streamHash = ""
        val searchSong = song.title + song.version

        for (i in (0 until jsonArray.length())) {
            if (jsonArray.getJSONObject(i).getString("title") + jsonArray.getJSONObject(i).getString(
                    "version"
                ) == searchSong
            ) {
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

    fun parsePatchedPlaylist(trackArray: JSONArray, songItem: HashMap<String, Any?>): Array<JSONObject?> {
        val patchedArray = arrayOfNulls<JSONObject>(trackArray.length() + 1)

        for (k in (0 until trackArray.length())) {
            patchedArray[k] = trackArray[k] as JSONObject
        }

        val songJsonObject = JSONObject()
        songJsonObject.put("releaseId", songItem["albumId"])
        songJsonObject.put("trackId", songItem["id"])

        patchedArray[trackArray.length()] = songJsonObject

        return patchedArray
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

    fun parsePlaylistToDB(context: Context, jsonObject: JSONObject):Long{
        val tracks = jsonObject.getJSONArray("tracks").toString()

        val playlistName = jsonObject.getString("name") as String
        val playlistId = jsonObject.getString("_id") as String
        val playlistTrackCount = jsonObject.getJSONArray("tracks").length()

        val databaseHelper = PlaylistDatabaseHelper(context)
        return if(databaseHelper.getPlaylist(playlistId) == null){
            println("Playlist does not exist!")
            databaseHelper.insertPlaylist(playlistId, playlistName, playlistTrackCount)
        }else{
            databaseHelper.getPlaylist(playlistId)!!.id.toLong()
        }
    }

    fun parsePlaylistTracksToHashMap(jsonObject: JSONObject, context: Context): HashMap<String, Any?>? {
        val settings = Settings(context)

        val primaryResolution = settings.getSetting("primaryCoverResolution")
        val secondaryResolution = settings.getSetting("secondaryCoverResolution")

        val title = jsonObject.getString("title")

        if (title != "null") {
            var version = jsonObject.getString("version")
            val artist = jsonObject.getString("artistsTitle")
            val coverUrl = jsonObject.getJSONObject("release").getString("coverUrl")
            val id = jsonObject.getString("_id")
            val albumId = jsonObject.getJSONObject("albums").getString("albumId")
            val streamHash = jsonObject.getJSONObject("albums").getString("streamHash")
            val downloadable = jsonObject.getBoolean("downloadable")
            val streamable = jsonObject.getBoolean("streamable")

            if (version == "null") {
                version = ""
            }

            val trackHashMap = HashMap<String, Any?>()
            trackHashMap["title"] = title
            trackHashMap["version"] = version
            trackHashMap["artist"] = artist
            trackHashMap["coverUrl"] = coverUrl
            trackHashMap["id"] = id
            trackHashMap["streamHash"] = streamHash
            trackHashMap["shownTitle"] = "$artist $title $version"
            trackHashMap["downloadable"] = downloadable
            trackHashMap["streamable"] = streamable
            trackHashMap["albumId"] = albumId

            trackHashMap["primaryImage"] =
                context.filesDir.toString() + "/" + albumId + ".png" + primaryResolution.toString()

            trackHashMap["secondaryImage"] =
                context.filesDir.toString() + "/" + albumId + ".png" + secondaryResolution.toString()

            trackHashMap["primaryRes"] = primaryResolution
            trackHashMap["secondaryRes"] = secondaryResolution
            trackHashMap["coverUrl"] = coverUrl
            trackHashMap["coverLocation"] =
                context.filesDir.toString() + "/" + albumId + ".png"

            return trackHashMap
        } else {
            return null
        }
    }

    fun parsePlaylistTrackToDB(playlistId:String, jsonObject: JSONObject, context: Context):Long?{
        val title = jsonObject.getString("title")

        if (title != "null") {
            var version = jsonObject.getString("version")
            val artist = jsonObject.getString("artistsTitle")
            val coverUrl = jsonObject.getJSONObject("release").getString("coverUrl")
            val id = jsonObject.getString("_id")
            val albumId = jsonObject.getJSONObject("albums").getString("albumId")
            val streamHash = jsonObject.getJSONObject("albums").getString("streamHash")

            if (version == "null") {
                version = ""
            }

            val song = Song()
            song.title = title
            song.artist = artist
            song.coverUrl = coverUrl
            song.version = version
            song.albumId = albumId
            song.songId = id
            song.streamLocation = context.getString(R.string.songStreamUrl) + streamHash

            val songDatabaseHelper = SongDatabaseHelper(context)

            val songid:Long

            if(songDatabaseHelper.getSong(id) == null){
                songid = songDatabaseHelper.insertSong(id, title, version, albumId, artist, coverUrl)
            }else{
                songid = songDatabaseHelper.getSong(id)!!.id.toLong()
            }

            val databaseHelper = PlaylistDataDatabaseHelper(context, playlistId)
            return if(databaseHelper.getPlaylistData(songid.toString()) == null){
                databaseHelper.insertSongId(songid)
            }else{
                databaseHelper.getPlaylistData(songid).id.toLong()
            }
        }else{
            return null
        }
    }

    fun parseDownloadPlaylistTracksToHashMap(jsonObject: JSONObject, context: Context): HashMap<String, Any?>? {
        val settings = Settings(context)

        val downloadType = settings.getSetting("downloadType")
        val downloadQuality = settings.getSetting("downloadQuality")

        val downloadable = jsonObject.getBoolean("downloadable")

        if (downloadable) {
            val title = jsonObject.getString("title")
            var version = jsonObject.getString("version")
            val artist = jsonObject.getString("artistsTitle")
            val coverUrl = jsonObject.getJSONObject("release").getString("coverUrl")
            val id = jsonObject.getString("_id")
            val albumId = jsonObject.getJSONObject("albums").getString("albumId")
            val downloadLocation =
                context.filesDir.toString() + "/" + artist + title + version + "." + downloadType


            if (version == "null") {
                version = ""
            }

            val downloadUrl =
                context.getString(R.string.songDownloadUrl) + albumId + "/download?method=download&type=" + downloadType + "_" + downloadQuality + "&track=" + id

            val hashMap = HashMap<String, Any?>()
            hashMap["title"] = title
            hashMap["version"] = version
            hashMap["artist"] = artist
            hashMap["coverUrl"] = coverUrl
            hashMap["id"] = id
            hashMap["albumId"] = albumId
            hashMap["downloadable"] = downloadable
            hashMap["downloadUrl"] = downloadUrl
            hashMap["downloadLocation"] = downloadLocation
            hashMap["shownTitle"] = "$artist $title $version"

            return hashMap
        } else {
            return null
        }
    }

}