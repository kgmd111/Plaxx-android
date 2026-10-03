package com.plaxx.app.data.models

data class EventDto(
    val subject: String,
    val group: String,
    val teacher: String? = null,
    val room: String? = null,
    val desc: String? = null,
    val start: String,
    val end: String,
    val day: Int,
    val id: String,
    val logicalId: String,
    /** true dla /me/events (custom user events). */
    val isCustom: Boolean = false,
    /** gdy ustawione - nadpisuje kolor z EventColors.bgFor(). */
    val colorHexOverride: String? = null,
    /** id eventu w bazie (Long). Dla plan events to samo co id; dla custom eventow id user_event. */
    val remoteId: Long? = null
)

data class UserDto(
    val id: Long,
    val username: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class SubjectInfo(
    val name: String,
    val groups: List<String>,
    val open: Boolean = true
)
