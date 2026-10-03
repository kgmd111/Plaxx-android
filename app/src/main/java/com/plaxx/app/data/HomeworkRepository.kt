package com.plaxx.app.data

import android.content.ContentResolver
import android.net.Uri
import com.plaxx.app.data.api.ApiClient
import com.plaxx.app.data.api.AttachmentDto
import com.plaxx.app.data.api.CreateHomeworkReq
import com.plaxx.app.data.api.HomeworkDto
import com.plaxx.app.data.api.PlaxxApi
import com.plaxx.app.data.api.UpdateHomeworkReq
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HomeworkRepository(
    private val api: PlaxxApi = ApiClient.api
) {
    private val apiFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun list(): Result<List<HomeworkDto>> = runCatching {
        api.listHomework().homework
    }

    suspend fun create(
        title: String,
        description: String?,
        dueDate: LocalDateTime?,
        planEventId: Long? = null,
        userEventId: Long? = null,
    ): Result<Long> = runCatching {
        require(title.isNotBlank()) { "title nie moze byc pusty" }
        api.createHomework(
            CreateHomeworkReq(
                title = title,
                description = description,
                dueDate = dueDate?.format(apiFmt),
                planEventId = planEventId,
                userEventId = userEventId,
            )
        ).id
    }

    suspend fun update(id: Long, req: UpdateHomeworkReq): Result<Unit> = runCatching {
        api.updateHomework(id, req)
        Unit
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        api.deleteHomework(id)
        Unit
    }

    suspend fun uploadAttachments(
        homeworkId: Long,
        parts: List<MultipartBody.Part>
    ): Result<List<AttachmentDto>> = runCatching {
        api.uploadAttachments(homeworkId, parts).attachments
    }

    fun buildParts(
        contentResolver: ContentResolver,
        uris: List<Uri>
    ): List<MultipartBody.Part> = uris.mapNotNull { uri ->
        val bytes = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return@mapNotNull null

        val mime = contentResolver.getType(uri) ?: "application/octet-stream"
        val filename = queryDisplayName(contentResolver, uri) ?: "file"

        MultipartBody.Part.createFormData(
            "file",
            filename,
            bytes.toRequestBody(mime.toMediaType())
        )
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? =
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()

    suspend fun downloadAttachment(
        homeworkId: Long,
        attachmentId: Long
    ): Result<ResponseBody> = runCatching {
        api.downloadAttachment(homeworkId, attachmentId)
    }

    suspend fun deleteAttachment(
        homeworkId: Long,
        attachmentId: Long
    ): Result<Unit> = runCatching {
        api.deleteAttachment(homeworkId, attachmentId)
        Unit
    }
}
