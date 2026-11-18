package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.AiSelectionMode
import com.example.model.LanguageOption
import com.example.model.SelectedHighlight
import com.example.model.SubtitleCue
import com.example.model.WordTiming
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "GeminiVideoAiService"

class GeminiVideoAiService {

    private val transcriptionSchema = Schema.obj(
        properties = mapOf(
            "detectedLanguage" to Schema.string(description = "Detected primary spoken language code, e.g. 'bn' or 'en'"),
            "segments" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "startSec" to Schema.double(description = "Segment start time in seconds relative to this audio chunk"),
                        "endSec" to Schema.double(description = "Segment end time in seconds relative to this audio chunk"),
                        "text" to Schema.string(description = "Accurate verbatim transcription in original script (Bengali বাংলা or English)"),
                        "speaker" to Schema.string(description = "Speaker label such as Speaker 1 or Speaker 2")
                    )
                ),
                description = "Sentence-aligned transcript segments"
            )
        )
    )

    private val highlightSchema = Schema.obj(
        properties = mapOf(
            "clips" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Concise, compelling hook title for the short clip"),
                        "startMs" to Schema.integer(description = "Exact start timestamp in milliseconds aligned to a sentence start"),
                        "endMs" to Schema.integer(description = "Exact end timestamp in milliseconds aligned to a sentence end"),
                        "score" to Schema.integer(description = "Engagement highlight score from 1 to 100"),
                        "reason" to Schema.string(description = "Brief explanation of why this moment works well as a short"),
                        "tags" to Schema.array(
                            items = Schema.string(),
                            description = "2 to 4 relevant topic tags"
                        )
                    )
                ),
                description = "Selected non-overlapping highlight clips"
            )
        )
    )

    suspend fun transcribeAudioChunk(
        audioBytes: ByteArray,
        mimeType: String,
        chunkOffsetMs: Long,
        chunkDurationMs: Long,
        languageOption: LanguageOption,
        modelName: String
    ): List<SubtitleCue> {
        val model by lazy {
            Firebase.ai.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = transcriptionSchema
                    temperature = 0.2f
                }
            )
        }

        val languageInstruction = when (languageOption) {
            LanguageOption.BENGALI -> "The primary spoken language is Bengali (বাংলা). Transcribe accurately in Bengali script (বাংলা বর্ণমালা) while keeping any English technical terms intact."
            LanguageOption.ENGLISH -> "The primary spoken language is English. Transcribe accurately in clean English."
            LanguageOption.AUTO_DETECT -> "Automatically detect whether the speech is Bengali (বাংলা), English, or mixed (Banglish/code-switched). Use Bengali script for Bengali words and English for English words."
        }

        val chunkDurationSec = (chunkDurationMs / 1000.0).coerceAtLeast(1.0)
        val promptText = """
            You are a precision speech transcription engine for short-form video creation.
            $languageInstruction
            Audio duration is approximately ${"%.1f".format(chunkDurationSec)} seconds.
            Break the speech into natural, complete sentence or clause segments (each 2 to 6 seconds long) with accurate startSec and endSec timestamps within [0, ${"%.1f".format(chunkDurationSec)}].
            Never invent speech if the audio is silent or music-only.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        inlineData(audioBytes, mimeType)
                        text(promptText)
                    }
                )
            }

            val rawJson = response.text ?: throw IllegalStateException("Gemini returned an empty transcription response.")
            val root = JSONObject(rawJson)
            val detectedLang = root.optString("detectedLanguage", languageOption.code)
            val segments = root.optJSONArray("segments") ?: return emptyList()

            val result = mutableListOf<SubtitleCue>()
            for (i in 0 until segments.length()) {
                val seg = segments.getJSONObject(i)
                val text = seg.optString("text", "").trim()
                if (text.isBlank()) continue

                val relStartMs = (seg.optDouble("startSec", 0.0) * 1000.0).toLong().coerceIn(0L, chunkDurationMs)
                val relEndMs = (seg.optDouble("endSec", 0.0) * 1000.0).toLong().coerceIn(relStartMs + 400L, chunkDurationMs.coerceAtLeast(relStartMs + 400L))
                val absStart = chunkOffsetMs + relStartMs
                val absEnd = chunkOffsetMs + relEndMs
                val speaker = seg.optString("speaker", "Speaker 1").ifBlank { "Speaker 1" }

                result += SubtitleCue(
                    id = UUID.randomUUID().toString(),
                    startMs = absStart,
                    endMs = absEnd,
                    text = text,
                    speaker = speaker,
                    language = detectedLang,
                    words = estimateWordTimings(text, absStart, absEnd)
                )
            }
            result.sortedBy { it.startMs }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Transcription timed out", e)
            throw IllegalStateException("Speech transcription timed out after 60 seconds. Try a shorter video or check your connection.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to transcribe speech with Gemini AI.")
        }
    }

    suspend fun selectHighlights(
        transcript: List<SubtitleCue>,
        keyframeBitmaps: List<Bitmap>,
        videoDurationMs: Long,
        minDurationSec: Int,
        maxDurationSec: Int,
        requestedCount: Int,
        selectionMode: AiSelectionMode,
        customPrompt: String,
        modelName: String
    ): List<SelectedHighlight> {
        val model by lazy {
            Firebase.ai.generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = highlightSchema
                    temperature = 0.4f
                }
            )
        }

        val transcriptSummary = if (transcript.isNotEmpty()) {
            transcript.joinToString("\n") { cue ->
                "[${cue.startMs}ms - ${cue.endMs}ms] (${cue.speaker}): ${cue.text}"
            }
        } else {
            "No spoken dialogue was detected. Analyze the provided video keyframes and total duration (${videoDurationMs}ms) to pick visually distinct, engaging segments."
        }

        val modeDirective = if (selectionMode == AiSelectionMode.CUSTOM && customPrompt.isNotBlank()) {
            "Custom Selection Goal: $customPrompt"
        } else {
            "Selection Focus: ${selectionMode.label} (${selectionMode.promptGuidance})."
        }

        val prompt = """
            You are an expert short-form video editor selecting clips for YouTube Shorts, Reels, and TikTok.
            Total source video duration: $videoDurationMs ms (${videoDurationMs / 1000} seconds).
            Target clip count: up to $requestedCount non-overlapping clips (fewer if the video is too short to support $requestedCount distinct clips).
            Target clip duration: between ${minDurationSec * 1000} ms and ${maxDurationSec * 1000} ms (must not exceed $videoDurationMs ms).
            $modeDirective

            STRICT RULES:
            1. Align startMs and endMs cleanly to complete sentence boundaries from the transcript—never start or end mid-sentence.
            2. Do NOT select duplicate or heavily overlapping clips (any two clips must have less than 20% overlap).
            3. Score each clip honestly from 1 to 100 based on hook strength, narrative completeness, and clarity. Do not promise guaranteed virality.
            4. Keep all timestamps strictly within [0, $videoDurationMs].

            TIMESTAMPED TRANSCRIPT:
            $transcriptSummary
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        keyframeBitmaps.take(6).forEach { bmp ->
                            image(bmp)
                        }
                        text(prompt)
                    }
                )
            }

            val rawJson = response.text ?: throw IllegalStateException("Gemini returned an empty highlight selection.")
            val root = JSONObject(rawJson)
            val clipsJson = root.optJSONArray("clips") ?: return fallbackSegments(videoDurationMs, minDurationSec, maxDurationSec, requestedCount)

            val parsed = mutableListOf<SelectedHighlight>()
            for (i in 0 until clipsJson.length()) {
                val obj = clipsJson.getJSONObject(i)
                val rawStart = obj.optLong("startMs", 0L).coerceIn(0L, (videoDurationMs - 3000L).coerceAtLeast(0L))
                val minEnd = (rawStart + (minDurationSec * 1000L).coerceAtMost(videoDurationMs - rawStart)).coerceAtMost(videoDurationMs)
                val rawEnd = obj.optLong("endMs", minEnd).coerceIn(rawStart + 3000L, videoDurationMs)
                val aligned = alignToTranscriptBoundaries(rawStart, rawEnd, transcript, videoDurationMs)
                if (aligned.second - aligned.first < 3000L) continue

                val tagsArray = obj.optJSONArray("tags")
                val tags = if (tagsArray != null) {
                    (0 until tagsArray.length()).map { tagsArray.getString(it) }
                } else emptyList()

                parsed += SelectedHighlight(
                    title = obj.optString("title", "Highlight Clip ${i + 1}").ifBlank { "Highlight Clip ${i + 1}" },
                    startMs = aligned.first,
                    endMs = aligned.second,
                    score = obj.optInt("score", 78).coerceIn(1, 100),
                    reason = obj.optString("reason", "Strong narrative coherence and clear delivery."),
                    tags = tags
                )
            }

            val deduplicated = removeHeavyOverlaps(parsed).take(requestedCount)
            if (deduplicated.isEmpty()) {
                fallbackSegments(videoDurationMs, minDurationSec, maxDurationSec, requestedCount)
            } else {
                deduplicated
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Highlight analysis timed out", e)
            throw IllegalStateException("AI highlight analysis timed out after 60 seconds. Please retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Highlight analysis failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to analyze highlights with Gemini AI.")
        }
    }

    private fun alignToTranscriptBoundaries(
        startMs: Long,
        endMs: Long,
        transcript: List<SubtitleCue>,
        videoDurationMs: Long
    ): Pair<Long, Long> {
        if (transcript.isEmpty()) return startMs to endMs
        val startCue = transcript.minByOrNull { kotlin.math.abs(it.startMs - startMs) }
        val endCue = transcript.minByOrNull { kotlin.math.abs(it.endMs - endMs) }

        val snappedStart = if (startCue != null && kotlin.math.abs(startCue.startMs - startMs) <= 2500L) {
            startCue.startMs
        } else startMs

        val snappedEnd = if (endCue != null && kotlin.math.abs(endCue.endMs - endMs) <= 2500L && endCue.endMs > snappedStart + 3000L) {
            endCue.endMs
        } else endMs

        return snappedStart.coerceIn(0L, videoDurationMs) to snappedEnd.coerceIn(snappedStart + 1000L, videoDurationMs)
    }

    private fun removeHeavyOverlaps(clips: List<SelectedHighlight>): List<SelectedHighlight> {
        val sorted = clips.sortedByDescending { it.score }
        val accepted = mutableListOf<SelectedHighlight>()
        for (candidate in sorted) {
            val candLen = (candidate.endMs - candidate.startMs).coerceAtLeast(1L)
            val overlapsHeavily = accepted.any { existing ->
                val overlapStart = maxOf(existing.startMs, candidate.startMs)
                val overlapEnd = minOf(existing.endMs, candidate.endMs)
                val overlap = (overlapEnd - overlapStart).coerceAtLeast(0L)
                val existingLen = (existing.endMs - existing.startMs).coerceAtLeast(1L)
                overlap.toFloat() / minOf(candLen, existingLen).toFloat() > 0.20f
            }
            if (!overlapsHeavily) {
                accepted += candidate
            }
        }
        return accepted.sortedBy { it.startMs }
    }

    private fun fallbackSegments(
        videoDurationMs: Long,
        minDurationSec: Int,
        maxDurationSec: Int,
        requestedCount: Int
    ): List<SelectedHighlight> {
        if (videoDurationMs <= 3000L) return emptyList()
        val targetMs = ((minDurationSec + maxDurationSec) / 2 * 1000L).coerceAtMost(videoDurationMs)
        val maxPossible = (videoDurationMs / targetMs.coerceAtLeast(5000L)).toInt().coerceAtLeast(1)
        val count = minOf(requestedCount, maxPossible)
        val step = if (count > 1) (videoDurationMs - targetMs) / (count - 1) else 0L
        return (0 until count).map { idx ->
            val start = (idx * step).coerceIn(0L, (videoDurationMs - targetMs).coerceAtLeast(0L))
            val end = (start + targetMs).coerceAtMost(videoDurationMs)
            SelectedHighlight(
                title = "Scene Highlight ${idx + 1}",
                startMs = start,
                endMs = end,
                score = 72,
                reason = "Selected based on visual scene pacing across the source timeline.",
                tags = listOf("highlight", "shorts")
            )
        }
    }

    companion object {
        fun estimateWordTimings(text: String, startMs: Long, endMs: Long): List<WordTiming> {
            val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) return emptyList()
            val totalChars = words.sumOf { it.length }.coerceAtLeast(1)
            val duration = (endMs - startMs).coerceAtLeast(200L)
            var cursor = startMs
            return words.mapIndexed { index, word ->
                val share = if (index == words.lastIndex) {
                    (endMs - cursor).coerceAtLeast(50L)
                } else {
                    ((word.length.toFloat() / totalChars) * duration).toLong().coerceAtLeast(50L)
                }
                val wordStart = cursor
                val wordEnd = (cursor + share).coerceAtMost(endMs)
                cursor = wordEnd
                WordTiming(word = word, startMs = wordStart, endMs = wordEnd)
            }
        }
    }
}
