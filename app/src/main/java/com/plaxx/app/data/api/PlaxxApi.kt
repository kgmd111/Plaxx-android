package com.plaxx.app.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface PlaxxApi {

    // ===== Auth =====
    @POST("auth/register")
    suspend fun register(@Body body: RegisterReq): AuthResp

    @POST("auth/login")
    suspend fun login(@Body body: LoginReq): AuthResp

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshReq = RefreshReq(null)): AuthResp

    @POST("auth/logout")
    suspend fun logout(): OkResp

    @GET("auth/me")
    suspend fun me(): ApiUserDto

    // ===== Parse preview =====
    @Multipart
    @POST("parse-preview")
    suspend fun preview(@Part file: MultipartBody.Part): PreviewResp

    // ===== Plans =====
    @GET("plans")
    suspend fun listPlans(): PlansResp

    @GET("plans/{id}")
    suspend fun getPlan(@Path("id") id: Long): ApiPlanDto

    @Multipart
    @POST("plans/import")
    suspend fun importPlan(
        @Part file: MultipartBody.Part,
        @Part("name") name: RequestBody
    ): ImportResp

    @DELETE("plans/{id}")
    suspend fun deletePlan(@Path("id") id: Long): OkResp

    @GET("plans/{id}/events")
    suspend fun planEvents(@Path("id") id: Long): EventsResp

    @GET("plans/{id}/changes")
    suspend fun planChanges(
        @Path("id") id: Long,
        @Query("since") since: Long
    ): ChangesResp

    // ===== My groups =====
    @GET("me/groups")
    suspend fun myGroups(): RawGroupsResp

    @PUT("me/groups/{planId}")
    suspend fun replaceGroups(
        @Path("planId") planId: Long,
        @Body body: GroupsBody
    ): OkResp

    @PATCH("me/groups/{planId}")
    suspend fun changeGroup(
        @Path("planId") planId: Long,
        @Body body: ChangeGroupReq
    ): ChangeGroupResp

    // ===== Colors =====
    @GET("me/colors/{planId}")
    suspend fun colors(@Path("planId") planId: Long): ColorsResp

    @PUT("me/colors/{planId}/{subject}")
    suspend fun setColor(
        @Path("planId") planId: Long,
        @Path("subject") subject: String,
        @Body body: ColorBody
    ): OkResp

    @DELETE("me/colors/{planId}/{subject}")
    suspend fun deleteColor(
        @Path("planId") planId: Long,
        @Path("subject") subject: String
    ): OkResp

    // ===== Custom events =====
    @GET("me/events")
    suspend fun userEvents(): UserEventsResp

    @POST("me/events")
    suspend fun createUserEvent(@Body body: CreateUserEventReq): IdResp

    @PATCH("me/events/{id}")
    suspend fun updateUserEvent(
        @Path("id") id: Long,
        @Body body: UpdateUserEventReq
    ): OkResp

    @DELETE("me/events/{id}")
    suspend fun deleteUserEvent(@Path("id") id: Long): OkResp

    // ===== Homework =====
    @GET("me/homework")
    suspend fun listHomework(): HomeworkResp

    @POST("me/homework")
    suspend fun createHomework(@Body body: CreateHomeworkReq): IdResp

    @PATCH("me/homework/{id}")
    suspend fun updateHomework(
        @Path("id") id: Long,
        @Body body: UpdateHomeworkReq
    ): OkResp

    @DELETE("me/homework/{id}")
    suspend fun deleteHomework(@Path("id") id: Long): OkResp

    @Multipart
    @POST("me/homework/{id}/attachments")
    suspend fun uploadAttachments(
        @Path("id") id: Long,
        @Part files: List<MultipartBody.Part>
    ): AttachmentUploadResp

    @Streaming
    @GET("me/homework/{id}/attachments/{attId}")
    suspend fun downloadAttachment(
        @Path("id") id: Long,
        @Path("attId") attId: Long
    ): ResponseBody

    @DELETE("me/homework/{id}/attachments/{attId}")
    suspend fun deleteAttachment(
        @Path("id") id: Long,
        @Path("attId") attId: Long
    ): OkResp

    // ===== Sync =====
    @GET("sync")
    suspend fun sync(@QueryMap params: Map<String, String>): SyncResp
}
