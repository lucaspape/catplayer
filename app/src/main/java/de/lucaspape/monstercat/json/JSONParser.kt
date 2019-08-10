package de.lucaspape.monstercat.json

import android.content.Context
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.settings.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.reflect.InvocationTargetException

class JSONParser {
    fun parseCatalogSongsToHashMap(jsonObject: JSONObject, context: Context): HashMap<String, Any?> {
        val settings = Settings(context)

        val primaryResolution = settings.getSetting("primaryCoverResolution")
        val secondaryResolution = settings.getSetting("secondaryCoverResolution")

        var id = ""
        var title = ""
        var artist = ""
        var coverUrl = ""
        var version = ""
        var songId = ""
        var downloadable = false
        var streamable = false

        try {
            id = jsonObject.getJSONObject("albums").getString("albumId")
            title = jsonObject.getString("title")
            artist = jsonObject.getString("artistsTitle")
            coverUrl = jsonObject.getJSONObject("release").getString("coverUrl")
            version = jsonObject.getString("version")
            songId = jsonObject.getString("_id")
            downloadable = jsonObject.getBoolean("downloadable")
            streamable = jsonObject.getBoolean("streamable")
        } catch (e: InvocationTargetException) {
        }

        if (version == "null") {
            version = ""
        }

        val hashMap = HashMap<String, Any?>()

        hashMap["id"] = id
        hashMap["title"] = title
        hashMap["artist"] = artist
        hashMap["primaryImage"] =
            context.filesDir.toString() + "/" + title + version + artist + ".png" + primaryResolution.toString()

        hashMap["secondaryImage"] =
            context.filesDir.toString() + "/" + title + version + artist + ".png" + secondaryResolution.toString()

        hashMap["version"] = version

        hashMap["shownTitle"] = "$artist $title $version"
        hashMap["songId"] = songId
        hashMap["downloadable"] = downloadable
        hashMap["streamable"] = streamable
        hashMap["coverUrl"] = coverUrl

        return hashMap
    }

    fun parseCoverToHashMap(hashMap: HashMap<String, Any?>, context: Context): HashMap<String, Any?>? {
        val settings = Settings(context)

        val primaryResolution = settings.getSetting("primaryCoverResolution")
        val secondaryResolution = settings.getSetting("secondaryCoverResolution")

        val title = hashMap["title"]
        val version = hashMap["version"]
        val artist = hashMap["artist"]
        val coverUrl = hashMap["coverUrl"]

        if (!File(context.filesDir.toString() + "/" + title + version + artist + ".png" + primaryResolution).exists()) {
            val coverHashMap = HashMap<String, Any?>()

            coverHashMap["primaryRes"] = primaryResolution
            coverHashMap["secondaryRes"] = secondaryResolution
            coverHashMap["coverUrl"] = coverUrl
            coverHashMap["location"] =
                context.filesDir.toString() + "/" + title + version + artist + ".png"

            return coverHashMap

        } else {
            return null
        }

    }

    fun parsePatchedPlaylist(trackArray: JSONArray, songItem: HashMap<String, Any?>): Array<JSONObject?> {
        val patchedArray = arrayOfNulls<JSONObject>(trackArray.length() + 1)

        for (k in (0 until trackArray.length())) {
            patchedArray[k] = trackArray[k] as JSONObject
        }

        val songJsonObject = JSONObject()
        songJsonObject.put("releaseId", songItem["id"])
        songJsonObject.put("trackId", songItem["songId"])

        patchedArray[trackArray.length()] = songJsonObject

        return patchedArray
    }

    fun parsePlaylistToHashMap(jsonObject: JSONObject): HashMap<String, Any?> {
        val playlistName = jsonObject.getString("name") as String
        val playlistId = jsonObject.getString("_id") as String
        val playlistTrackCount = jsonObject.getJSONArray("tracks").length()

        val tracks = jsonObject.getJSONArray("tracks").toString()

        val playlistHashMap = HashMap<String, Any?>()
        playlistHashMap["playlistName"] = playlistName
        playlistHashMap["coverUrl"] = ""
        playlistHashMap["titles"] = tracks
        playlistHashMap["playlistId"] = playlistId
        playlistHashMap["type"] = "playlist"
        playlistHashMap["trackCount"] = playlistTrackCount

        return playlistHashMap
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
                context.filesDir.toString() + "/" + title + version + artist + ".png" + primaryResolution.toString()

            trackHashMap["secondaryImage"] =
                context.filesDir.toString() + "/" + title + version + artist + ".png" + secondaryResolution.toString()

            return trackHashMap
        } else {
            return null
        }
    }

    fun parsePlaylistTrackCoverToHashMap(hashMap: HashMap<String, Any?>, context: Context): HashMap<String, Any?>? {
        val settings = Settings(context)

        val title = hashMap["title"]
        val version = hashMap["version"]
        val artist = hashMap["artist"]
        val coverUrl = hashMap["coverUrl"]

        val primaryResolution = settings.getSetting("primaryCoverResolution")
        val secondaryResolution = settings.getSetting("secondaryCoverResolution")

        return if (!File(context.filesDir.toString() + "/" + title + version + artist + ".png" + primaryResolution).exists()) {
            val coverHashMap = HashMap<String, Any?>()

            coverHashMap["primaryRes"] = primaryResolution
            coverHashMap["secondaryRes"] = secondaryResolution
            coverHashMap["coverUrl"] = coverUrl
            coverHashMap["location"] =
                context.filesDir.toString() + "/" + title + version + artist + ".png"
            coverHashMap
        } else {
            null
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