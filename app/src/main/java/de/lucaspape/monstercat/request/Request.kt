package de.lucaspape.monstercat.request

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.objects.Song
import de.lucaspape.util.Settings
import org.json.JSONArray
import org.json.JSONObject

fun newAddToPlaylistRequest(context:Context, playlistId:String, song:Song, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit):JsonObjectRequest{
    val addToPlaylistUrl =
        context.getString(R.string.playlistUrl) + playlistId + "/record"

    val songJsonObject = JSONObject()
    songJsonObject.put("trackId", song.songId)
    songJsonObject.put("releaseId", song.albumId)

    return JsonObjectRequest(
        Request.Method.PATCH,
        addToPlaylistUrl,
        songJsonObject,
        Response.Listener {
            callback(it)
        },
        Response.ErrorListener {
            errorCallback(it)
        })
}

fun newSearchTrackRequest(context: Context, term:String, skip: Int, forceCustomApi:Boolean, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit):StringRequest{
    val searchUrl = if(Settings.getSettings(context).getBoolean(context.getString(R.string.useCustomApiSetting)) == true || forceCustomApi){
        context.getString(R.string.customApiBaseUrl) + "catalog/search?term=$term&limit=50&skip=" + skip.toString()
    }else{
        context.getString(R.string.loadSongsUrl) + "?term=$term&limit=50&skip=" + skip.toString() + "&fields=&search=$term"
    }

    return StringRequest(Request.Method.GET,
        searchUrl,
        Response.Listener {
            callback(JSONObject(it))
        },
        Response.ErrorListener {
            errorCallback(it)
        })
}

fun newChangePlaylistPublicStateRequest(context: Context, playlistId: String, public:Boolean, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit): com.android.volley.toolbox.JsonObjectRequest{
    val playlistPatchUrl = context.getString(R.string.playlistUrl) + playlistId

    val postObject = JSONObject()

    postObject.put("public", public)

    return com.android.volley.toolbox.JsonObjectRequest(
        Request.Method.PATCH, playlistPatchUrl, postObject,
        Response.Listener {
            callback(it)
        },
        Response.ErrorListener {
            errorCallback(it)
        }
    )
}

fun newCreatePlaylistRequest(context: Context, playlistName:String, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit): com.android.volley.toolbox.JsonObjectRequest{
    val playlistPostUrl = context.getString(R.string.newPlaylistUrl)

    val postObject = JSONObject()

    postObject.put("name", playlistName)
    postObject.put("public", false)
    postObject.put("tracks", JSONArray())

    return com.android.volley.toolbox.JsonObjectRequest(
        Request.Method.POST, playlistPostUrl, postObject,
        Response.Listener {
            callback(it)
        },
        Response.ErrorListener {
            errorCallback(it)
        }
    )
}

fun newDeletePlaylistRequest(context: Context, playlistId: String, callback:(response:String)-> Unit, errorCallback:(error:VolleyError) -> Unit):StringRequest{
    val deletePlaylistUrl = context.getString(R.string.playlistUrl) + playlistId

    return StringRequest(
        Request.Method.DELETE, deletePlaylistUrl,
        Response.Listener {
            callback(it)
        },
        Response.ErrorListener {
            errorCallback(it)
        })
}

fun newDeletePlaylistTrackRequest(context: Context, playlistId: String, song:Song, deleteIndex:Int, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit):JsonObjectRequest{
    val deleteTrackFromPlaylistUrl =
        context.getString(R.string.playlistUrl) + playlistId + "/record"

    val deleteSongObject = JSONObject()
    deleteSongObject.put("trackId", song.songId)
    deleteSongObject.put("releaseId", song.albumId)
    deleteSongObject.put("sort", deleteIndex)

    return JsonObjectRequest(
        Request.Method.DELETE,
        deleteTrackFromPlaylistUrl,
        deleteSongObject,
        Response.Listener {
            callback(it)
        },
        Response.ErrorListener {
            errorCallback(it)
        })
}

fun newLoadAlbumRequest(context: Context, mcID:String, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit):StringRequest{
    val requestUrl = if(Settings.getSettings(context).getBoolean(context.getString(R.string.useCustomApiSetting)) == true){
        context.getString(R.string.customApiBaseUrl) + "catalog/release/$mcID"
    }else{
        context.getString(R.string.loadAlbumSongsUrl) + "/$mcID"
    }

    return StringRequest(
        Request.Method.GET, requestUrl,
        Response.Listener {
            callback(JSONObject(it))

        }, Response.ErrorListener {
            errorCallback(it)
        }
    )
}

