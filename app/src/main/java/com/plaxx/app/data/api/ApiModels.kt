package com.plaxx.app.data.api

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ===== Auth =====
data class RegisterReq(
    val username: String,
    val password: String,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name")  val lastName: String?
)
data class LoginReq(val username: String, val password: String)
data class RefreshReq(@SerializedName("refresh_token") val refreshToken: String? = null)

data class AuthResp(
    val user: ApiUserDto,
    @SerializedName("access_token")  val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class ApiUserDto(
    val id: Long,
    val username: String,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name")  val lastName: String?
)

// ===== Plans =====
data class ApiPlanDto(
    val id: Long,
    @SerializedName("owner_user_id")   val ownerUserId: Long,
    val name: String,
    val description: String?,
    @SerializedName("current_version") val currentVersion: Long,
    @SerializedName("created_at")      val createdAt: String,
    @SerializedName("updated_at")      val updatedAt: String
)
data class PlansResp(val plans: List<ApiPlanDto>)
data class ImportResp(
    @SerializedName("plan_id") val planId: Long,
    @SerializedName("events_count") val eventsCount: Int
)

// ===== Plan events =====
data class PlanEventDto(
    val id: Long,
    @SerializedName("plan_id") val planId: Long,
    val subject: String,
    @SerializedName("group_name") val groupName: String,
    val teacher: String?,
    val room: String?,
    @SerializedName("start_datetime") val startDatetime: String,
    @SerializedName("end_datetime")   val endDatetime: String,
    @SerializedName("source_version") val sourceVersion: Long,
    val deleted: Int
)
data class EventsResp(val events: List<PlanEventDto>)

// ===== Delta changes =====
data class ChangeDto(
    val id: Long,
    @SerializedName("plan_id") val planId: Long,
    val version: Long,
    val op: String,
    @SerializedName("entity_id") val entityId: Long?,
    val payload: JsonElement?,
    @SerializedName("created_at") val createdAt: String
)
data class ChangesResp(
    @SerializedName("plan_id") val planId: Long,
    @SerializedName("current_version") val currentVersion: Long,
    val changes: List<ChangeDto>
)

// ===== My groups =====
data class RawGroup(
    @SerializedName("plan_id") val planId: Long,
    val subject: String,
    @SerializedName("group_name") val groupName: String
)
data class RawGroupsResp(val groups: List<RawGroup>)
data class GroupItem(val subject: String, @SerializedName("group_name") val groupName: String)
data class GroupsBody(val groups: List<GroupItem>)
data class ChangeGroupReq(val subject: String, @SerializedName("group_name") val groupName: String)
data class ChangeGroupResp(
    val ok: Boolean,
    @SerializedName("old_group") val oldGroup: String?,
    @SerializedName("new_group") val newGroup: String
)

// ===== Colors =====
data class ColorDto(val subject: String, @SerializedName("color_hex") val colorHex: String)
data class ColorsResp(val colors: List<ColorDto>)
data class ColorBody(@SerializedName("color_hex") val colorHex: String)

// ===== Custom user events =====
data class UserEventDto(
    val id: Long,
    @SerializedName("user_id") val userId: Long,
    val title: String,
    val description: String?,
    val location: String?,
    @SerializedName("color_hex") val colorHex: String?,
    @SerializedName("start_datetime") val startDatetime: String,
    @SerializedName("end_datetime")   val endDatetime: String
)
data class UserEventsResp(val events: List<UserEventDto>)
data class CreateUserEventReq(
    val title: String,
    val description: String?,
    val location: String?,
    @SerializedName("color_hex") val colorHex: String?,
    val start: String,
    val end: String
)
data class UpdateUserEventReq(
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    @SerializedName("color_hex") val colorHex: String? = null,
    val start: String? = null,
    val end: String? = null
)

// ===== Homework =====
data class HomeworkDto(
    val id: Long,
    @SerializedName("user_id") val userId: Long,
    val title: String,
    val description: String?,
    @SerializedName("due_date") val dueDate: String?,
    val status: String,
    @SerializedName("plan_event_id") val planEventId: Long?,
    @SerializedName("user_event_id") val userEventId: Long?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)
data class HomeworkResp(val homework: List<HomeworkDto>)
data class CreateHomeworkReq(
    val title: String,
    val description: String? = null,
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("plan_event_id") val planEventId: Long? = null,
    @SerializedName("user_event_id") val userEventId: Long? = null
)
data class UpdateHomeworkReq(
    val title: String? = null,
    val description: String? = null,
    @SerializedName("due_date") val dueDate: String? = null,
    val status: String? = null
)
data class AttachmentDto(val id: Long, val filename: String)
data class AttachmentUploadResp(val ok: Boolean, val attachments: List<AttachmentDto>)

// ===== Parse preview =====
data class SubjectGroups(val groups: List<String>)
data class ParsedEventDto(
    val subject: String,
    val group: String,
    val teacher: String?,
    val room: String?,
    val start: String,
    val end: String
)
data class PreviewResp(
    val filename: String,
    val count: Int,
    val subjects: Map<String, SubjectGroups>,
    val events: List<ParsedEventDto>
)

// ===== Sync =====
data class SyncColor(
    @SerializedName("plan_id") val planId: Long,
    val subject: String,
    @SerializedName("color_hex") val colorHex: String
)
data class SyncPlan(
    val id: Long,
    val name: String,
    @SerializedName("current_version") val currentVersion: Long,
    val changes: List<ChangeDto>
)
data class SyncResp(
    @SerializedName("server_time") val serverTime: String,
    val plans: List<SyncPlan>,
    val groups: List<RawGroup>,
    val colors: List<SyncColor>,
    @SerializedName("user_events") val userEvents: List<UserEventDto>,
    val homework: List<HomeworkDto>
)

// ===== Generic =====
data class OkResp(val ok: Boolean)
data class IdResp(val id: Long)
