package com.plaxx.app.data

import com.plaxx.app.data.api.ApiClient
import com.plaxx.app.data.api.ColorBody
import com.plaxx.app.data.api.PlaxxApi

class ColorsRepository(
    private val api: PlaxxApi = ApiClient.api
) {
    suspend fun get(planId: Long): Result<Map<String, String>> = runCatching {
        api.colors(planId).colors.associate { it.subject to it.colorHex }
    }

    suspend fun set(planId: Long, subject: String, hex: String): Result<Unit> = runCatching {
        api.setColor(planId, subject, ColorBody(hex))
        Unit
    }

    suspend fun reset(planId: Long, subject: String): Result<Unit> = runCatching {
        api.deleteColor(planId, subject)
        Unit
    }
}
