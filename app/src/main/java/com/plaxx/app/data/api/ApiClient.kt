package com.plaxx.app.data.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:5000/api/v1/"

    private val cookieStore = mutableMapOf<String, List<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            cookieStore[url.host] ?: emptyList()
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val baseClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val refreshOnlyClient = baseClient.newBuilder()
        .addInterceptor(logging)
        .build()

    private val mainClient = baseClient.newBuilder()
        .addInterceptor(AuthInterceptor(BASE_URL + "auth/refresh", refreshOnlyClient))
        .addInterceptor(logging)
        .build()

    val api: PlaxxApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(mainClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlaxxApi::class.java)
    }

    fun clearCookies() {
        cookieStore.clear()
    }
}
