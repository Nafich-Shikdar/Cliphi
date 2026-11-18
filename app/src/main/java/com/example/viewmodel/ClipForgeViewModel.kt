package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiVideoAiService
import com.example.data.local.ClipEntity
import com.example.data.local.ClipForgeDatabase
import com.example.data.local.ProjectEntity
import com.example.data.local.SettingsDataStore
import com.example.data.media.VideoProcessingEngine
import com.example.data.media.YouTubeMetadataFetcher
import com.example.data.repository.ClipForgeRepository
import com.example.model.AiSelectionMode
import com.example.model.AppSettings
import com.example.model.DurationPreset
import com.example.model.HardwareProfile
import com.example.model.LanguageOption
import com.example.model.NavDestination
import com.example.model.ProcessingJobState
import com.example.model.ProcessingStage
import com.example.model.SelectedVideoInfo
import com.example.model.SubtitleCue
import com.example.model.SubtitleStyleConfig
import com.example.model.SubtitleTemplate
import com.example.model.VerticalCropMode
import com.example.model.YouTubeMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "ClipForgeViewModel"

sealed interface NewProjectUiInput {
    data object Idle : NewProjectUiInput
    data object InspectingVideo : NewProjectUiInput
    data class VideoReady(val info: SelectedVideoInfo) : NewProjectUiInput
    data class Error(val message: String) : NewProjectUiInput
}

sealed interface YouTubeLookupState {
    data object Idle : YouTubeLookupState
    data object Loading : YouTubeLookupState
    data class Loaded(val metadata: YouTubeMetadata) : YouTubeLookupState
    data class Error(val message: String) : YouTubeLookupState
}

class ClipForgeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ClipForgeDatabase.getInstance(application)
    private val settingsDataStore = SettingsDataStore(application)
    private val videoEngine = VideoProcessingEngine(application)
    private val aiService = GeminiVideoAiService()
    private val youtubeFetcher = YouTubeMetadataFetcher()

    private val repository = ClipForgeRepository(
        projectDao = db.projectDao(),
        clipDao = db.clipDao(),
        subtitleCueDao = db.subtitleCueDao(),
        settingsDataStore = settingsDataStore,
        videoEngine = videoEngine,
        aiService = aiService,
        youtubeFetcher = youtubeFetcher
    )

    private val _currentDestination = MutableStateFlow(NavDestination.DASHBOARD)
    val currentDestination: StateFlow<NavDestination> = _currentDestination.asStateFlow()

    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClips: StateFlow<List<ClipEntity>> = repository.allClips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectCount: StateFlow<Int> = repository.projectCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val clipCount: StateFlow<Int> = repository.clipCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val processingCount: StateFlow<Int> = repository.processingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appSettings: StateFlow<AppSettings> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val subtitleStyle: StateFlow<SubtitleStyleConfig> = repository.subtitleStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubtitleStyleConfig())

    private val _storageUsageBytes = MutableStateFlow(0L)
    val storageUsageBytes: StateFlow<Long> = _storageUsageBytes.asStateFlow()

    private val _hardwareProfile = MutableStateFlow(videoEngine.getHardwareProfile())
    val hardwareProfile: StateFlow<HardwareProfile> = _hardwareProfile.asStateFlow()

    // New Project Builder State
    private val _selectedVideoState = MutableStateFlow<NewProjectUiInput>(NewProjectUiInput.Idle)
    val selectedVideoState: StateFlow<NewProjectUiInput> = _selectedVideoState.asStateFlow()

    private val _youtubeState = MutableStateFlow<YouTubeLookupState>(YouTubeLookupState.Idle)
    val youtubeState: StateFlow<YouTubeLookupState> = _youtubeState.asStateFlow()

    private val _processingJobState = MutableStateFlow(ProcessingJobState())
    val processingJobState: StateFlow<ProcessingJobState> = _processingJobState.asStateFlow()

    // Filter & Editor State
    private val _selectedProjectFilterId = MutableStateFlow<String?>(null)
    val selectedProjectFilterId: StateFlow<String?> = _selectedProjectFilterId.asStateFlow()

    val filteredClips: StateFlow<List<ClipEntity>> = combine(
        allClips,
        _selectedProjectFilterId
    ) { clips, filterId ->
        if (filterId == null) clips else clips.filter { it.projectId == filterId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _previewClip = MutableStateFlow<ClipEntity?>(null)
    val previewClip: StateFlow<ClipEntity?> = _previewClip.asStateFlow()

    private val _editingClip = MutableStateFlow<ClipEntity?>(null)
    val editingClip: StateFlow<ClipEntity?> = _editingClip.asStateFlow()

    val editingClipCues: StateFlow<List<SubtitleCue>> = _editingClip
        .flatMapLatest { clip ->
            if (clip == null) flowOf(emptyList())
            else repository.observeCuesForClip(clip.id).map { list ->
                list.map { ClipForgeRepository.entityToCue(it) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reRenderProgress = MutableStateFlow<Float?>(null)
    val reRenderProgress: StateFlow<Float?> = _reRenderProgress.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private var activeProcessingJob: Job? = null

    // Last launch parameters for retry
    private var lastRunParams: RunParams? = null

    private data class RunParams(
        val title: String,
        val uri: Uri,
        val videoInfo: SelectedVideoInfo,
        val sourceType: String,
        val youtubeUrl: String?,
        val minDurationSec: Int,
        val maxDurationSec: Int,
        val requestedClipCount: Int,
        val languageOption: LanguageOption,
        val selectionMode: AiSelectionMode,
        val customPrompt: String,
        val cropMode: VerticalCropMode,
        val subtitleStyle: SubtitleStyleConfig
    )

    init {
        refreshStorageAndHardware()
    }

    fun navigateTo(destination: NavDestination) {
        _currentDestination.value = destination
        if (destination == NavDestination.DASHBOARD || destination == NavDestination.SETTINGS) {
            refreshStorageAndHardware()
        }
    }

    fun openProjectClips(projectId: String) {
        _selectedProjectFilterId.value = projectId
        _currentDestination.value = NavDestination.CLIPS
    }

    fun clearProjectFilter() {
        _selectedProjectFilterId.value = null
    }

    fun refreshStorageAndHardware() {
        viewModelScope.launch {
            _storageUsageBytes.value = repository.getStorageUsageBytes()
            _hardwareProfile.value = repository.getHardwareProfile()
        }
    }

    fun onVideoUriPicked(uri: Uri) {
        viewModelScope.launch {
            _selectedVideoState.value = NewProjectUiInput.InspectingVideo
            try {
                val info = repository.inspectLocalVideo(uri)
                if (info.durationMs <= 0L) {
                    _selectedVideoState.value = NewProjectUiInput.Error("Could not read video duration. Ensure this is a valid MP4, MOV, MKV, WEBM, or AVI file.")
                } else {
                    _selectedVideoState.value = NewProjectUiInput.VideoReady(info)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inspect picked video", e)
                _selectedVideoState.value = NewProjectUiInput.Error(
                    e.localizedMessage ?: "Failed to read selected video metadata."
                )
            }
        }
    }

    fun clearSelectedVideo() {
        _selectedVideoState.value = NewProjectUiInput.Idle
    }

    fun validateYouTubeUrl(url: String) {
        viewModelScope.launch {
            _youtubeState.value = YouTubeLookupState.Loading
            val res = repository.fetchYouTubeMetadata(url)
            res.onSuccess { meta ->
                _youtubeState.value = YouTubeLookupState.Loaded(meta)
            }.onFailure { err ->
                _youtubeState.value = YouTubeLookupState.Error(
                    err.localizedMessage ?: "Unable to fetch YouTube metadata."
                )
            }
        }
    }

    fun clearYouTubeLookup() {
        _youtubeState.value = YouTubeLookupState.Idle
    }

    fun startClipGeneration(
        projectTitle: String,
        durationPreset: DurationPreset,
        customMinSec: Int,
        customMaxSec: Int,
        clipCount: Int,
        languageOption: LanguageOption,
        selectionMode: AiSelectionMode,
        customPrompt: String,
        cropMode: VerticalCropMode,
        burnInSubtitles: Boolean,
        selectedTemplate: SubtitleTemplate
    ) {
        val ready = _selectedVideoState.value as? NewProjectUiInput.VideoReady ?: return
        val ytMeta = (_youtubeState.value as? YouTubeLookupState.Loaded)?.metadata

        val minSec = if (durationPreset == DurationPreset.CUSTOM) customMinSec.coerceIn(10, 120) else durationPreset.minSeconds
        val maxSec = if (durationPreset == DurationPreset.CUSTOM) customMaxSec.coerceAtLeast(minSec + 5).coerceIn(15, 180) else durationPreset.maxSeconds
        val resolvedTitle = projectTitle.trim().ifBlank {
            ytMeta?.title ?: ready.info.fileName.substringBeforeLast('.')
        }

        viewModelScope.launch {
            val baseStyle = subtitleStyle.first()
            val appliedStyle = if (baseStyle.template != selectedTemplate) {
                SubtitleStyleConfig(
                    template = selectedTemplate,
                    fontFamily = selectedTemplate.defaultFont,
                    fontSizeSp = selectedTemplate.defaultFontSizeSp,
                    textColorHex = selectedTemplate.defaultTextHex,
                    backgroundColorHex = selectedTemplate.defaultBgHex,
                    outlineColorHex = selectedTemplate.defaultOutlineHex,
                    highlightColorHex = selectedTemplate.defaultHighlightHex,
                    position = selectedTemplate.defaultPosition,
                    burnInSubtitles = burnInSubtitles
                )
            } else {
                baseStyle.copy(burnInSubtitles = burnInSubtitles)
            }

            val params = RunParams(
                title = resolvedTitle,
                uri = Uri.parse(ready.info.uriString),
                videoInfo = ready.info,
                sourceType = if (ytMeta != null) "YOUTUBE_ATTACHED" else "LOCAL",
                youtubeUrl = ytMeta?.url,
                minDurationSec = minSec,
                maxDurationSec = maxSec,
                requestedClipCount = clipCount,
                languageOption = languageOption,
                selectionMode = selectionMode,
                customPrompt = customPrompt,
                cropMode = cropMode,
                subtitleStyle = appliedStyle
            )
            lastRunParams = params
            executeProcessingJob(params)
        }
    }

    fun retryLastProcessingJob() {
        val params = lastRunParams ?: return
        executeProcessingJob(params)
    }

    private fun executeProcessingJob(params: RunParams) {
        activeProcessingJob?.cancel()
        activeProcessingJob = viewModelScope.launch {
            _processingJobState.value = ProcessingJobState(
                projectTitle = params.title,
                currentStage = ProcessingStage.UPLOADING_VIDEO,
                stageProgress = 0f,
                overallProgress = 0f,
                detailMessage = "Initializing studio pipeline...",
                isRunning = true,
                isCompleted = false,
                errorMessage = null
            )

            try {
                val project = repository.processNewProject(
                    title = params.title,
                    sourceUri = params.uri,
                    videoInfo = params.videoInfo,
                    sourceType = params.sourceType,
                    youtubeUrl = params.youtubeUrl,
                    minDurationSec = params.minDurationSec,
                    maxDurationSec = params.maxDurationSec,
                    requestedClipCount = params.requestedClipCount,
                    languageOption = params.languageOption,
                    selectionMode = params.selectionMode,
                    customPrompt = params.customPrompt,
                    cropMode = params.cropMode,
                    subtitleStyle = params.subtitleStyle
                ) { state ->
                    _processingJobState.value = state
                }
                _selectedProjectFilterId.value = project.id
                refreshStorageAndHardware()
                _snackbarMessages.emit("ClipForge finished generating shorts for \"${project.title}\".")
            } catch (_: CancellationException) {
                _processingJobState.value = _processingJobState.value.copy(
                    isRunning = false,
                    isCompleted = false,
                    errorMessage = "Processing was cancelled."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Clip generation failed", e)
                _processingJobState.value = _processingJobState.value.copy(
                    isRunning = false,
                    isCompleted = false,
                    errorMessage = e.localizedMessage ?: "An unexpected error occurred during processing."
                )
            }
        }
    }

    fun cancelActiveProcessing() {
        activeProcessingJob?.cancel()
        activeProcessingJob = null
    }

    fun resetProcessingState() {
        _processingJobState.value = ProcessingJobState()
    }

    fun openClipPreview(clip: ClipEntity) {
        _previewClip.value = clip
    }

    fun closeClipPreview() {
        _previewClip.value = null
    }

    fun openClipInSubtitleStudio(clip: ClipEntity) {
        _editingClip.value = clip
        _currentDestination.value = NavDestination.SUBTITLE_STUDIO
    }

    fun selectClipForSubtitleStudio(clip: ClipEntity) {
        _editingClip.value = clip
    }

    fun applyTemplatePreset(template: SubtitleTemplate) {
        viewModelScope.launch {
            val current = subtitleStyle.value
            repository.updateSubtitleStyle(
                current.copy(
                    template = template,
                    fontFamily = template.defaultFont,
                    fontSizeSp = template.defaultFontSizeSp,
                    textColorHex = template.defaultTextHex,
                    backgroundColorHex = template.defaultBgHex,
                    outlineColorHex = template.defaultOutlineHex,
                    highlightColorHex = template.defaultHighlightHex,
                    position = template.defaultPosition
                )
            )
        }
    }

    fun updateSubtitleStyle(style: SubtitleStyleConfig) {
        viewModelScope.launch {
            repository.updateSubtitleStyle(style)
        }
    }

    fun saveCueChanges(clip: ClipEntity, updatedCues: List<SubtitleCue>) {
        viewModelScope.launch {
            try {
                repository.saveEditedCuesAndSrt(clip, updatedCues)
                _snackbarMessages.emit("Subtitles & SRT updated.")
            } catch (e: Exception) {
                _snackbarMessages.emit(e.localizedMessage ?: "Failed to save subtitle cues.")
            }
        }
    }

    fun reRenderEditingClip(
        clip: ClipEntity,
        newStartMs: Long,
        newEndMs: Long,
        cropMode: VerticalCropMode,
        updatedCues: List<SubtitleCue>
    ) {
        viewModelScope.launch {
            _reRenderProgress.value = 0f
            try {
                val style = subtitleStyle.value
                val quality = appSettings.value.exportQuality
                val updated = repository.reRenderClip(
                    clip = clip,
                    newStartMs = newStartMs,
                    newEndMs = newEndMs,
                    cropMode = cropMode,
                    updatedCues = updatedCues,
                    subtitleStyle = style,
                    exportQuality = quality
                ) { p ->
                    _reRenderProgress.value = p
                }
                _editingClip.value = updated
                refreshStorageAndHardware()
                _snackbarMessages.emit("Re-rendered ${updated.title} with updated subtitles.")
            } catch (e: Exception) {
                Log.e(TAG, "Re-render failed", e)
                _snackbarMessages.emit(e.localizedMessage ?: "Failed to re-render clip.")
            } finally {
                _reRenderProgress.value = null
            }
        }
    }

    fun downloadClipMp4(clip: ClipEntity) {
        viewModelScope.launch {
            val uri = repository.downloadClipMp4(clip)
            if (uri != null) {
                _snackbarMessages.emit("Saved MP4 to Downloads/ClipForge.")
            } else {
                _snackbarMessages.emit("Could not save MP4 file.")
            }
        }
    }

    fun downloadClipSrt(clip: ClipEntity) {
        viewModelScope.launch {
            val uri = repository.downloadClipSrt(clip)
            if (uri != null) {
                _snackbarMessages.emit("Saved SRT captions to Downloads/ClipForge.")
            } else {
                _snackbarMessages.emit("Could not save SRT file.")
            }
        }
    }

    fun downloadAllAsZip(clipsToExport: List<ClipEntity>, label: String) {
        if (clipsToExport.isEmpty()) return
        viewModelScope.launch {
            try {
                val (zipFile, uri) = repository.downloadClipsZip(label, clipsToExport)
                if (uri != null) {
                    _snackbarMessages.emit("Saved ${zipFile.name} (${clipsToExport.size} clips + SRTs) to Downloads/ClipForge.")
                } else {
                    _snackbarMessages.emit("ZIP created in cache: ${zipFile.name}")
                }
            } catch (e: Exception) {
                _snackbarMessages.emit(e.localizedMessage ?: "Failed to create ZIP archive.")
            }
        }
    }

    fun deleteClip(clip: ClipEntity) {
        viewModelScope.launch {
            repository.deleteClip(clip)
            if (_editingClip.value?.id == clip.id) _editingClip.value = null
            if (_previewClip.value?.id == clip.id) _previewClip.value = null
            refreshStorageAndHardware()
            _snackbarMessages.emit("Deleted clip \"${clip.title}\".")
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_selectedProjectFilterId.value == project.id) {
                _selectedProjectFilterId.value = null
            }
            refreshStorageAndHardware()
            _snackbarMessages.emit("Deleted project \"${project.title}\".")
        }
    }

    fun updateAppSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.updateAppSettings(settings)
            _snackbarMessages.emit("Settings saved.")
        }
    }

    fun clearExportCache() {
        viewModelScope.launch {
            val freed = repository.clearExportCache()
            refreshStorageAndHardware()
            _snackbarMessages.emit("Cleared ${freed / 1024} KB of export cache.")
        }
    }
}
