package de.lucaspape.monstercat.request

import android.content.Context
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.MoodDatabaseHelper
import de.lucaspape.monstercat.core.database.objects.Song
import de.lucaspape.monstercat.core.util.Settings
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

fun newAddToPlaylistRequest(
    context: Context,
    playlistId: String,
    song: Song,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError) -> Unit
): JsonObjectRequest {
    val settings = Settings.getSettings(context)

    val addToPlaylistUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/playlist/" + playlistId + "/record"
        } else {
            context.getString(R.string.playlistUrl) + playlistId + "/record"
        }

    val songJsonObject = JSONObject()
    songJsonObject.put("trackId", song.songId)
    songJsonObject.put("releaseId", song.albumId)

    return JsonObjectRequest(
        Request.Method.PATCH,
        addToPlaylistUrl,
        songJsonObject,
        Response.Listener(callback),
        Response.ErrorListener(errorCallback)
    )
}

fun newSearchTrackRequest(
    context: Context,
    term: String,
    skip: Int,
    forceCustomApi: Boolean,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest? {
    val settings = Settings.getSettings(context)

    var usesCustomApi = false

    val searchUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForSearchSetting)) == true || forceCustomApi
        ) {
            if (settings.getBoolean(
                    context.getString(R.string.customApiSupportsV1Setting)
                ) == true
            ) {
                usesCustomApi = true
                settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v1/catalog/search?term=$term&limit=50&skip=" + skip.toString()
            } else {
                return null
            }
        } else {
            context.getString(R.string.loadSongsUrl) + "?raw={}&limit=50&skip=$skip&offset=0&search=$term&sort=-date&nogold=false&onlyReleased=true&types[]=Single&types[]=EP&types[]=Album"
        }

    return StringRequest(
        Request.Method.GET,
        searchUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, usesCustomApi)))
            } catch (e: JSONException) {
                errorCallback(null)
            }

        },
        Response.ErrorListener(errorCallback)
    )
}

fun newChangePlaylistPublicStateRequest(
    context: Context,
    playlistId: String,
    public: Boolean,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError) -> Unit
): com.android.volley.toolbox.JsonObjectRequest {
    val settings = Settings.getSettings(context)

    val playlistPatchUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/playlist/" + playlistId
        } else {
            context.getString(R.string.playlistUrl) + playlistId
        }

    val postObject = JSONObject()

    postObject.put("public", public)

    return com.android.volley.toolbox.JsonObjectRequest(
        Request.Method.PATCH, playlistPatchUrl, postObject,
        Response.Listener(callback),
        Response.ErrorListener(errorCallback)
    )
}

fun newCreatePlaylistRequest(
    context: Context,
    playlistName: String,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError) -> Unit
): com.android.volley.toolbox.JsonObjectRequest {
    val settings = Settings.getSettings(context)

    val playlistPostUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/playlist"
        } else {
            context.getString(R.string.newPlaylistUrl)
        }

    val postObject = JSONObject()

    postObject.put("name", playlistName)
    postObject.put("public", false)
    postObject.put("tracks", JSONArray())

    return com.android.volley.toolbox.JsonObjectRequest(
        Request.Method.POST, playlistPostUrl, postObject,
        Response.Listener(callback),
        Response.ErrorListener(errorCallback)
    )
}

fun newDeletePlaylistRequest(
    context: Context,
    playlistId: String,
    callback: (response: String) -> Unit,
    errorCallback: (error: VolleyError) -> Unit
): StringRequest {
    val settings = Settings.getSettings(context)

    val deletePlaylistUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/playlist/" + playlistId
        } else {
            context.getString(R.string.playlistUrl) + playlistId
        }

    return StringRequest(
        Request.Method.DELETE, deletePlaylistUrl,
        Response.Listener(callback),
        Response.ErrorListener(errorCallback)
    )
}