fun newLoadAlbumListRequest(context: Context, skip:Int, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit):StringRequest{
    val requestUrl = if(Settings.getSettings(context).getBoolean(context.getString(R.string.useCustomApiSetting)) == true){
        context.getString(R.string.customApiBaseUrl) + "releases?limit=50&skip=" + skip.toString()
    }else{
        context.getString(R.string.loadAlbumsUrl) + "?limit=50&skip=" + skip.toString()
    }

    return StringRequest(
        Request.Method.GET, requestUrl,
        Response.Listener {
            callback(JSONObject(it))

        },
        Response.ErrorListener {
            errorCallback(it)
        }
    )
}

fun newLoadPlaylistRequest(context: Context, playlistId: String, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit): StringRequest{
    return StringRequest(
        Request.Method.GET,
        context.getString(R.string.playlistUrl) + playlistId,
        Response.Listener {
            callback(JSONObject(it))
        },
        Response.ErrorListener {
            errorCallback(it)
        }
    )
}

fun newLoadPlaylistTracksRequest(context: Context, playlistId: String, skip: Int, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit):StringRequest{
    val playlistTrackUrl =
        context.getString(R.string.playlistTrackUrl) + playlistId + "/catalog?skip=" + skip.toString() + "&limit=50"

    return StringRequest(Request.Method.GET, playlistTrackUrl, Response.Listener {
        callback(JSONObject(it))

    }, Response.ErrorListener {
        errorCallback(it)
    })
}

fun newLoadRelatedTracksRequest(context: Context, trackIdArray:List<String>, skipMC:Boolean, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit): com.android.volley.toolbox.JsonObjectRequest{
    val jsonObject = JSONObject()
    val trackJsonArray = JSONArray()
    val excludeJsonArray = JSONArray()

    val startIndex = if (trackIdArray.size >= 10) {
        trackIdArray.size - 10
    } else {
        0
    }

    for (i in (startIndex until trackIdArray.size)) {
        val trackObject = JSONObject()
        trackObject.put("id", trackIdArray[i])
        trackJsonArray.put(trackObject)
    }

    for (trackId in trackIdArray) {
        val trackObject = JSONObject()
        trackObject.put("id", trackId)
        excludeJsonArray.put(trackObject)
    }

    jsonObject.put("tracks", trackJsonArray)
    jsonObject.put("exclude", excludeJsonArray)

    val loadRelatedTracksRequest = com.android.volley.toolbox.JsonObjectRequest(Request.Method.POST,
        context.getString(R.string.customApiBaseUrl) + "related?skipMC=$skipMC",
        jsonObject,
        Response.Listener {
            callback(it)

        },
        Response.ErrorListener {
            errorCallback(it)
        })

    loadRelatedTracksRequest.retryPolicy =
        DefaultRetryPolicy(
            20000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

    return loadRelatedTracksRequest
}

fun newLoadSongListRequest(context: Context, skip: Int, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit):StringRequest{
    val requestUrl = if(Settings.getSettings(context).getBoolean(context.getString(R.string.useCustomApiSetting)) == true){
        context.getString(R.string.customApiBaseUrl) + "catalog/?limit=50&skip=" + skip.toString()
    }else{
        context.getString(R.string.loadSongsUrl) + "?limit=50&skip=" + skip.toString()
    }

    return StringRequest(
        Request.Method.GET, requestUrl,
        Response.Listener {
            callback(JSONObject(it))

        }, Response.ErrorListener {
            errorCallback(it)
        }
    )
}

fun newRenamePlaylistRequest(context: Context, playlistId: String, newPlaylistName:String, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit):com.android.volley.toolbox.JsonObjectRequest{
    val playlistPatchUrl = context.getString(R.string.playlistUrl) + playlistId

    val postObject = JSONObject()

    postObject.put("name", newPlaylistName)

    return com.android.volley.toolbox.JsonObjectRequest(
        Request.Method.PATCH, playlistPatchUrl, postObject,
        Response.Listener {
            callback(it)
        },
        Response.ErrorListener {
            errorCallback(it)
        }
    )
}

fun newLoadPlaylistsRequest(context: Context, callback:(response:JSONObject)-> Unit, errorCallback:(error:VolleyError) -> Unit):StringRequest{
    val playlistUrl = context.getString(R.string.playlistsUrl)

    return StringRequest(
        Request.Method.GET, playlistUrl,
        Response.Listener {
            callback(JSONObject(it))
        },
        Response.ErrorListener {
            errorCallback(it)
        })
}