package de.lucaspape.monstercat.request

import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.toolbox.HttpStack
import org.apache.http.*
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

class CustomHurlStack : HttpStack {
    companion object {
        @JvmStatic
        val HEADER_CONTENT_TYPE = "Content-Type"
    }

    interface UrlRewriter {
        fun rewriteUrl(originalUrl: String): String
    }

    private var mUrlRewriter: UrlRewriter? = null
    private var mSslSocketFactory: SSLSocketFactory? = null

    @Throws(IOException::class, AuthFailureError::class)
    override fun performRequest(
        request: Request<*>,
        additionalHeaders: Map<String, String>?
    ): HttpResponse? {
        var url = request.url
        val map =
            HashMap<String, String>()
        map.putAll(request.headers)
        map.putAll(additionalHeaders!!)
        if (mUrlRewriter != null) {
            val urlRewriter = mUrlRewriter as UrlRewriter
            val rewritten = urlRewriter.rewriteUrl(url)
            url = rewritten
        }
        val parsedUrl = URL(url)
        val connection = openConnection(parsedUrl, request)
        for (headerName in map.keys) {
            connection.addRequestProperty(headerName, map[headerName])
        }
        setConnectionParametersForRequest(connection, request)
        // Initialize HttpResponse with data from the HttpURLConnection.
        val protocolVersion =
            ProtocolVersion("HTTP", 1, 1)
        val responseCode = connection.responseCode
        if (responseCode == -1) {
            // -1 is returned by getResponseCode() if the response code could not be retrieved.
            // Signal to the caller that something was wrong with the connection.
            throw IOException("Could not retrieve response code from HttpUrlConnection.")
        }
        val responseStatus: StatusLine = BasicStatusLine(
            protocolVersion,
            connection.responseCode, connection.responseMessage
        )
        val response = BasicHttpResponse(responseStatus)
        if (hasResponseBody(request.method, responseStatus.statusCode)) {
            response.entity = entityFromConnection(connection)
        }
        for ((key, value) in connection.headerFields) {
            if (key != null) {
                val h: Header = BasicHeader(key, value[0])
                response.addHeader(h)
            }
        }
        return response
    }

    /**
     * Checks if a response message contains a body.
     *
     * @param requestMethod request method
     * @param responseCode  response status code
     * @return whether the response has a body
     * @see [RFC 7230 section 3.3](https://tools.ietf.org/html/rfc7230.section-3.3)
     */
    private fun hasResponseBody(requestMethod: Int, responseCode: Int): Boolean {
        return (requestMethod != Request.Method.HEAD && !(HttpStatus.SC_CONTINUE <= responseCode && responseCode < HttpStatus.SC_OK)
                && responseCode != HttpStatus.SC_NO_CONTENT && responseCode != HttpStatus.SC_NOT_MODIFIED)
    }

    /**
     * Initializes an [HttpEntity] from the given [HttpURLConnection].
     *
     * @param connection
     * @return an HttpEntity populated with data from `connection`.
     */
    private fun entityFromConnection(connection: HttpURLConnection): HttpEntity? {
        val entity = BasicHttpEntity()
        val inputStream: InputStream? = try {
            connection.inputStream
        } catch (ioe: IOException) {
            connection.errorStream
        }
        entity.content = inputStream
        entity.contentLength = connection.contentLength.toLong()
        entity.setContentEncoding(connection.contentEncoding)
        entity.setContentType(connection.contentType)
        return entity
    }

    /**
     * Create an [HttpURLConnection] for the specified `url`.
     */
    @Throws(IOException::class)
    private fun createConnection(url: URL): HttpURLConnection {
        val connection =
            url.openConnection() as HttpURLConnection
        // Workaround for the M release HttpURLConnection not observing the
        // HttpURLConnection.setFollowRedirects() property.
        // https://code.google.com/p/android/issues/detail?id=194495
        connection.instanceFollowRedirects = HttpURLConnection.getFollowRedirects()
        return connection
    }

    /**
     * Opens an [HttpURLConnection] with parameters.
     *
     * @param url
     * @return an open connection
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun openConnection(
        url: URL,
        request: Request<*>
    ): HttpURLConnection {
        val connection = createConnection(url)
        val timeoutMs = request.timeoutMs
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.useCaches = false
        connection.doInput = true
        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        if ("https" == url.protocol && mSslSocketFactory != null) {
            (connection as HttpsURLConnection).sslSocketFactory = mSslSocketFactory
        }
        return connection
    }

    @Throws(IOException::class, AuthFailureError::class)
    fun setConnectionParametersForRequest(
        connection: HttpURLConnection,
        request: Request<*>
    ) {
        when (request.method) {
            Request.Method.DEPRECATED_GET_OR_POST -> {
                // This is the deprecated way that needs to be handled for backwards compatibility.
                // If the request's post body is null, then the assumption is that the request is
                // GET.  Otherwise, it is assumed that the request is a POST.
                val postBody = request.postBody
                if (postBody != null) {
                    // Prepare output. There is no need to set Content-Length explicitly,
                    // since this is handled by HttpURLConnection using the size of the prepared
                    // output stream.
                    connection.doOutput = true
                    connection.requestMethod = "POST"
                    connection.addRequestProperty(
                        HEADER_CONTENT_TYPE,
                        request.postBodyContentType
                    )
                    val out =
                        DataOutputStream(connection.outputStream)
                    out.write(postBody)
                    out.close()
                }
            }
            Request.Method.GET ->                 // Not necessary to set the request method because connection defaults to GET but
                // being explicit here.
                connection.requestMethod = "GET"
            Request.Method.DELETE -> {
                connection.requestMethod = "DELETE"
                addBodyIfExists(connection, request)
            }
            Request.Method.POST -> {
                connection.requestMethod = "POST"
                addBodyIfExists(connection, request)
            }
            Request.Method.PUT -> {
                connection.requestMethod = "PUT"
                addBodyIfExists(connection, request)
            }
            Request.Method.HEAD -> connection.requestMethod = "HEAD"
            Request.Method.OPTIONS -> connection.requestMethod = "OPTIONS"
            Request.Method.TRACE -> connection.requestMethod = "TRACE"
            Request.Method.PATCH -> {
                connection.requestMethod = "PATCH"
                addBodyIfExists(connection, request)
            }
            else -> throw IllegalStateException("Unknown method type.")
        }
    }

    @Throws(IOException::class, AuthFailureError::class)
    private fun addBodyIfExists(
        connection: HttpURLConnection,
        request: Request<*>
    ) {
        val body = request.body
        if (body != null) {
            connection.doOutput = true
            connection.addRequestProperty(
                HEADER_CONTENT_TYPE,
                request.bodyContentType
            )
            val out =
                DataOutputStream(connection.outputStream)
            out.write(body)
            out.close()
        }
    }
}