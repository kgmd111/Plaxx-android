package com.plaxx.app.data

import com.plaxx.app.data.api.ApiClient
import com.plaxx.app.data.api.GroupItem
import com.plaxx.app.data.api.GroupsBody
import com.plaxx.app.data.api.LoginReq
import com.plaxx.app.data.api.PlaxxApi
import com.plaxx.app.data.api.PlanEventDto
import com.plaxx.app.data.api.SyncResp
import com.plaxx.app.data.models.EventDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class InitialLoad(
    val events: List<EventDto>,
    val selectedGroups: Set<String>,
)

class ScheduleRepository(
    private val api: PlaxxApi = ApiClient.api
) {

    private val apiFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // ===== Auth =====
    suspend fun login(username: String, password: String) =
        runCatching { api.login(LoginReq(username, password)) }

    suspend fun logout() = runCatching { api.logout() }

    // ===== Plans =====
    suspend fun listPlans() = runCatching { api.listPlans().plans }

    suspend fun planEvents(planId: Long): Result<List<EventDto>> = runCatching {
        api.planEvents(planId).events.map { it.toInternal(apiFmt) }
    }

    // ===== Groups =====
    suspend fun myGroups(planId: Long): Result<Set<String>> = runCatching {
        api.myGroups().groups
            .filter { it.planId == planId }
            .map { "${it.subject.trim()} \u2022 ${it.groupName.trim()}" }
            .toSet()
    }

    suspend fun replaceGroups(planId: Long, logicalIds: Set<String>): Result<Unit> = runCatching {
        val groups = logicalIds.map { logicalId ->
            val parts = logicalId.split(" \u2022 ")
            GroupItem(
                subject = parts[0],
                groupName = parts.getOrElse(1) { "" }
            )
        }
        api.replaceGroups(planId, GroupsBody(groups))
        Unit
    }

    // ===== Initial load =====
    suspend fun initialLoad(planId: Long): Result<InitialLoad> = runCatching {
        coroutineScope {
            val eventsDeferred = async { planEvents(planId).getOrThrow() }
            val groupsDeferred = async { myGroups(planId).getOrThrow() }
            InitialLoad(
                events = eventsDeferred.await(),
                selectedGroups = groupsDeferred.await(),
            )
        }
    }

    // ===== Delta sync =====
    suspend fun sync(
        planIds: List<Long>,
        sinceVersions: Map<Long, Long>
    ): Result<SyncResp> = runCatching {
        require(planIds.isNotEmpty()) { "brak planow do synchronizacji" }
        val params = buildMap {
            put("plan_ids", planIds.joinToString(","))
            planIds.forEach { id ->
                put("since_$id", (sinceVersions[id] ?: 0L).toString())
            }
        }
        api.sync(params)
    }
}

// ===== Mapper: PlanEventDto -> EventDto (ISO daty) =====
private fun PlanEventDto.toInternal(fmt: DateTimeFormatter): EventDto {
    val startLdt = runCatching { LocalDateTime.parse(startDatetime, fmt) }.getOrNull()
    val endLdt = runCatching { LocalDateTime.parse(endDatetime, fmt) }.getOrNull()

    val startIso = startLdt?.toString() ?: startDatetime
    val endIso = endLdt?.toString() ?: endDatetime
    val day = startLdt?.dayOfWeek?.value ?: 1

    val s = subject.trim()
    val g = groupName.trim()

    return EventDto(
        subject = s,
        group = g,
        teacher = teacher?.trim(),
        room = room?.trim(),
        desc = null,
        start = startIso,
        end = endIso,
        day = day,
        id = id.toString(),
        logicalId = "$s \u2022 $g"
    )
}