fun newDeletePlaylistTrackRequest(
    context: Context,
    playlistId: String,
    song: Song,
    deleteIndex: Int,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError) -> Unit
): JsonObjectRequest {
    val settings = Settings.getSettings(context)

    val deleteTrackFromPlaylistUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/playlist/" + playlistId + "/record"
        } else {
            context.getString(R.string.playlistUrl) + playlistId + "/record"
        }

    val deleteSongObject = JSONObject()
    deleteSongObject.put("trackId", song.songId)
    deleteSongObject.put("releaseId", song.albumId)
    deleteSongObject.put("sort", deleteIndex)

    return JsonObjectRequest(
        Request.Method.DELETE,
        deleteTrackFromPlaylistUrl,
        deleteSongObject,
        Response.Listener(callback),
        Response.ErrorListener(errorCallback)
    )
}

fun newLoadAlbumRequest(
    context: Context,
    mcID: String,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val settings = Settings.getSettings(context)

    var usesCustomApi = false

    val requestUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForCatalogAndAlbumViewSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV1Setting)
            ) == true
        ) {
            usesCustomApi = true
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v1/catalog/release/$mcID"
        } else {
            context.getString(R.string.loadAlbumSongsUrl) + "/$mcID"
        }

    return StringRequest(
        Request.Method.GET, requestUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, usesCustomApi)))
            } catch (e: JSONException) {
                errorCallback(null)
            }
        }, Response.ErrorListener(errorCallback)
    )
}

fun newLoadAlbumListRequest(
    context: Context,
    skip: Int,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val settings = Settings.getSettings(context)

    var usesCustomApi = false

    val requestUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForCatalogAndAlbumViewSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV1Setting)
            ) == true
        ) {
            usesCustomApi = true
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v1/releases?limit=50&skip=" + skip.toString()
        } else {
            context.getString(R.string.loadAlbumsUrl) + "?limit=50&skip=" + skip.toString()
        }

    return StringRequest(
        Request.Method.GET, requestUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, usesCustomApi)))
            } catch (e: JSONException) {
                errorCallback(null)
            }
        },
        Response.ErrorListener(errorCallback)
    )
}

fun newLoadPlaylistRequest(
    context: Context,
    playlistId: String,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val settings = Settings.getSettings(context)

    var usesCustomApi = false

    val playlistUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            usesCustomApi = true
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/playlist/" + playlistId
        } else {
            context.getString(R.string.playlistUrl) + playlistId
        }

    return StringRequest(
        Request.Method.GET,
        playlistUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, usesCustomApi)))
            } catch (e: JSONException) {
                errorCallback(null)
            }
        },
        Response.ErrorListener(errorCallback)
    )
}

fun newLoadPlaylistTracksRequest(
    context: Context,
    playlistId: String,
    skip: Int,
    limit: Int,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val settings = Settings.getSettings(context)

    var usesCustomApi = false

    val playlistTrackUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            usesCustomApi = true
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/playlist/" + playlistId + "/catalog?skip=" + skip.toString() + "&limit=$limit"
        } else {
            context.getString(R.string.playlistTrackUrl) + playlistId + "/catalog?skip=" + skip.toString() + "&limit=$limit"
        }

    return StringRequest(Request.Method.GET, playlistTrackUrl, {
        try {
            callback(JSONObject(getCharacterFromUnicode(it, usesCustomApi)))
        } catch (e: JSONException) {
            errorCallback(null)
        }

    }, Response.ErrorListener(errorCallback))
}

