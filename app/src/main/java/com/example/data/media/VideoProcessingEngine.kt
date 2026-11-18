package com.example.data.media

import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.FaceDetector
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.R
import com.example.data.local.ClipEntity
import com.example.model.ExportQuality
import com.example.model.HardwareProfile
import com.example.model.SelectedVideoInfo
import com.example.model.SubtitleCue
import com.example.model.SubtitleFontFamily
import com.example.model.SubtitlePosition
import com.example.model.SubtitleStyleConfig
import com.example.model.VerticalCropMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
class VideoProcessingEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun getProjectsRootDir(): File = File(context.filesDir, "projects").apply { mkdirs() }

    fun getProjectDir(projectId: String): File = File(getProjectsRootDir(), projectId).apply { mkdirs() }

    fun getHardwareProfile(): HardwareProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024L * 1024L)

        val statFs = StatFs(context.filesDir.absolutePath)
        val availableStorageMb = (statFs.availableBlocksLong * statFs.blockSizeLong) / (1024L * 1024L)

        return HardwareProfile(
            cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            totalRamMb = totalRamMb,
            availableStorageMb = availableStorageMb,
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        )
    }

    fun calculateWorkspaceUsageBytes(): Long {
        fun folderSize(dir: File): Long {
            if (!dir.exists()) return 0L
            return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        return folderSize(getProjectsRootDir()) + folderSize(File(context.cacheDir, "exports"))
    }

    fun clearExportCache(): Long {
        val cacheDir = File(context.cacheDir, "exports")
        val freed = if (cacheDir.exists()) cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
        cacheDir.deleteRecursively()
        return freed
    }

    suspend fun inspectSelectedVideo(uri: Uri): SelectedVideoInfo = withContext(Dispatchers.IO) {
        var displayName = "video_input.mp4"
        var sizeBytes = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
            }
        }

        val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val (w, h) = if (rotation == 90 || rotation == 270) rawH to rawW else rawW to rawH

            SelectedVideoInfo(
                uriString = uri.toString(),
                fileName = displayName,
                fileSizeBytes = sizeBytes,
                durationMs = durationMs,
                width = w,
                height = h,
                mimeType = mimeType
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun copyVideoToWorkspace(
        uri: Uri,
        projectId: String,
        expectedBytes: Long,
        onProgress: (Float) -> Unit
    ): Pair<File, String?> = withContext(Dispatchers.IO) {
        val projectDir = getProjectDir(projectId)
        val destFile = File(projectDir, "source_video.mp4")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var copied = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    copied += read
                    if (expectedBytes > 0L) {
                        onProgress((copied.toFloat() / expectedBytes.toFloat()).coerceIn(0f, 1f))
                    }
                }
                output.flush()
            }
        } ?: throw IllegalStateException("Unable to read the selected video file.")

        onProgress(1f)
        val thumbPath = captureFrameThumbnail(destFile, 1_500_000L, File(projectDir, "project_thumb.jpg"))
        destFile to thumbPath
    }

    suspend fun captureFrameThumbnail(videoFile: File, timeUs: Long, outFile: File): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (bmp != null) {
                val scaled = scaleBitmapMax(bmp, 640)
                FileOutputStream(outFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                if (scaled !== bmp) scaled.recycle()
                bmp.recycle()
                outFile.absolutePath
            } else null
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun extractKeyframes(videoFile: File, durationMs: Long, count: Int = 5): List<Bitmap> = withContext(Dispatchers.IO) {
        if (durationMs <= 0L) return@withContext emptyList()
        val retriever = MediaMetadataRetriever()
        val bitmaps = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val step = durationMs / (count + 1)
            for (i in 1..count) {
                val timeUs = (step * i) * 1000L
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val scaled = scaleBitmapMax(frame, 480)
                    if (scaled !== frame) frame.recycle()
                    bitmaps += scaled
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }
        bitmaps
    }

    fun hasAudioTrack(videoFile: File): Boolean {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(videoFile.absolutePath)
            (0 until extractor.trackCount).any { idx ->
                val mime = extractor.getTrackFormat(idx).getString(MediaFormat.KEY_MIME) ?: ""
                mime.startsWith("audio/")
            }
        } catch (_: Exception) {
            false
        } finally {
            runCatching { extractor.release() }
        }
    }

    suspend fun extractAudioChunks(
        sourceFile: File,
        projectDir: File,
        totalDurationMs: Long,
        onProgress: (Float) -> Unit
    ): List<AudioChunk> {
        if (!hasAudioTrack(sourceFile)) {
            onProgress(1f)
            return emptyList()
        }

        val fullAudioFile = File(projectDir, "extracted_audio.mp4")
        if (fullAudioFile.exists()) fullAudioFile.delete()

        val mediaItem = MediaItem.fromUri(Uri.fromFile(sourceFile))
        val edited = EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true)
            .build()

        runTransformer(edited, fullAudioFile, audioOnly = true) { p ->
            onProgress(p * 0.85f)
        }

        // Gemini inline data limit is 20MB; keep chunks <= 11MB so base64 remains well under 18MB
        val maxChunkBytes = 11L * 1024L * 1024L
        if (fullAudioFile.length() <= maxChunkBytes || totalDurationMs <= 60_000L) {
            onProgress(1f)
            return listOf(
                AudioChunk(
                    file = fullAudioFile,
                    offsetMs = 0L,
                    durationMs = totalDurationMs
                )
            )
        }

        val numChunks = ((fullAudioFile.length() / maxChunkBytes) + 1).toInt().coerceIn(2, 8)
        val chunkDurMs = (totalDurationMs / numChunks).coerceAtLeast(30_000L)
        val chunks = mutableListOf<AudioChunk>()
        var cursorMs = 0L
        var idx = 0
        while (cursorMs < totalDurationMs) {
            val endMs = (cursorMs + chunkDurMs).coerceAtMost(totalDurationMs)
            val chunkFile = File(projectDir, "audio_chunk_$idx.mp4")
            if (chunkFile.exists()) chunkFile.delete()
            val clippedItem = MediaItem.Builder()
                .setUri(Uri.fromFile(fullAudioFile))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(cursorMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()
            val editedChunk = EditedMediaItem.Builder(clippedItem).build()
            runTransformer(editedChunk, chunkFile, audioOnly = true) {}
            chunks += AudioChunk(
                file = chunkFile,
                offsetMs = cursorMs,
                durationMs = endMs - cursorMs
            )
            cursorMs = endMs
            idx++
            onProgress(0.85f + 0.15f * (cursorMs.toFloat() / totalDurationMs.toFloat()))
        }
        onProgress(1f)
        return chunks
    }

    suspend fun detectSpeakerCenterX(
        videoFile: File,
        startMs: Long,
        endMs: Long,
        cropMode: VerticalCropMode
    ): Float = withContext(Dispatchers.IO) {
        if (cropMode != VerticalCropMode.SMART_FACE_TRACK) return@withContext 0.5f

        val retriever = MediaMetadataRetriever()
        val centers = mutableListOf<Float>()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val sampleTimes = listOf(
                startMs + (endMs - startMs) / 5,
                startMs + (endMs - startMs) / 2,
                startMs + (endMs - startMs) * 4 / 5
            )
            for (ms in sampleTimes) {
                val frame = retriever.getFrameAtTime(ms * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: continue
                val scaled = scaleBitmapMax(frame, 320)
                if (scaled !== frame) frame.recycle()
                val rgb565 = scaled.copy(Bitmap.Config.RGB_565, false)
                scaled.recycle()

                // FaceDetector requires even width
                val evenW = if (rgb565.width % 2 == 0) rgb565.width else rgb565.width - 1
                val detectBmp = if (evenW != rgb565.width && evenW > 2) {
                    Bitmap.createBitmap(rgb565, 0, 0, evenW, rgb565.height).also { rgb565.recycle() }
                } else rgb565

                val detector = FaceDetector(detectBmp.width, detectBmp.height, 2)
                val faces = arrayOfNulls<FaceDetector.Face>(2)
                val found = detector.findFaces(detectBmp, faces)
                if (found > 0) {
                    val midPoint = android.graphics.PointF()
                    faces[0]?.getMidPoint(midPoint)
                    if (midPoint.x > 0f) {
                        centers += (midPoint.x / detectBmp.width.toFloat()).coerceIn(0.18f, 0.82f)
                    }
                }
                detectBmp.recycle()
            }
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }
        if (centers.isNotEmpty()) centers.average().toFloat() else 0.5f
    }

    suspend fun renderVerticalClip(
        sourceFile: File,
        sourceWidth: Int,
        sourceHeight: Int,
        outputFile: File,
        startMs: Long,
        endMs: Long,
        cropMode: VerticalCropMode,
        focusCenterX: Float,
        exportQuality: ExportQuality,
        cues: List<SubtitleCue>,
        subtitleStyle: SubtitleStyleConfig,
        onProgress: (Float) -> Unit
    ) {
        if (outputFile.exists()) outputFile.delete()

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(sourceFile))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()

        val videoEffects = mutableListOf<Effect>()

        // 1. Smart Horizontal Pan + 9:16 Crop or Auto Zoom in NDC coordinates [-1f, 1f]
        val srcAspect = if (sourceHeight > 0) sourceWidth.toFloat() / sourceHeight.toFloat() else 16f / 9f
        val targetAspect = 9f / 16f

        if (srcAspect > targetAspect + 0.05f) {
            val visibleWidthFraction = (targetAspect / srcAspect).coerceIn(0.25f, 1f)
            val zoomFactor = if (cropMode == VerticalCropMode.AUTO_ZOOM) 0.88f else 1.0f
            val cropSpanWidth = (visibleWidthFraction * zoomFactor) * 2f // in [-1, 1] space of width 2.0
            val ndcCenter = (focusCenterX.coerceIn(0f, 1f) * 2f - 1f)
            val halfSpan = cropSpanWidth / 2f
            val clampedCenter = ndcCenter.coerceIn(-1f + halfSpan, 1f - halfSpan)
            val left = (clampedCenter - halfSpan).coerceIn(-1f, 1f)
            val right = (clampedCenter + halfSpan).coerceIn(-1f, 1f)
            val topBottomSpan = if (cropMode == VerticalCropMode.AUTO_ZOOM) 0.88f else 1.0f
            videoEffects += Crop(left, right, -topBottomSpan, topBottomSpan)
        } else if (cropMode == VerticalCropMode.AUTO_ZOOM) {
            videoEffects += Crop(-0.88f, 0.88f, -0.88f, 0.88f)
        }

        // 2. Scale strictly to target 9:16 resolution (e.g., 1080x1920) without black borders
        videoEffects += Presentation.createForWidthAndHeight(
            exportQuality.width,
            exportQuality.height,
            Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
        )

        // 3. Burned-in Synchronized Bengali/English Subtitle BitmapOverlay
        if (subtitleStyle.burnInSubtitles && cues.isNotEmpty()) {
            val subtitleOverlay = SubtitleBitmapOverlay(
                context = context,
                videoWidth = exportQuality.width,
                videoHeight = exportQuality.height,
                cues = cues,
                subtitleStyle = subtitleStyle
            )
            videoEffects += OverlayEffect(listOf(subtitleOverlay))
        }

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        runTransformer(editedMediaItem, outputFile, audioOnly = false, onProgress = onProgress)
    }

    private suspend fun runTransformer(
        editedMediaItem: EditedMediaItem,
        outputFile: File,
        audioOnly: Boolean,
        onProgress: (Float) -> Unit
    ) = suspendCancellableCoroutine { continuation ->
        mainHandler.post {
            val builder = Transformer.Builder(context)
            if (!audioOnly) {
                builder.setVideoMimeType(MimeTypes.VIDEO_H264)
            }
            builder.setAudioMimeType(MimeTypes.AUDIO_AAC)

            var finished = false
            lateinit var transformer: Transformer
            val progressRunnable = object : Runnable {
                val holder = ProgressHolder()
                override fun run() {
                    if (finished) return
                    val state = transformer.getProgress(holder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress((holder.progress / 100f).coerceIn(0f, 1f))
                    }
                    mainHandler.postDelayed(this, 250L)
                }
            }

            builder.addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (!finished) {
                        finished = true
                        mainHandler.removeCallbacks(progressRunnable)
                        onProgress(1f)
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    if (!finished) {
                        finished = true
                        mainHandler.removeCallbacks(progressRunnable)
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    exportException.localizedMessage ?: "Media3 video export failed."
                                )
                            )
                        }
                    }
                }
            })

            transformer = builder.build()
            continuation.invokeOnCancellation {
                mainHandler.post {
                    finished = true
                    mainHandler.removeCallbacks(progressRunnable)
                    runCatching { transformer.cancel() }
                }
            }

            transformer.start(editedMediaItem, outputFile.absolutePath)
            mainHandler.post(progressRunnable)
        }
    }

    fun generateSrtContent(cues: List<SubtitleCue>): String {
        val sb = StringBuilder()
        cues.sortedBy { it.startMs }.forEachIndexed { idx, cue ->
            sb.append(idx + 1).append('\n')
            sb.append(formatSrtTimestamp(cue.startMs))
                .append(" --> ")
                .append(formatSrtTimestamp(cue.endMs))
                .append('\n')
            sb.append(cue.text.trim()).append("\n\n")
        }
        return sb.toString()
    }

    fun writeSrtFile(cues: List<SubtitleCue>, outFile: File) {
        outFile.parentFile?.mkdirs()
        outFile.writeText(generateSrtContent(cues), Charsets.UTF_8)
    }

    suspend fun saveFileToDownloads(
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ClipForge")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val clipForgeDir = File(downloadsDir, "ClipForge").apply { mkdirs() }
            val dest = File(clipForgeDir, displayName)
            sourceFile.copyTo(dest, overwrite = true)
            Uri.fromFile(dest)
        }
    }

    suspend fun createClipsZipArchive(
        archiveBaseName: String,
        clips: List<ClipEntity>
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = archiveBaseName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "clipforge_clips" }
        val zipFile = File(exportDir, "${safeName}_shorts.zip")
        if (zipFile.exists()) zipFile.delete()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            val usedNames = mutableSetOf<String>()
            clips.forEachIndexed { index, clip ->
                val baseTitle = clip.title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(32).ifBlank { "clip_${index + 1}" }
                var entryPrefix = "${index + 1}_$baseTitle"
                while (!usedNames.add(entryPrefix)) {
                    entryPrefix = "${entryPrefix}_dup"
                }

                val mp4 = File(clip.outputVideoPath)
                if (mp4.exists()) {
                    zos.putNextEntry(ZipEntry("$entryPrefix.mp4"))
                    BufferedInputStream(FileInputStream(mp4)).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
                val srt = File(clip.srtFilePath)
                if (srt.exists()) {
                    zos.putNextEntry(ZipEntry("$entryPrefix.srt"))
                    BufferedInputStream(FileInputStream(srt)).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        zipFile
    }

    private fun scaleBitmapMax(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDim && h <= maxDim) return source
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        val newW = (w * ratio).toInt().coerceAtLeast(2)
        val newH = (h * ratio).toInt().coerceAtLeast(2)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }

    private fun formatSrtTimestamp(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val hours = safeMs / 3_600_000L
        val mins = (safeMs % 3_600_000L) / 60_000L
        val secs = (safeMs % 60_000L) / 1000L
        val millis = safeMs % 1000L
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, mins, secs, millis)
    }

    data class AudioChunk(
        val file: File,
        val offsetMs: Long,
        val durationMs: Long
    )
}

