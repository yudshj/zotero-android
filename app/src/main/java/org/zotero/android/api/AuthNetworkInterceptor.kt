package org.zotero.android.api

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.zotero.android.architecture.SdkPrefs
import javax.inject.Inject

class AuthNetworkInterceptor @Inject constructor(
    private val sdkPrefs: SdkPrefs,
) : Interceptor {

    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        return when (val token = sdkPrefs.getApiToken()) {
            null -> chain.proceed(request)
            else -> runBlocking { authenticateRequest(request, token, chain) }
        }
    }

    private fun Request.setAuthorizationHeader(accessToken: String): Request =
        newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun authenticateRequest(
        request: Request,
        token: String,
        chain: Chain,
    ): Response {
        val authRequest = request.setAuthorizationHeader(token)
        val response = chain.proceed(authRequest)
        val body = response.body
        val bodyString = body?.string() ?: ""
        val defaultResponse = response
            .newBuilder()
            .body(bodyString.toResponseBody(body?.contentType()))
            .build()
        return defaultResponse
    }
}