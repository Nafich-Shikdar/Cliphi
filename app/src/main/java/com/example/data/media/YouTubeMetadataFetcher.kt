package com.example.data.media

import com.example.model.YouTubeMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class YouTubeMetadataFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun extractVideoId(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        val patterns = listOf(
            Regex("""(?:https?://)?(?:www\.|m\.)?youtube\.com/watch\?.*v=([A-Za-z0-9_-]{11})"""),
            Regex("""(?:https?://)?youtu\.be/([A-Za-z0-9_-]{11})"""),
            Regex("""(?:https?://)?(?:www\.|m\.)?youtube\.com/shorts/([A-Za-z0-9_-]{11})"""),
            Regex("""(?:https?://)?(?:www\.|m\.)?youtube\.com/live/([A-Za-z0-9_-]{11})""")
        )
        for (pattern in patterns) {
            val match = pattern.find(trimmed)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    suspend fun fetchMetadata(rawUrl: String): Result<YouTubeMetadata> = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(rawUrl)
            ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid YouTube URL. Supported formats: youtube.com/watch?v=..., youtu.be/..., or youtube.com/shorts/...")
            )

        val canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
        val encodedUrl = URLEncoder.encode(canonicalUrl, "UTF-8")
        val oEmbedUrl = "https://www.youtube.com/oembed?url=$encodedUrl&format=json"

        try {
            val request = Request.Builder()
                .url(oEmbedUrl)
                .header("User-Agent", "ClipForge/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Could not fetch YouTube metadata (HTTP ${response.code}). Ensure the video is public or unlisted.")
                    )
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Empty metadata response from YouTube."))
                }
                val json = JSONObject(body)
                val title = json.optString("title", "YouTube Video ($videoId)")
                val author = json.optString("author_name", "Unknown Creator")
                val thumbnail = json.optString(
                    "thumbnail_url",
                    "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                )
                val durationLabel = tryFetchDurationHint(canonicalUrl)

                Result.success(
                    YouTubeMetadata(
                        url = canonicalUrl,
                        videoId = videoId,
                        title = title,
                        authorName = author,
                        thumbnailUrl = thumbnail,
                        durationLabel = durationLabel
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(e.localizedMessage ?: "Network error while checking YouTube URL.")
            )
        }
    }

    private fun tryFetchDurationHint(watchUrl: String): String? {
        return try {
            val req = Request.Builder()
                .url(watchUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string().orEmpty()
                val match = Regex(""""lengthSeconds":"(\d+)"""").find(html)
                val secs = match?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return null
                val mins = secs / 60
                val rem = secs % 60
                String.format("%02d:%02d", mins, rem)
            }
        } catch (_: Exception) {
            null
        }
    }
}