fun newLoadRelatedTracksRequest(
    context: Context,
    trackIdArray: List<String>,
    skipMC: Boolean,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError) -> Unit
): com.android.volley.toolbox.JsonObjectRequest? {
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

    val settings = Settings.getSettings(context)

    val relatedTracksUrl =
        if (settings.getBoolean(context.getString(R.string.playRelatedSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV1Setting)
            ) == true
        ) {
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v1/related?skipMC=$skipMC"
        } else {
            return null
        }

    val loadRelatedTracksRequest = com.android.volley.toolbox.JsonObjectRequest(
        Request.Method.POST,
        relatedTracksUrl,
        jsonObject,
        Response.Listener(callback),
        Response.ErrorListener(errorCallback)
    )

    loadRelatedTracksRequest.retryPolicy =
        DefaultRetryPolicy(
            20000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

    return loadRelatedTracksRequest
}

fun newLoadSongListRequest(
    context: Context,
    skip: Int,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val settings = Settings.getSettings(context)

    var usesCustomApi = false

    val requestUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForCatalogAndAlbumViewSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV1Setting)
            ) == true
        ) {
            usesCustomApi = true
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v1/catalog/?limit=50&skip=" + skip.toString()
        } else {
            context.getString(R.string.loadSongsUrl) + "?limit=50&skip=" + skip.toString()
        }

    return StringRequest(
        Request.Method.GET, requestUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, usesCustomApi)))
            } catch (e: JSONException) {
                errorCallback(null)
            }

        }, Response.ErrorListener(errorCallback)
    )
}

fun newRenamePlaylistRequest(
    context: Context,
    playlistId: String,
    newPlaylistName: String,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError) -> Unit
): com.android.volley.toolbox.JsonObjectRequest {
    val settings = Settings.getSettings(context)

    val playlistPatchUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/playlist/" + playlistId
        } else {
            context.getString(R.string.playlistUrl) + playlistId
        }

    val postObject = JSONObject()

    postObject.put("name", newPlaylistName)

    return com.android.volley.toolbox.JsonObjectRequest(
        Request.Method.PATCH, playlistPatchUrl, postObject,
        Response.Listener(callback),
        Response.ErrorListener(errorCallback)
    )
}

fun newLoadPlaylistsRequest(
    context: Context,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val settings = Settings.getSettings(context)

    var usesCustomApi = false

    val playlistUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            usesCustomApi = true
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/playlists"
        } else {
            context.getString(R.string.playlistsUrl)
        }

    return StringRequest(
        Request.Method.GET, playlistUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, usesCustomApi)))
            } catch (e: JSONException) {
                errorCallback(null)
            }
        },
        Response.ErrorListener(errorCallback)
    )
}

fun newLoginRequest(
    context: Context,
    username: String,
    password: String,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError) -> Unit
): JsonObjectRequest {
    val loginPostParams = JSONObject()
    loginPostParams.put("email", username)
    loginPostParams.put("password", password)

    val settings = Settings.getSettings(context)

    val loginUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/login"
        } else {
            context.getString(R.string.loginUrl)
        }

    return JsonObjectRequest(
        Request.Method.POST, loginUrl, loginPostParams,
        Response.Listener(callback),
        Response.ErrorListener(errorCallback)
    )
}

fun newTwoFaRequest(
    context: Context, twoFACode: String, callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError) -> Unit
): JsonObjectRequest {

    val twoFaTokenParams = JSONObject()
    twoFaTokenParams.put("token", twoFACode)

    val settings = Settings.getSettings(context)

    val twoFaUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/login/token"
        } else {
            context.getString(R.string.tokenUrl)
        }

    return JsonObjectRequest(
        Request.Method.POST,
        twoFaUrl,
        twoFaTokenParams,
        Response.Listener(callback),
        Response.ErrorListener(errorCallback)
    )
}

fun newCheckLoginRequest(
    context: Context, callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val settings = Settings.getSettings(context)

    var usesCustomApi = false

    val sessionUrl =
        if (settings.getBoolean(context.getString(R.string.useCustomApiForEverythingSetting)) == true && settings.getBoolean(
                context.getString(R.string.customApiSupportsV2Setting)
            ) == true
        ) {
            usesCustomApi = true
            settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v2/session"
        } else {
            context.getString(R.string.sessionUrl)
        }

    return StringRequest(
        Request.Method.GET, sessionUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, usesCustomApi)))
            } catch (e: JSONException) {
                errorCallback(null)
            }
        },
        Response.ErrorListener(errorCallback)
    )
}

fun newCustomApiFeatureRequest(
    context: Context,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    return StringRequest(
        Request.Method.GET,
        Settings.getSettings(context)
            .getString(context.getString(R.string.customApiBaseUrlSetting)) + "features",
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, true)))
            } catch (e: JSONException) {
                errorCallback(null)
            }
        },
        Response.ErrorListener(errorCallback)
    )
}

