package com.plaxx.app.data

import android.content.ContentResolver
import android.net.Uri
import com.plaxx.app.data.api.ApiClient
import com.plaxx.app.data.api.ImportResp
import com.plaxx.app.data.api.PlaxxApi
import com.plaxx.app.data.api.PreviewResp
import com.plaxx.app.data.api.RegisterReq
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PlanImportRepository(
    private val api: PlaxxApi = ApiClient.api
) {

    /** POST /auth/register */
    suspend fun register(
        username: String,
        password: String,
        firstName: String?,
        lastName: String?,
    ) = runCatching {
        require(username.trim().length >= 3) { "Nazwa min. 3 znaki" }
        require(password.length >= 6) { "Haslo min. 6 znakow" }
        api.register(RegisterReq(username.trim(), password, firstName, lastName))
    }

    /** POST /parse-preview (bez auth) */
    suspend fun preview(
        contentResolver: ContentResolver,
        uri: Uri,
    ): Result<PreviewResp> = runCatching {
        val part = buildPart(contentResolver, uri, "file")
            ?: throw IllegalStateException("Nie mozna odczytac pliku")
        api.preview(part)
    }

    /** POST /plans/import (wymaga auth) */
    suspend fun importPlan(
        contentResolver: ContentResolver,
        uri: Uri,
        name: String,
    ): Result<ImportResp> = runCatching {
        val part = buildPart(contentResolver, uri, "file")
            ?: throw IllegalStateException("Nie mozna odczytac pliku")
        val nameBody = name.toRequestBody("text/plain".toMediaType())
        api.importPlan(part, nameBody)
    }

    // ─── Helpers ─────────────────────────────────────────────

    private fun buildPart(
        contentResolver: ContentResolver,
        uri: Uri,
        fieldName: String,
    ): MultipartBody.Part? {
        val bytes = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null

        val mime = contentResolver.getType(uri) ?: "application/octet-stream"
        val filename = queryDisplayName(contentResolver, uri) ?: "plan"

        return MultipartBody.Part.createFormData(
            fieldName, filename, bytes.toRequestBody(mime.toMediaType())
        )
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? =
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
}
