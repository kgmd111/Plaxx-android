package com.plaxx.app.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.plaxx.app.data.api.ChangeDto
import com.plaxx.app.data.api.PlanEventDto

object SyncPayloadParser {

    private val gson = Gson()

    /**
     * MySQL zwraca JSON jako string w JSON - sqlx serializuje drugi raz.
     */
    fun parseEvent(change: ChangeDto): PlanEventDto? {
        val el: JsonElement = change.payload ?: return null
        val json = if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
            el.asString
        } else {
            el.toString()
        }
        return runCatching { gson.fromJson(json, PlanEventDto::class.java) }.getOrNull()
    }
}
