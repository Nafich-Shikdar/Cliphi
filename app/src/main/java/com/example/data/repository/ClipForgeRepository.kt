package com.example.data.repository

import android.net.Uri
import com.example.data.ai.GeminiVideoAiService
import com.example.data.local.ClipDao
import com.example.data.local.ClipEntity
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.SubtitleCueDao
import com.example.data.local.SubtitleCueEntity
import com.example.data.media.VideoProcessingEngine
import com.example.data.media.YouTubeMetadataFetcher
import com.example.model.AiSelectionMode
import com.example.model.AppSettings
import com.example.model.ExportQuality
import com.example.model.HardwareProfile
import com.example.model.LanguageOption
import com.example.model.ProcessingJobState
import com.example.model.ProcessingStage
import com.example.model.SelectedVideoInfo
import com.example.model.SubtitleCue
import com.example.model.SubtitleStyleConfig
import com.example.model.VerticalCropMode
import com.example.model.WordTiming
import com.example.model.YouTubeMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ClipForgeRepository(
    private val projectDao: ProjectDao,
    private val clipDao: ClipDao,
    private val subtitleCueDao: SubtitleCueDao,
    private val settingsDataStore: SettingsDataStore,
    private val videoEngine: VideoProcessingEngine,
    private val aiService: GeminiVideoAiService,
    private val youtubeFetcher: YouTubeMetadataFetcher
) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.observeAllProjects()
    val allClips: Flow<List<ClipEntity>> = clipDao.observeAllClips()
    val projectCount: Flow<Int> = projectDao.observeProjectCount()
    val clipCount: Flow<Int> = clipDao.observeClipCount()
    val processingCount: Flow<Int> = projectDao.observeProcessingCount()
    val appSettings: Flow<AppSettings> = settingsDataStore.appSettingsFlow
    val subtitleStyle: Flow<SubtitleStyleConfig> = settingsDataStore.subtitleStyleFlow

    fun observeClipsForProject(projectId: String): Flow<List<ClipEntity>> =
        clipDao.observeClipsForProject(projectId)

    fun observeCuesForClip(clipId: String): Flow<List<SubtitleCueEntity>> =
        subtitleCueDao.observeCuesForClip(clipId)

    suspend fun inspectLocalVideo(uri: Uri): SelectedVideoInfo =
        videoEngine.inspectSelectedVideo(uri)

    suspend fun fetchYouTubeMetadata(url: String): Result<YouTubeMetadata> =
        youtubeFetcher.fetchMetadata(url)

    fun getHardwareProfile(): HardwareProfile = videoEngine.getHardwareProfile()

    fun getStorageUsageBytes(): Long = videoEngine.calculateWorkspaceUsageBytes()

    fun clearExportCache(): Long = videoEngine.clearExportCache()

    suspend fun updateAppSettings(settings: AppSettings) =
        settingsDataStore.updateAppSettings(settings)

    suspend fun updateSubtitleStyle(style: SubtitleStyleConfig) =
        settingsDataStore.updateSubtitleStyle(style)

    suspend fun processNewProject(
        title: String,
        sourceUri: Uri,
        videoInfo: SelectedVideoInfo,
        sourceType: String,
        youtubeUrl: String?,
        minDurationSec: Int,
        maxDurationSec: Int,
        requestedClipCount: Int,
        languageOption: LanguageOption,
        selectionMode: AiSelectionMode,
        customPrompt: String,
        cropMode: VerticalCropMode,
        subtitleStyle: SubtitleStyleConfig,
        onStateUpdate: (ProcessingJobState) -> Unit
    ): ProjectEntity {
        val projectId = UUID.randomUUID().toString()
        val projectDir = videoEngine.getProjectDir(projectId)
        val settings = appSettings.first()

        fun emitStage(
            stage: ProcessingStage,
            stageProgress: Float,
            detail: String
        ) {
            val baseOverall = (stage.stepNumber - 1) / 8f
            val overall = if (stage == ProcessingStage.EXPORT_COMPLETED) 1f else (baseOverall + stageProgress / 8f).coerceIn(0f, 0.99f)
            onStateUpdate(
                ProcessingJobState(
                    projectId = projectId,
                    projectTitle = title,
                    currentStage = stage,
                    stageProgress = stageProgress.coerceIn(0f, 1f),
                    overallProgress = overall,
                    detailMessage = detail,
                    isRunning = stage != ProcessingStage.EXPORT_COMPLETED,
                    isCompleted = stage == ProcessingStage.EXPORT_COMPLETED,
                    errorMessage = null
                )
            )
        }

        // Stage 1: Uploading / Importing video into local studio workspace
        emitStage(ProcessingStage.UPLOADING_VIDEO, 0.05f, "Copying ${videoInfo.fileName} to studio workspace...")
        val (sourceFile, thumbPath) = videoEngine.copyVideoToWorkspace(
            uri = sourceUri,
            projectId = projectId,
            expectedBytes = videoInfo.fileSizeBytes
        ) { progress ->
            emitStage(
                ProcessingStage.UPLOADING_VIDEO,
                progress,
                "Importing video (${(progress * 100).toInt()}%)..."
            )
        }

        val initialProject = ProjectEntity(
            id = projectId,
            title = title,
            sourceType = sourceType,
            sourceUrl = youtubeUrl,
            sourceFilePath = sourceFile.absolutePath,
            thumbnailPath = thumbPath,
            durationMs = videoInfo.durationMs,
            fileSizeBytes = sourceFile.length(),
            width = videoInfo.width,
            height = videoInfo.height,
            languageCode = languageOption.code,
            selectionMode = selectionMode.name,
            customPrompt = customPrompt,
            targetMinSec = minDurationSec,
            targetMaxSec = maxDurationSec,
            requestedClipCount = requestedClipCount,
            cropMode = cropMode.name,
            status = "PROCESSING"
        )
        projectDao.insertProject(initialProject)

        try {
            // Stage 2: Extracting audio
            emitStage(ProcessingStage.EXTRACTING_AUDIO, 0.1f, "Extracting AAC audio track via Media3...")
            val audioChunks = videoEngine.extractAudioChunks(
                sourceFile = sourceFile,
                projectDir = projectDir,
                totalDurationMs = videoInfo.durationMs
            ) { p ->
                emitStage(
                    ProcessingStage.EXTRACTING_AUDIO,
                    p,
                    "Extracting audio (${(p * 100).toInt()}%)..."
                )
            }

            // Stage 3: Transcribing speech with Gemini
            emitStage(ProcessingStage.TRANSCRIBING_SPEECH, 0.05f, "Transcribing speech in ${languageOption.label}...")
            val fullTranscript = mutableListOf<SubtitleCue>()
            if (audioChunks.isNotEmpty()) {
                audioChunks.forEachIndexed { idx, chunk ->
                    val bytes = chunk.file.readBytes()
                    emitStage(
                        ProcessingStage.TRANSCRIBING_SPEECH,
                        (idx.toFloat() / audioChunks.size.toFloat()),
                        "Transcribing audio segment ${idx + 1} of ${audioChunks.size}..."
                    )
                    val cues = aiService.transcribeAudioChunk(
                        audioBytes = bytes,
                        mimeType = "audio/mp4",
                        chunkOffsetMs = chunk.offsetMs,
                        chunkDurationMs = chunk.durationMs,
                        languageOption = languageOption,
                        modelName = settings.aiModelName
                    )
                    fullTranscript += cues
                }
            }
            emitStage(
                ProcessingStage.TRANSCRIBING_SPEECH,
                1f,
                "Captured ${fullTranscript.size} transcript segments."
            )

            // Stage 4: Analyzing highlights
            emitStage(ProcessingStage.ANALYZING_HIGHLIGHTS, 0.25f, "Sampling video keyframes & analyzing pacing...")
            val keyframes = videoEngine.extractKeyframes(sourceFile, videoInfo.durationMs, count = 5)
            emitStage(ProcessingStage.ANALYZING_HIGHLIGHTS, 0.65f, "Evaluating ${selectionMode.label.lowercase()} with ${settings.aiModelName}...")
            val highlights = try {
                aiService.selectHighlights(
                    transcript = fullTranscript,
                    keyframeBitmaps = keyframes,
                    videoDurationMs = videoInfo.durationMs,
                    minDurationSec = minDurationSec,
                    maxDurationSec = maxDurationSec,
                    requestedCount = requestedClipCount,
                    selectionMode = selectionMode,
                    customPrompt = customPrompt,
                    modelName = settings.aiModelName
                )
            } finally {
                keyframes.forEach { it.recycle() }
            }

            // Stage 5: Selecting clips
            emitStage(
                ProcessingStage.SELECTING_CLIPS,
                1f,
                "Selected ${highlights.size} non-overlapping sentence-aligned clips."
            )

            // Stages 6 & 7: Sequential CPU-friendly vertical video rendering & subtitle generation
            val clipsDir = File(projectDir, "clips").apply { mkdirs() }
            val createdClips = mutableListOf<ClipEntity>()

            highlights.forEachIndexed { idx, highlight ->
                val clipId = UUID.randomUUID().toString()
                val clipCues = sliceCuesForClip(fullTranscript, highlight.startMs, highlight.endMs)

                emitStage(
                    ProcessingStage.GENERATING_VERTICAL_VIDEOS,
                    idx.toFloat() / highlights.size.coerceAtLeast(1).toFloat(),
                    "Rendering clip ${idx + 1}/${highlights.size}: ${highlight.title} (${settings.exportQuality.width}×${settings.exportQuality.height})..."
                )

                val focusCenterX = videoEngine.detectSpeakerCenterX(
                    videoFile = sourceFile,
                    startMs = highlight.startMs,
                    endMs = highlight.endMs,
                    cropMode = cropMode
                )

                val outputMp4 = File(clipsDir, "clip_${idx + 1}_${clipId.take(6)}.mp4")
                val srtFile = File(clipsDir, "clip_${idx + 1}_${clipId.take(6)}.srt")

                videoEngine.renderVerticalClip(
                    sourceFile = sourceFile,
                    sourceWidth = videoInfo.width,
                    sourceHeight = videoInfo.height,
                    outputFile = outputMp4,
                    startMs = highlight.startMs,
                    endMs = highlight.endMs,
                    cropMode = cropMode,
                    focusCenterX = focusCenterX,
                    exportQuality = settings.exportQuality,
                    cues = clipCues,
                    subtitleStyle = subtitleStyle
                ) { renderProgress ->
                    val combinedProgress = (idx + renderProgress) / highlights.size.coerceAtLeast(1).toFloat()
                    emitStage(
                        ProcessingStage.GENERATING_VERTICAL_VIDEOS,
                        combinedProgress,
                        "Encoding 9:16 H.264 clip ${idx + 1}/${highlights.size} (${(renderProgress * 100).toInt()}%)..."
                    )
                }

                emitStage(
                    ProcessingStage.CREATING_SUBTITLES,
                    (idx + 1).toFloat() / highlights.size.coerceAtLeast(1).toFloat(),
                    "Writing synchronized SRT & subtitles for clip ${idx + 1}..."
                )
                videoEngine.writeSrtFile(clipCues, srtFile)

                val clipThumb = videoEngine.captureFrameThumbnail(
                    videoFile = outputMp4,
                    timeUs = 500_000L,
                    outFile = File(clipsDir, "thumb_${clipId.take(6)}.jpg")
                )

                val subtitleStatus = when {
                    clipCues.isEmpty() -> "NONE"
                    subtitleStyle.burnInSubtitles -> "BURNED_IN"
                    else -> "SRT_ONLY"
                }

                val clipEntity = ClipEntity(
                    id = clipId,
                    projectId = projectId,
                    title = highlight.title,
                    startMs = highlight.startMs,
                    endMs = highlight.endMs,
                    durationMs = highlight.endMs - highlight.startMs,
                    highlightScore = highlight.score,
                    highlightReason = highlight.reason,
                    tagsCsv = highlight.tags.joinToString(","),
                    outputVideoPath = outputMp4.absolutePath,
                    srtFilePath = srtFile.absolutePath,
                    thumbnailPath = clipThumb,
                    fileSizeBytes = outputMp4.length(),
                    subtitleStatus = subtitleStatus,
                    cropMode = cropMode.name,
                    focusCenterX = focusCenterX,
                    subtitleTemplateName = subtitleStyle.template.name
                )
                clipDao.insertClip(clipEntity)
                subtitleCueDao.insertCues(clipCues.map { it.toEntity(clipId) })
                createdClips += clipEntity
            }

            val completed = initialProject.copy(status = "COMPLETED")
            projectDao.updateProject(completed)
            emitStage(
                ProcessingStage.EXPORT_COMPLETED,
                1f,
                "Generated ${createdClips.size} vertical 9:16 shorts ready for preview & export."
            )
            return completed
        } catch (e: CancellationException) {
            projectDao.updateProject(initialProject.copy(status = "CANCELLED", errorMessage = "Cancelled by user"))
            throw e
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Processing failed."
            projectDao.updateProject(initialProject.copy(status = "FAILED", errorMessage = msg))
            throw e
        }
    }

    suspend fun reRenderClip(
        clip: ClipEntity,
        newStartMs: Long,
        newEndMs: Long,
        cropMode: VerticalCropMode,
        updatedCues: List<SubtitleCue>,
        subtitleStyle: SubtitleStyleConfig,
        exportQuality: ExportQuality,
        onProgress: (Float) -> Unit
    ): ClipEntity {
        val project = projectDao.getProjectById(clip.projectId)
            ?: throw IllegalStateException("Parent project not found.")
        val sourceFile = File(project.sourceFilePath)
        if (!sourceFile.exists()) throw IllegalStateException("Source video file is no longer available on disk.")

        val focusCenterX = videoEngine.detectSpeakerCenterX(
            videoFile = sourceFile,
            startMs = newStartMs,
            endMs = newEndMs,
            cropMode = cropMode
        )

        val outputFile = File(clip.outputVideoPath)
        val tempOut = File(outputFile.parentFile, "rerender_${System.currentTimeMillis()}.mp4")
        videoEngine.renderVerticalClip(
            sourceFile = sourceFile,
            sourceWidth = project.width,
            sourceHeight = project.height,
            outputFile = tempOut,
            startMs = newStartMs,
            endMs = newEndMs,
            cropMode = cropMode,
            focusCenterX = focusCenterX,
            exportQuality = exportQuality,
            cues = updatedCues,
            subtitleStyle = subtitleStyle,
            onProgress = onProgress
        )
        if (outputFile.exists()) outputFile.delete()
        tempOut.renameTo(outputFile)

        val srtFile = File(clip.srtFilePath)
        videoEngine.writeSrtFile(updatedCues, srtFile)

        subtitleCueDao.deleteCuesForClip(clip.id)
        subtitleCueDao.insertCues(updatedCues.map { it.toEntity(clip.id) })

        val subtitleStatus = when {
            updatedCues.isEmpty() -> "NONE"
            subtitleStyle.burnInSubtitles -> "BURNED_IN"
            else -> "SRT_ONLY"
        }

        val updatedClip = clip.copy(
            startMs = newStartMs,
            endMs = newEndMs,
            durationMs = newEndMs - newStartMs,
            fileSizeBytes = outputFile.length(),
            subtitleStatus = subtitleStatus,
            cropMode = cropMode.name,
            focusCenterX = focusCenterX,
            subtitleTemplateName = subtitleStyle.template.name
        )
        clipDao.updateClip(updatedClip)
        return updatedClip
    }

    suspend fun saveEditedCuesAndSrt(clip: ClipEntity, updatedCues: List<SubtitleCue>) {
        subtitleCueDao.deleteCuesForClip(clip.id)
        subtitleCueDao.insertCues(updatedCues.map { it.toEntity(clip.id) })
        videoEngine.writeSrtFile(updatedCues, File(clip.srtFilePath))
    }

    suspend fun downloadClipMp4(clip: ClipEntity): Uri? {
        val safeTitle = clip.title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(36).ifBlank { "clipforge_short" }
        return videoEngine.saveFileToDownloads(
            sourceFile = File(clip.outputVideoPath),
            displayName = "${safeTitle}.mp4",
            mimeType = "video/mp4"
        )
    }

    suspend fun downloadClipSrt(clip: ClipEntity): Uri? {
        val safeTitle = clip.title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(36).ifBlank { "clipforge_subtitles" }
        return videoEngine.saveFileToDownloads(
            sourceFile = File(clip.srtFilePath),
            displayName = "${safeTitle}.srt",
            mimeType = "application/x-subrip"
        )
    }

    suspend fun downloadClipsZip(projectTitle: String, clips: List<ClipEntity>): Pair<File, Uri?> {
        val zipFile = videoEngine.createClipsZipArchive(projectTitle, clips)
        val uri = videoEngine.saveFileToDownloads(
            sourceFile = zipFile,
            displayName = zipFile.name,
            mimeType = "application/zip"
        )
        return zipFile to uri
    }

    suspend fun deleteClip(clip: ClipEntity) {
        runCatching { File(clip.outputVideoPath).delete() }
        runCatching { File(clip.srtFilePath).delete() }
        clip.thumbnailPath?.let { runCatching { File(it).delete() } }
        clipDao.deleteClipById(clip.id)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        videoEngine.getProjectDir(project.id).deleteRecursively()
        projectDao.deleteProjectById(project.id)
    }

    private fun sliceCuesForClip(
        fullTranscript: List<SubtitleCue>,
        clipStartMs: Long,
        clipEndMs: Long
    ): List<SubtitleCue> {
        val clipDuration = (clipEndMs - clipStartMs).coerceAtLeast(500L)
        return fullTranscript
            .filter { it.endMs > clipStartMs && it.startMs < clipEndMs }
            .map { cue ->
                val relStart = (cue.startMs - clipStartMs).coerceIn(0L, clipDuration)
                val relEnd = (cue.endMs - clipStartMs).coerceIn(relStart + 200L, clipDuration)
                val relWords = cue.words.mapNotNull { w ->
                    if (w.endMs <= clipStartMs || w.startMs >= clipEndMs) null
                    else WordTiming(
                        word = w.word,
                        startMs = (w.startMs - clipStartMs).coerceIn(relStart, relEnd),
                        endMs = (w.endMs - clipStartMs).coerceIn(relStart, relEnd)
                    )
                }.ifEmpty {
                    GeminiVideoAiService.estimateWordTimings(cue.text, relStart, relEnd)
                }
                cue.copy(
                    id = UUID.randomUUID().toString(),
                    startMs = relStart,
                    endMs = relEnd,
                    words = relWords
                )
            }
    }

    companion object {
        fun entityToCue(entity: SubtitleCueEntity): SubtitleCue {
            val words = mutableListOf<WordTiming>()
            runCatching {
                val arr = JSONArray(entity.wordsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    words += WordTiming(
                        word = obj.getString("w"),
                        startMs = obj.getLong("s"),
                        endMs = obj.getLong("e")
                    )
                }
            }
            return SubtitleCue(
                id = entity.id,
                startMs = entity.startMs,
                endMs = entity.endMs,
                text = entity.text,
                speaker = entity.speaker,
                language = entity.language,
                words = words
            )
        }

        private fun SubtitleCue.toEntity(clipId: String): SubtitleCueEntity {
            val arr = JSONArray()
            words.forEach { w ->
                arr.put(JSONObject().apply {
                    put("w", w.word)
                    put("s", w.startMs)
                    put("e", w.endMs)
                })
            }
            return SubtitleCueEntity(
                id = id,
                clipId = clipId,
                startMs = startMs,
                endMs = endMs,
                text = text,
                speaker = speaker,
                language = language,
                wordsJson = arr.toString()
            )
        }
    }
}
