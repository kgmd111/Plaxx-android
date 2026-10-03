package com.plaxx.app.data

import com.plaxx.app.data.api.ApiClient
import com.plaxx.app.data.api.CreateUserEventReq
import com.plaxx.app.data.api.PlaxxApi
import com.plaxx.app.data.api.UpdateUserEventReq
import com.plaxx.app.data.api.UserEventDto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UserEventRepository(
    private val api: PlaxxApi = ApiClient.api
) {
    private val apiFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun list(): Result<List<UserEventDto>> = runCatching {
        api.userEvents().events
    }

    suspend fun create(
        title: String,
        description: String?,
        location: String?,
        colorHex: String?,
        start: LocalDateTime,
        end: LocalDateTime,
    ): Result<Long> = runCatching {
        require(end.isAfter(start)) { "end musi byc po start" }
        require(title.length in 1..255) { "title 1..255 znakow" }
        api.createUserEvent(
            CreateUserEventReq(
                title = title,
                description = description,
                location = location,
                colorHex = colorHex,
                start = start.format(apiFmt),
                end = end.format(apiFmt),
            )
        ).id
    }

    suspend fun update(id: Long, req: UpdateUserEventReq): Result<Unit> = runCatching {
        api.updateUserEvent(id, req)
        Unit
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        api.deleteUserEvent(id)
        Unit
    }
}
