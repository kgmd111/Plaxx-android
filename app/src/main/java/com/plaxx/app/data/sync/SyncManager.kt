package com.plaxx.app.data.sync

import com.plaxx.app.data.ScheduleRepository
import com.plaxx.app.data.SyncPayloadParser
import com.plaxx.app.data.api.SyncResp

/**
 * Wrapper dla GET /sync. Aplikowanie delt do UI robi ScheduleViewModel
 * (docelowo: zapis do Room + emit Flow). Tutaj tylko request + parsowanie.
 */
class SyncManager(
    private val repository: ScheduleRepository = ScheduleRepository(),
) {
    suspend fun sync(
        planIds: List<Long>,
        sinceVersions: Map<Long, Long>,
    ): Result<SyncResp> = repository.sync(planIds, sinceVersions)

    /** Parsuje payload zmiany na event (jesli CREATE/UPDATE). */
    fun payloadEvent(change: com.plaxx.app.data.api.ChangeDto) =
        SyncPayloadParser.parseEvent(change)
}
