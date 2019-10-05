package de.lucaspape.monstercat.download

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser


class DownloadRequest(
    method:Int,
    url: String,
    private val listener: Response.Listener<ByteArray>,
    errorListener: Response.ErrorListener,
    private val headerParams: HashMap<String, String>
) : Request<ByteArray>(method, url, errorListener) {

    override fun deliverResponse(response: ByteArray) = listener.onResponse(response)

    override fun getHeaders(): Map<String, String> {
        return headerParams
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<ByteArray>? {
        return Response.success(response!!.data, HttpHeaderParser.parseCacheHeaders(response))
    }
}
