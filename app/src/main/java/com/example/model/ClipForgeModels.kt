package com.example.model

enum class NavDestination(val route: String, val label: String) {
    DASHBOARD("dashboard", "Dashboard"),
    NEW_PROJECT("new_project", "New Project"),
    PROJECTS("projects", "My Projects"),
    CLIPS("clips", "Generated Clips"),
    SUBTITLE_STUDIO("subtitle_studio", "Subtitle Studio"),
    SETTINGS("settings", "Settings")
}

enum class DurationPreset(val label: String, val minSeconds: Int, val maxSeconds: Int) {
    SHORT_15_30("15–30 sec", 15, 30),
    MEDIUM_30_45("30–45 sec", 30, 45),
    LONG_45_60("45–60 sec", 45, 60),
    CUSTOM("Custom", 20, 60)
}

enum class LanguageOption(val label: String, val code: String) {
    AUTO_DETECT("Auto Detect", "auto"),
    BENGALI("Bengali (বাংলা)", "bn"),
    ENGLISH("English", "en")
}

enum class AiSelectionMode(val label: String, val promptGuidance: String) {
    MOST_ENGAGING("Most engaging moments", "high-energy hooks, punchy insights, compelling delivery, strong opening lines"),
    IMPORTANT_INFO("Important information", "key facts, announcements, core takeaways, actionable insights"),
    EDUCATIONAL("Educational highlights", "clear explanations, step-by-step teaching moments, definitions, tips"),
    STORYTELLING("Storytelling moments", "narrative arcs, personal anecdotes, turning points, vivid buildup and payoff"),
    EMOTIONAL("Emotional moments", "heartfelt reflections, inspiring passion, authentic reactions, memorable quotes"),
    CUSTOM("Custom AI selection", "follow the user's custom highlight instruction carefully")
}

enum class VerticalCropMode(val label: String, val description: String) {
    SMART_FACE_TRACK("Smart Face Tracking", "Detects speaker faces and centers the 9:16 frame around them"),
    AUTO_ZOOM("Dynamic Auto Zoom", "Slight punch-in zoom with center framing to fill 9:16 cleanly"),
    CENTER_CROP("Center Crop", "Fast CPU-friendly center crop without black borders")
}

enum class ExportQuality(val label: String, val width: Int, val height: Int, val ramHint: String) {
    FHD_1080X1920("1080 × 1920 (Full HD 9:16)", 1080, 1920, "Recommended for crisp Reels/Shorts"),
    HD_720X1280("720 × 1280 (HD 9:16)", 720, 1280, "Balanced speed & quality for 8GB RAM"),
    FAST_540X960("540 × 960 (CPU Fast)", 540, 960, "Lowest memory usage & fastest render")
}

enum class SubtitleTemplate(
    val label: String,
    val defaultFont: SubtitleFontFamily,
    val defaultTextHex: Long,
    val defaultBgHex: Long,
    val defaultOutlineHex: Long,
    val defaultHighlightHex: Long,
    val defaultPosition: SubtitlePosition,
    val defaultFontSizeSp: Float
) {
    MINIMAL_WHITE(
        label = "Minimal White",
        defaultFont = SubtitleFontFamily.INTER,
        defaultTextHex = 0xFFF8FAFC,
        defaultBgHex = 0x990B0D14,
        defaultOutlineHex = 0xFF000000,
        defaultHighlightHex = 0xFFFFFFFF,
        defaultPosition = SubtitlePosition.BOTTOM,
        defaultFontSizeSp = 20f
    ),
    BOLD_CREATOR(
        label = "Bold Creator",
        defaultFont = SubtitleFontFamily.MONTSERRAT,
        defaultTextHex = 0xFFFFFFFF,
        defaultBgHex = 0x00000000,
        defaultOutlineHex = 0xFF000000,
        defaultHighlightHex = 0xFFFACC15,
        defaultPosition = SubtitlePosition.CENTER,
        defaultFontSizeSp = 25f
    ),
    MODERN_HIGHLIGHT(
        label = "Modern Highlight",
        defaultFont = SubtitleFontFamily.INTER,
        defaultTextHex = 0xFFF8FAFC,
        defaultBgHex = 0xCC141927,
        defaultOutlineHex = 0xFF0B0D14,
        defaultHighlightHex = 0xFF8B5CF6,
        defaultPosition = SubtitlePosition.BOTTOM,
        defaultFontSizeSp = 22f
    ),
    PODCAST_STYLE(
        label = "Podcast Style",
        defaultFont = SubtitleFontFamily.ROBOTO_MONO,
        defaultTextHex = 0xFFF1F5F9,
        defaultBgHex = 0xD90B0D14,
        defaultOutlineHex = 0xFF1E293B,
        defaultHighlightHex = 0xFF38BDF8,
        defaultPosition = SubtitlePosition.BOTTOM,
        defaultFontSizeSp = 19f
    ),
    PREMIUM_PURPLE(
        label = "Premium Purple",
        defaultFont = SubtitleFontFamily.INTER,
        defaultTextHex = 0xFFF8FAFC,
        defaultBgHex = 0xD92E1E59,
        defaultOutlineHex = 0xFF8B5CF6,
        defaultHighlightHex = 0xFFA78BFA,
        defaultPosition = SubtitlePosition.BOTTOM,
        defaultFontSizeSp = 22f
    )
}

