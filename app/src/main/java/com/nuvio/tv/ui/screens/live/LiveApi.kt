package com.nuvio.tv.ui.screens.live

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

internal const val SPORTSRC_API_BASE = "https://api.sportsrc.org"

@Serializable
internal data class LiveMatch(
    val id: String,
    val title: String,
    val category: String,
    val poster: String = "",
    val popular: Boolean = false,
)

@Serializable
internal data class LiveStreamSource(
    val id: String,
    val streamNo: Int,
    val language: String = "",
    val hd: Boolean = false,
    val embedUrl: String = "",
    val viewers: Int = 0,
)

@Serializable
private data class MatchesResponse(
    val success: Boolean = false,
    val data: List<LiveMatchDto> = emptyList(),
)

@Serializable
private data class LiveMatchDto(
    val id: String,
    val title: String,
    val category: String,
    val poster: String = "",
    val popular: Boolean = false,
)

@Serializable
private data class DetailResponse(
    val success: Boolean = false,
    val data: LiveDetailDto? = null,
)

@Serializable
private data class LiveDetailDto(
    val id: String,
    val title: String,
    val category: String,
    val sources: List<LiveSourceDto> = emptyList(),
)

@Serializable
private data class LiveSourceDto(
    val id: String,
    val streamNo: Int,
    val language: String = "",
    val hd: Boolean = false,
    val embedUrl: String = "",
    val viewers: Int = 0,
)

internal object LiveApi {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val categories = listOf(
        "cricket", "football", "tennis", "basketball",
        "hockey", "baseball", "rugby", "combat"
    )

    private fun get(url: String): String? {
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getMatches(category: String): List<LiveMatch> = withContext(Dispatchers.IO) {
        try {
            val body = get("$SPORTSRC_API_BASE/?data=matches&category=$category") ?: return@withContext emptyList()
            val parsed = json.decodeFromString<MatchesResponse>(body)
            parsed.data.map { LiveMatch(it.id, it.title, it.category, it.poster, it.popular) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getSources(category: String, id: String): List<LiveStreamSource> = withContext(Dispatchers.IO) {
        try {
            val body = get("$SPORTSRC_API_BASE/?data=detail&category=$category&id=$id") ?: return@withContext emptyList()
            val parsed = json.decodeFromString<DetailResponse>(body)
            parsed.data?.sources?.map {
                LiveStreamSource(it.id, it.streamNo, it.language, it.hd, it.embedUrl, it.viewers)
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