fun newLoadMoodsRequest(
    context: Context,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val requestUrl = context.getString(R.string.moodsUrl)

    return StringRequest(
        Request.Method.GET, requestUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, false)))
            } catch (e: JSONException) {
                errorCallback(null)
            }

        }, Response.ErrorListener(errorCallback)
    )
}

fun newLoadMoodRequest(
    context: Context,
    moodId: String,
    skip: Int,
    limit: Int,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest? {

    val mood = MoodDatabaseHelper(context).getMood(moodId)

    mood?.uri?.let { uri ->
        val requestUrl =
            context.getString(R.string.moodUrl) + "$uri?raw=%7B%7D&limit=$limit&skip=$skip&offset=0&search=&sort=-date&nogold=false&onlyReleased=true"

        return StringRequest(
            Request.Method.GET, requestUrl,
            {
                try {
                    callback(JSONObject(getCharacterFromUnicode(it, false)))
                } catch (e: JSONException) {
                    errorCallback(null)
                }

            }, Response.ErrorListener(errorCallback)
        )
    }
    
    return null
}

fun newLoadFiltersRequest(
    context: Context,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val requestUrl = context.getString(R.string.filtersUrl)

    return StringRequest(
        Request.Method.GET, requestUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, false)))
            } catch (e: JSONException) {
                errorCallback(null)
            }

        }, Response.ErrorListener(errorCallback)
    )
}

fun newLoadGenreRequest(
    context: Context,
    genreName: String,
    skip: Int,
    limit: Int,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val requestUrl =
        context.getString(R.string.loadSongsUrl) + "?raw=%7B%7D&limit=$limit&skip=$skip&offset=0&search=&sort=-date&nogold=false&onlyReleased=true&types[]=Single&types[]=EP&types[]=Album&genres[]=${
            genreName.replace(
                " ",
                "+"
            ).replace("&", "%26")
        }"

    return StringRequest(
        Request.Method.GET, requestUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, false)))
            } catch (e: JSONException) {
                errorCallback(null)
            }

        }, Response.ErrorListener(errorCallback)
    )
}

fun newLoadGreatestHitsRequest(
    context: Context,
    skip: Int,
    limit: Int,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
): StringRequest {
    val requestUrl =
        context.getString(R.string.playlistUrl) + context.getString(R.string.greatestHitsPlaylistId) + "/catalog?raw=%7B%7D&limit=$limit&skip=$skip&offset=0&search=&sort=-date&nogold=false&onlyReleased=true"

    return StringRequest(
        Request.Method.GET, requestUrl,
        {
            try {
                callback(JSONObject(getCharacterFromUnicode(it, false)))
            } catch (e: JSONException) {
                errorCallback(null)
            }

        }, Response.ErrorListener(errorCallback)
    )
}

fun newLoadLivestreamUrlRequest(
    context: Context,
    callback: (response: JSONObject) -> Unit,
    errorCallback: (error: VolleyError?) -> Unit
):StringRequest? {
    val settings = Settings.getSettings(context)

    val requestUrl = if (settings.getBoolean(
            context.getString(R.string.customApiSupportsV1Setting)
        ) == true
    ) {
        settings.getString(context.getString(R.string.customApiBaseUrlSetting)) + "v1/streamurl"
    } else {
        return null
    }

    return StringRequest(
        Request.Method.GET, requestUrl,
        {
            try {
                callback(JSONObject(it))
            } catch (e: JSONException) {
                errorCallback(null)
            }

        }, Response.ErrorListener(errorCallback)
    )
}

//custom api doesnt have this problem, umlauts are shown wrong
fun getCharacterFromUnicode(unicodeChar: String, customApi: Boolean): String {
    return if (!customApi) {
        val utf8 = unicodeChar.toByteArray(charset("ISO-8859-1"))
        String(utf8, charset("UTF-8"))
    } else {
        unicodeChar
    }
}