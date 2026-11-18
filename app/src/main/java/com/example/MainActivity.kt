package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.NavDestination
import com.example.ui.components.ClipForgeBottomNav
import com.example.ui.components.ClipForgeSidebar
import com.example.ui.screens.ClipsGalleryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NewProjectScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubtitleStudioScreen
import com.example.ui.theme.ClipForgeTheme
import com.example.ui.theme.ObsidianBg
import com.example.viewmodel.ClipForgeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before rendering UI or creating ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            ClipForgeTheme {
                Surface(
                    color = ObsidianBg,
                    modifier = Modifier.fillMaxSize()
                ) {
                    ClipForgeApp()
                }
            }
        }
    }
}

@Composable
fun ClipForgeApp(
    viewModel: ClipForgeViewModel = viewModel()
) {
    val destination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val allClips by viewModel.allClips.collectAsStateWithLifecycle()
    val filteredClips by viewModel.filteredClips.collectAsStateWithLifecycle()
    val projectCount by viewModel.projectCount.collectAsStateWithLifecycle()
    val clipCount by viewModel.clipCount.collectAsStateWithLifecycle()
    val processingCount by viewModel.processingCount.collectAsStateWithLifecycle()
    val storageUsageBytes by viewModel.storageUsageBytes.collectAsStateWithLifecycle()
    val hardwareProfile by viewModel.hardwareProfile.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val subtitleStyle by viewModel.subtitleStyle.collectAsStateWithLifecycle()
    val selectedVideoState by viewModel.selectedVideoState.collectAsStateWithLifecycle()
    val youtubeState by viewModel.youtubeState.collectAsStateWithLifecycle()
    val processingState by viewModel.processingJobState.collectAsStateWithLifecycle()
    val selectedProjectFilterId by viewModel.selectedProjectFilterId.collectAsStateWithLifecycle()
    val previewClip by viewModel.previewClip.collectAsStateWithLifecycle()
    val editingClip by viewModel.editingClip.collectAsStateWithLifecycle()
    val editingCues by viewModel.editingClipCues.collectAsStateWithLifecycle()
    val reRenderProgress by viewModel.reRenderProgress.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = previewClip != null || destination != NavDestination.DASHBOARD) {
        if (previewClip != null) {
            viewModel.closeClipPreview()
        } else {
            viewModel.navigateTo(NavDestination.DASHBOARD)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        Scaffold(
            containerColor = ObsidianBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isWideScreen) {
                    ClipForgeBottomNav(
                        currentDestination = destination,
                        onSelectDestination = viewModel::navigateTo
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(ObsidianBg)
            ) {
                if (isWideScreen) {
                    ClipForgeSidebar(
                        currentDestination = destination,
                        onSelectDestination = viewModel::navigateTo,
                        isProcessing = processingState.isRunning,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .windowInsetsPadding(
                            if (isWideScreen) WindowInsets.safeDrawing
                            else WindowInsets(0, 0, 0, 0)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (!isWideScreen) Modifier.windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Top)
                                ) else Modifier
                            )
                    ) {
                        when (destination) {
                            NavDestination.DASHBOARD -> DashboardScreen(
                                projects = projects,
                                clips = allClips,
                                totalProjects = projectCount,
                                totalClips = clipCount,
                                processingJobsCount = processingCount,
                                storageUsageBytes = storageUsageBytes,
                                activeJob = processingState,
                                isWideScreen = isWideScreen,
                                onCreateNewProject = { viewModel.navigateTo(NavDestination.NEW_PROJECT) },
                                onOpenProjectClips = { projectId -> viewModel.openProjectClips(projectId) },
                                onOpenAllProjects = { viewModel.navigateTo(NavDestination.PROJECTS) },
                                onOpenProcessingView = { viewModel.navigateTo(NavDestination.NEW_PROJECT) }
                            )

                            NavDestination.NEW_PROJECT -> NewProjectScreen(
                                selectedVideoState = selectedVideoState,
                                youtubeState = youtubeState,
                                processingState = processingState,
                                defaultSettings = appSettings,
                                onVideoUriPicked = viewModel::onVideoUriPicked,
                                onClearVideo = viewModel::clearSelectedVideo,
                                onValidateYouTubeUrl = viewModel::validateYouTubeUrl,
                                onStartGeneration = viewModel::startClipGeneration,
                                onCancelProcessing = viewModel::cancelActiveProcessing,
                                onRetryProcessing = viewModel::retryLastProcessingJob,
                                onResetProcessing = viewModel::resetProcessingState,
                                onViewGeneratedClips = { viewModel.navigateTo(NavDestination.CLIPS) }
                            )

                            NavDestination.PROJECTS -> ProjectsScreen(
                                projects = projects,
                                clips = allClips,
                                onCreateNewProject = { viewModel.navigateTo(NavDestination.NEW_PROJECT) },
                                onOpenProjectClips = { projectId -> viewModel.openProjectClips(projectId) },
                                onDeleteProject = viewModel::deleteProject
                            )

                            NavDestination.CLIPS -> ClipsGalleryScreen(
                                clips = filteredClips,
                                projects = projects,
                                selectedProjectFilterId = selectedProjectFilterId,
                                previewClip = previewClip,
                                onSelectProjectFilter = { id ->
                                    if (id == null) viewModel.clearProjectFilter()
                                    else viewModel.openProjectClips(id)
                                },
                                onPreviewClip = viewModel::openClipPreview,
                                onClosePreview = viewModel::closeClipPreview,
                                onDownloadMp4 = viewModel::downloadClipMp4,
                                onDownloadSrt = viewModel::downloadClipSrt,
                                onDownloadAllZip = viewModel::downloadAllAsZip,
                                onEditClip = viewModel::openClipInSubtitleStudio,
                                onDeleteClip = viewModel::deleteClip,
                                onCreateNewProject = { viewModel.navigateTo(NavDestination.NEW_PROJECT) }
                            )

                            NavDestination.SUBTITLE_STUDIO -> SubtitleStudioScreen(
                                allClips = allClips,
                                editingClip = editingClip,
                                cues = editingCues,
                                subtitleStyle = subtitleStyle,
                                reRenderProgress = reRenderProgress,
                                onSelectClip = viewModel::selectClipForSubtitleStudio,
                                onApplyTemplate = viewModel::applyTemplatePreset,
                                onUpdateStyle = viewModel::updateSubtitleStyle,
                                onSaveCues = viewModel::saveCueChanges,
                                onReRenderClip = viewModel::reRenderEditingClip,
                                onDownloadSrt = viewModel::downloadClipSrt
                            )

                            NavDestination.SETTINGS -> SettingsScreen(
                                settings = appSettings,
                                hardwareProfile = hardwareProfile,
                                storageUsageBytes = storageUsageBytes,
                                onUpdateSettings = viewModel::updateAppSettings,
                                onClearExportCache = viewModel::clearExportCache
                            )
                        }
                    }
                }
            }
        }
    }
}
