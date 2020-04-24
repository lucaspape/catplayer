package de.lucaspape.monstercat.request

import android.content.Context
import android.content.pm.PackageInfo
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.toolbox.BaseHttpStack
import com.android.volley.toolbox.HttpResponse
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttp3Stack(private val context: Context, private val cookieJar: CookieJar) :
    BaseHttpStack() {

    @Throws(IOException::class, AuthFailureError::class)
    private fun setConnectionParametersForRequest(
        builder: okhttp3.Request.Builder,
        request: Request<*>
    ) {
        when (request.method) {
            Request.Method.DEPRECATED_GET_OR_POST -> {
                // Ensure backwards compatibility.  Volley assumes a request with a null body is a GET.
                val postBody = request.postBody
                if (postBody != null) {
                    builder.post(
                        RequestBody.create(
                            request.postBodyContentType.toMediaTypeOrNull(),
                            postBody
                        )
                    )
                }
            }
            Request.Method.GET -> builder.get()
            Request.Method.DELETE -> builder.delete(createRequestBody(request))
            Request.Method.POST -> builder.post(createRequestBody(request)!!)
            Request.Method.PUT -> builder.put(createRequestBody(request)!!)
            Request.Method.HEAD -> builder.head()
            Request.Method.OPTIONS -> builder.method("OPTIONS", null)
            Request.Method.TRACE -> builder.method("TRACE", null)
            Request.Method.PATCH -> builder.patch(createRequestBody(request)!!)
            else -> throw IllegalStateException("Unknown method type.")
        }
    }

    @Throws(AuthFailureError::class)
    private fun createRequestBody(r: Request<*>): RequestBody? {
        val body = r.body ?: return null
        return RequestBody.create(r.bodyContentType.toMediaTypeOrNull(), body)
    }

    override fun executeRequest(
        request: Request<*>?,
        additionalHeaders: MutableMap<String, String>?
    ): HttpResponse {
        val clientBuilder = OkHttpClient.Builder()

        val timeoutMs = if (request?.timeoutMs == null) {
            1000.toLong()
        } else {
            request.timeoutMs.toLong()
        }

        clientBuilder.connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        clientBuilder.readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        clientBuilder.writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)

        val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val version = pInfo.versionName

        clientBuilder.addInterceptor(UserAgentInterceptor(context.packageName, version))

        val okHttpRequestBuilder = okhttp3.Request.Builder()

        request?.let {
            okHttpRequestBuilder.url(request.url)

            for (name in request.headers.keys) {
                request.headers[name]?.let {
                    okHttpRequestBuilder.addHeader(name, it)
                }
            }

            additionalHeaders?.let {
                for (name in additionalHeaders.keys) {
                    additionalHeaders[name]?.let {
                        okHttpRequestBuilder.addHeader(name, it)
                    }
                }
            }

            setConnectionParametersForRequest(okHttpRequestBuilder, request)
        }

        val client = clientBuilder.cookieJar(cookieJar).build()
        val okHttpRequest = okHttpRequestBuilder.build()
        val okHttpCall = client.newCall(okHttpRequest)

        val okHttpResponse = okHttpCall.execute()

        val code = okHttpResponse.code
        val body = okHttpResponse.body

        val content = body?.byteStream()

        val contentLength = if (body?.contentLength() == null) {
            0
        } else {
            body.contentLength().toInt()
        }

        val responseHeaders = mapHeaders(okHttpResponse.headers)

        return HttpResponse(code, responseHeaders, contentLength, content)
    }

    private fun mapHeaders(responseHeaders: Headers): List<com.android.volley.Header> {
        val headers = ArrayList<com.android.volley.Header>()

        for (i in (0 until responseHeaders.size)) {
            val name = responseHeaders.name(i)
            val value = responseHeaders.value(i)

            headers.add(com.android.volley.Header(name, value))
        }

        return headers
    }
}