enum class SubtitlePosition(val label: String) {
    BOTTOM("Bottom"),
    CENTER("Center"),
    TOP("Top")
}

enum class SubtitleFontFamily(val label: String) {
    INTER("Inter"),
    NOTO_SANS_BENGALI("Noto Sans Bengali (বাংলা)"),
    MONTSERRAT("Montserrat"),
    ROBOTO_MONO("Roboto Mono")
}

data class SubtitleStyleConfig(
    val template: SubtitleTemplate = SubtitleTemplate.PREMIUM_PURPLE,
    val fontFamily: SubtitleFontFamily = SubtitleFontFamily.INTER,
    val fontSizeSp: Float = 22f,
    val textColorHex: Long = 0xFFF8FAFC,
    val backgroundColorHex: Long = 0xD92E1E59,
    val outlineColorHex: Long = 0xFF0B0D14,
    val highlightColorHex: Long = 0xFFA78BFA,
    val position: SubtitlePosition = SubtitlePosition.BOTTOM,
    val burnInSubtitles: Boolean = true
)

data class WordTiming(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

data class SubtitleCue(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val speaker: String = "Speaker 1",
    val language: String = "en",
    val words: List<WordTiming> = emptyList()
)

enum class ProcessingStage(val stepNumber: Int, val label: String) {
    UPLOADING_VIDEO(1, "Uploading video"),
    EXTRACTING_AUDIO(2, "Extracting audio"),
    TRANSCRIBING_SPEECH(3, "Transcribing speech"),
    ANALYZING_HIGHLIGHTS(4, "Analyzing highlights"),
    SELECTING_CLIPS(5, "Selecting clips"),
    GENERATING_VERTICAL_VIDEOS(6, "Generating vertical videos"),
    CREATING_SUBTITLES(7, "Creating subtitles"),
    EXPORT_COMPLETED(8, "Export completed")
}

data class ProcessingJobState(
    val projectId: String = "",
    val projectTitle: String = "",
    val currentStage: ProcessingStage = ProcessingStage.UPLOADING_VIDEO,
    val stageProgress: Float = 0f,
    val overallProgress: Float = 0f,
    val detailMessage: String = "",
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

data class YouTubeMetadata(
    val url: String,
    val videoId: String,
    val title: String,
    val authorName: String,
    val thumbnailUrl: String,
    val durationLabel: String? = null
)

data class SelectedVideoInfo(
    val uriString: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val mimeType: String
)

data class SelectedHighlight(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val score: Int,
    val reason: String,
    val tags: List<String>
)

data class HardwareProfile(
    val cpuCores: Int,
    val totalRamMb: Long,
    val availableStorageMb: Long,
    val abi: String
)

data class AppSettings(
    val aiModelName: String = "gemini-3.8-flash",
    val defaultLanguage: LanguageOption = LanguageOption.AUTO_DETECT,
    val exportQuality: ExportQuality = ExportQuality.FHD_1080X1920,
    val storageLocationLabel: String = "Downloads/ClipForge & App Workspace",
    val defaultDurationPreset: DurationPreset = DurationPreset.MEDIUM_30_45,
    val defaultClipCount: Int = 5
)
