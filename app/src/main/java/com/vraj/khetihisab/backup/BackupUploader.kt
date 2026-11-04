package com.vraj.khetihisab.backup

import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * OkHttp client for server/backup_receiver.php (contract: server/README.md "API").
 *
 * POST multipart: headers `X-Vraj-Key`, `X-Vraj-Device`; parts `file` (application/zip), `date` (yyyy-MM-dd),
 * `sha256` (hex of the ZIP). `GET ?ping=1` tests the key from Settings.
 *
 * https is enforced by [BackupSettings.validateEndpoint] before a URL can be saved; this class accepts any
 * [HttpUrl] so tests can use MockWebServer.
 */
class BackupUploader(
    private val client: OkHttpClient = defaultClient(),
) {
    data class Response(
        val httpCode: Int,
        val ok: Boolean,
        val error: String?,
        val stored: String?,
        val bytes: Long?,
        /** Retry-After seconds on 429. */
        val retryAfterSeconds: Long?,
    ) {
        val failure: BackupFailure? get() = BackupErrorPolicy.classify(httpCode, ok, error)
    }

    sealed interface Outcome {
        data class Http(val response: Response) : Outcome
        data class NetworkError(val cause: IOException) : Outcome
    }

    fun upload(endpoint: HttpUrl, apiKey: String, deviceId: String, zip: File, date: LocalDate, sha256: String): Outcome {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("date", date.toString())
            .addFormDataPart("sha256", sha256)
            .addFormDataPart("file", zip.name, zip.asRequestBody(ZIP))
            .build()
        val req = Request.Builder()
            .url(endpoint)
            .header(HEADER_KEY, apiKey)
            .header(HEADER_DEVICE, deviceId)
            .post(body)
            .build()
        return execute(req)
    }

    fun ping(endpoint: HttpUrl, apiKey: String, deviceId: String): Outcome {
        val req = Request.Builder()
            .url(endpoint.newBuilder().setQueryParameter("ping", "1").build())
            .header(HEADER_KEY, apiKey)
            .header(HEADER_DEVICE, deviceId)
            .get()
            .build()
        return execute(req)
    }

    private fun execute(req: Request): Outcome = try {
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty().take(MAX_BODY_CHARS)
            Outcome.Http(
                Response(
                    httpCode = resp.code,
                    ok = Json.boolField(text, "ok") == true,
                    error = Json.stringField(text, "error"),
                    stored = Json.stringField(text, "stored"),
                    bytes = Json.longField(text, "bytes"),
                    retryAfterSeconds = resp.header("Retry-After")?.trim()?.toLongOrNull(),
                ),
            )
        }
    } catch (e: IOException) {
        Outcome.NetworkError(e)
    }

    companion object {
        const val HEADER_KEY = "X-Vraj-Key"
        const val HEADER_DEVICE = "X-Vraj-Device"
        private const val MAX_BODY_CHARS = 16 * 1024
        val ZIP = "application/zip".toMediaType()

        /** doc 15: connect 30 s, write 5 min. Redirects off so a POST is never silently turned into a GET. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(true)
            .build()
    }
}
