package com.plaxx.app.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class AuthInterceptor(
    private val refreshUrl: String,
    private val refreshClient: OkHttpClient
) : Interceptor {

    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val response = chain.proceed(original)

        if (response.code != 401) return response

        val path = original.url.encodedPath
        if (path.endsWith("/auth/refresh") || path.endsWith("/auth/login")) return response

        synchronized(lock) {
            val refreshReq = Request.Builder()
                .url(refreshUrl)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            val refreshResp = try {
                refreshClient.newCall(refreshReq).execute()
            } catch (_: Exception) {
                return response
            }
            refreshResp.use {
                if (!it.isSuccessful) return response
            }
        }

        response.close()
        return chain.proceed(original)
    }
}