@UnstableApi
private class SubtitleBitmapOverlay(
    private val context: Context,
    private val videoWidth: Int,
    private val videoHeight: Int,
    private val cues: List<SubtitleCue>,
    private val subtitleStyle: SubtitleStyleConfig
) : BitmapOverlay() {

    private var lastCueId: String? = null
    private var lastWordIdx: Int = -2
    private var cachedBitmap: Bitmap = Bitmap.createBitmap(
        videoWidth.coerceAtLeast(64),
        videoHeight.coerceAtLeast(64),
        Bitmap.Config.ARGB_8888
    )

    private val typeface: Typeface by lazy {
        val containsBengali = cues.any { cue ->
            cue.text.any { ch -> ch in '\u0980'..'\u09FF' }
        }
        val fontRes = when {
            containsBengali -> R.font.noto_sans_bengali
            subtitleStyle.fontFamily == SubtitleFontFamily.NOTO_SANS_BENGALI -> R.font.noto_sans_bengali
            subtitleStyle.fontFamily == SubtitleFontFamily.MONTSERRAT -> R.font.montserrat
            subtitleStyle.fontFamily == SubtitleFontFamily.ROBOTO_MONO -> R.font.roboto_mono
            else -> R.font.inter
        }
        ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT_BOLD
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = presentationTimeUs / 1000L
        val activeCue = cues.firstOrNull { timeMs in it.startMs..it.endMs }

        if (activeCue == null) {
            if (lastCueId != null) {
                lastCueId = null
                lastWordIdx = -2
                cachedBitmap.eraseColor(Color.TRANSPARENT)
            }
            return cachedBitmap
        }

        val activeWordIdx = activeCue.words.indexOfFirst { timeMs in it.startMs..it.endMs }
        if (activeCue.id == lastCueId && activeWordIdx == lastWordIdx) {
            return cachedBitmap
        }

        lastCueId = activeCue.id
        lastWordIdx = activeWordIdx
        drawCueOnBitmap(activeCue, activeWordIdx)
        return cachedBitmap
    }

    private fun drawCueOnBitmap(cue: SubtitleCue, activeWordIdx: Int) {
        cachedBitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(cachedBitmap)

        val scaleFactor = videoWidth / 390f
        val textSizePx = (subtitleStyle.fontSizeSp * scaleFactor).coerceIn(28f, 110f)
        val horizontalPadding = (videoWidth * 0.08f).toInt()
        val maxTextWidth = (videoWidth - horizontalPadding * 2).coerceAtLeast(100)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            typeface = this@SubtitleBitmapOverlay.typeface
            color = subtitleStyle.textColorHex.toInt()
            isFakeBoldText = true
        }

        val outlinePaint = TextPaint(textPaint).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = (textSizePx * 0.12f).coerceAtLeast(3f)
            color = subtitleStyle.outlineColorHex.toInt()
        }

        val layoutOutline = StaticLayout.Builder.obtain(
            cue.text, 0, cue.text.length, outlinePaint, maxTextWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build()

        val layoutFill = StaticLayout.Builder.obtain(
            cue.text, 0, cue.text.length, textPaint, maxTextWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build()

        val textHeight = layoutFill.height.toFloat()
        val contentWidth = (0 until layoutFill.lineCount)
            .maxOfOrNull { layoutFill.getLineWidth(it) }
            ?.coerceAtMost(maxTextWidth.toFloat()) ?: maxTextWidth.toFloat()

        val boxPadX = 24f * scaleFactor
        val boxPadY = 14f * scaleFactor

        val yTop = when (subtitleStyle.position) {
            SubtitlePosition.TOP -> videoHeight * 0.14f
            SubtitlePosition.CENTER -> (videoHeight - textHeight) / 2f
            SubtitlePosition.BOTTOM -> videoHeight * 0.77f - textHeight
        }

        val boxRect = RectF(
            (videoWidth - contentWidth) / 2f - boxPadX,
            yTop - boxPadY,
            (videoWidth + contentWidth) / 2f + boxPadX,
            yTop + textHeight + boxPadY
        )

        val bgAlpha = ((subtitleStyle.backgroundColorHex ushr 24) and 0xFF).toInt()
        if (bgAlpha > 0) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = subtitleStyle.backgroundColorHex.toInt()
                this.style = Paint.Style.FILL
            }
            canvas.drawRoundRect(boxRect, 18f * scaleFactor, 18f * scaleFactor, bgPaint)
        }

        if (activeWordIdx >= 0 && bgAlpha == 0) {
            val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = (subtitleStyle.highlightColorHex.toInt() and 0x00FFFFFF) or 0x55000000
                this.style = Paint.Style.FILL
            }
            canvas.drawRoundRect(boxRect, 16f * scaleFactor, 16f * scaleFactor, highlightPaint)
        }

        canvas.save()
        canvas.translate(horizontalPadding.toFloat(), yTop)
        layoutOutline.draw(canvas)
        layoutFill.draw(canvas)
        canvas.restore()
    }
}
