package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AiSelectionMode
import com.example.model.AppSettings
import com.example.model.DurationPreset
import com.example.model.LanguageOption
import com.example.model.ProcessingJobState
import com.example.model.SubtitleTemplate
import com.example.model.VerticalCropMode
import com.example.ui.components.ProcessingStagesCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBytes
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletDeepContainer
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.NewProjectUiInput
import com.example.viewmodel.YouTubeLookupState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewProjectScreen(
    selectedVideoState: NewProjectUiInput,
    youtubeState: YouTubeLookupState,
    processingState: ProcessingJobState,
    defaultSettings: AppSettings,
    onVideoUriPicked: (android.net.Uri) -> Unit,
    onClearVideo: () -> Unit,
    onValidateYouTubeUrl: (String) -> Unit,
    onStartGeneration: (
        title: String,
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
    ) -> Unit,
    onCancelProcessing: () -> Unit,
    onRetryProcessing: () -> Unit,
    onResetProcessing: () -> Unit,
    onViewGeneratedClips: () -> Unit
) {
    var inputTab by remember { mutableIntStateOf(0) } // 0 = Upload Video, 1 = YouTube Import
    var youtubeUrl by remember { mutableStateOf("") }
    var projectTitle by remember { mutableStateOf("") }

    var durationPreset by remember(defaultSettings) { mutableStateOf(defaultSettings.defaultDurationPreset) }
    var customMinSec by remember { mutableIntStateOf(20) }
    var customMaxSec by remember { mutableIntStateOf(55) }
    var clipCount by remember(defaultSettings) { mutableIntStateOf(defaultSettings.defaultClipCount) }
    var languageOption by remember(defaultSettings) { mutableStateOf(defaultSettings.defaultLanguage) }
    var selectionMode by remember { mutableStateOf(AiSelectionMode.MOST_ENGAGING) }
    var customPrompt by remember { mutableStateOf("") }
    var cropMode by remember { mutableStateOf(VerticalCropMode.SMART_FACE_TRACK) }
    var burnInSubtitles by remember { mutableStateOf(true) }
    var selectedTemplate by remember { mutableStateOf(SubtitleTemplate.PREMIUM_PURPLE) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onVideoUriPicked(uri)
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onVideoUriPicked(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "New AI Clipping Project",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Import a long video, configure Gemini AI highlight selection, and render 9:16 shorts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Active / Completed / Failed Processing Experience
        if (processingState.isRunning || processingState.isCompleted || processingState.errorMessage != null) {
            item {
                ProcessingStagesCard(jobState = processingState)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (processingState.isRunning) {
                        OutlinedButton(
                            onClick = onCancelProcessing,
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("cancel_processing_button")
                        ) {
                            Icon(Icons.Rounded.Cancel, contentDescription = null, tint = ErrorRed)
                            Spacer(Modifier.width(8.dp))
                            Text("Cancel Job", color = ErrorRed)
                        }
                    }
                    if (processingState.errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = processingState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onRetryProcessing,
                                        colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                                        modifier = Modifier.testTag("retry_processing_button")
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Retry")
                                    }
                                    OutlinedButton(onClick = onResetProcessing) {
                                        Text("Dismiss", color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                    if (processingState.isCompleted) {
                        Button(
                            onClick = onViewGeneratedClips,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = ObsidianBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("view_generated_clips_button")
                        ) {
                            Icon(Icons.Rounded.MovieFilter, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("View Generated Shorts", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                onResetProcessing()
                                onClearVideo()
                            },
                            border = BorderStroke(1.dp, StudioOutline),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("New Batch", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Step 1: Video Input (Upload vs YouTube Import)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "1. Video Source",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    TabRow(
                        selectedTabIndex = inputTab,
                        containerColor = StudioCardElevated,
                        contentColor = TextPrimary,
                        indicator = { tabPositions ->
                            SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[inputTab]),
                                color = VioletAccent
                            )
                        },
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = inputTab == 0,
                            onClick = { inputTab = 0 },
                            text = { Text("Upload Video") },
                            icon = { Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_upload_video")
                        )
                        Tab(
                            selected = inputTab == 1,
                            onClick = { inputTab = 1 },
                            text = { Text("YouTube Import") },
                            icon = { Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("tab_youtube_import")
                        )
                    }

                    if (inputTab == 0) {
                        Text(
                            text = "Supported formats: MP4, MOV, MKV, WEBM, AVI",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_gallery_video_button")
                            ) {
                                Icon(Icons.Rounded.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Video Gallery")
                            }
                            OutlinedButton(
                                onClick = {
                                    documentPickerLauncher.launch(
                                        arrayOf("video/mp4", "video/quicktime", "video/x-matroska", "video/webm", "video/x-msvideo", "video/*")
                                    )
                                },
                                border = BorderStroke(1.dp, StudioOutline),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("browse_video_files_button")
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Browse Files", color = TextPrimary)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = youtubeUrl,
                                onValueChange = { youtubeUrl = it },
                                label = { Text("YouTube Video URL") },
                                placeholder = { Text("https://www.youtube.com/watch?v=...") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VioletAccent,
                                    unfocusedBorderColor = StudioOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("youtube_url_input")
                            )
                            Button(
                                onClick = { onValidateYouTubeUrl(youtubeUrl) },
                                enabled = youtubeUrl.isNotBlank() && youtubeState !is YouTubeLookupState.Loading,
                                colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("validate_youtube_button")
                            ) {
                                Text("Fetch")
                            }
                        }

                        when (youtubeState) {
                            is YouTubeLookupState.Loading -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LavenderAccent)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Validating URL & fetching metadata...", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                            is YouTubeLookupState.Error -> {
                                Text(youtubeState.message, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                            }
                            is YouTubeLookupState.Loaded -> {
                                val meta = youtubeState.metadata
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
                                    border = BorderStroke(1.dp, StudioOutline),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = meta.thumbnailUrl,
                                            contentDescription = meta.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 96.dp, height = 56.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(meta.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                text = "${meta.authorName}${meta.durationLabel?.let { " • $it" } ?: ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LavenderAccent
                                            )
                                        }
                                    }
                                }
                            }
                            YouTubeLookupState.Idle -> {}
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.10f)),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.youtube_restriction_notice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        documentPickerLauncher.launch(
                                            arrayOf("video/mp4", "video/quicktime", "video/x-matroska", "video/webm", "video/*")
                                        )
                                    },
                                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Attach Authorized Local Video File", color = WarningAmber)
                                }
                            }
                        }
                    }

                    // Selected Video Status Card
                    when (selectedVideoState) {
                        NewProjectUiInput.Idle -> {}
                        NewProjectUiInput.InspectingVideo -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = VioletAccent)
                                Spacer(Modifier.width(10.dp))
                                Text("Inspecting video codec, resolution & duration...", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                        is NewProjectUiInput.Error -> {
                            Text(selectedVideoState.message, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                        }
                        is NewProjectUiInput.VideoReady -> {
                            val info = selectedVideoState.info
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(info.fileName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = "${formatDurationMs(info.durationMs)} • ${formatBytes(info.fileSizeBytes)} • ${info.width}×${info.height}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LavenderAccent
                                        )
                                    }
                                    StatusPill(label = "Ready", color = SuccessGreen)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step 2: AI Clip Generator Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "2. AI Clip Generator & Vertical Framing",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        label = { Text("Project Name (optional)") },
                        placeholder = { Text("Auto-filled from video or YouTube title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioletAccent,
                            unfocusedBorderColor = StudioOutline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_title_input")
                    )

                    // Duration Presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Clip Duration", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DurationPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = durationPreset == preset,
                                    onClick = { durationPreset = preset },
                                    label = { Text(preset.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VioletDeepContainer,
                                        selectedLabelColor = LavenderAccent
                                    )
                                )
                            }
                        }
                        if (durationPreset == DurationPreset.CUSTOM) {
                            Text(
                                text = "Custom range: ${customMinSec}s – ${customMaxSec}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = LavenderAccent
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Min: ${customMinSec}s", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Slider(
                                        value = customMinSec.toFloat(),
                                        onValueChange = {
                                            customMinSec = it.roundToInt()
                                            if (customMaxSec < customMinSec + 5) customMaxSec = customMinSec + 5
                                        },
                                        valueRange = 10f..90f,
                                        colors = SliderDefaults.colors(thumbColor = VioletAccent, activeTrackColor = VioletAccent)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Max: ${customMaxSec}s", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Slider(
                                        value = customMaxSec.toFloat(),
                                        onValueChange = {
                                            customMaxSec = it.roundToInt().coerceAtLeast(customMinSec + 5)
                                        },
                                        valueRange = 15f..120f,
                                        colors = SliderDefaults.colors(thumbColor = VioletAccent, activeTrackColor = VioletAccent)
                                    )
                                }
                            }
                        }
                    }

                    // Number of Clips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Number of Clips", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 5, 10, 15, 20).forEach { count ->
                                FilterChip(
                                    selected = clipCount == count,
                                    onClick = { clipCount = count },
                                    label = { Text("$count clips") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VioletDeepContainer,
                                        selectedLabelColor = LavenderAccent
                                    )
                                )
                            }
                        }
                    }

                    // Speech & Subtitle Language
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Language (Transcription & Subtitles)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LanguageOption.entries.forEach { lang ->
                                FilterChip(
                                    selected = languageOption == lang,
                                    onClick = { languageOption = lang },
                                    label = { Text(lang.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VioletDeepContainer,
                                        selectedLabelColor = LavenderAccent
                                    )
                                )
                            }
                        }
                    }

                    // AI Selection Mode
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI Highlight Selection", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AiSelectionMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = selectionMode == mode,
                                    onClick = { selectionMode = mode },
                                    label = { Text(mode.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VioletDeepContainer,
                                        selectedLabelColor = LavenderAccent
                                    )
                                )
                            }
                        }
                        if (selectionMode == AiSelectionMode.CUSTOM) {
                            OutlinedTextField(
                                value = customPrompt,
                                onValueChange = { customPrompt = it },
                                label = { Text("Custom AI Selection Instructions") },
                                placeholder = { Text("e.g., Extract only technical coding demos or Bengali Q&A moments") },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VioletAccent,
                                    unfocusedBorderColor = StudioOutline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Vertical 9:16 Framing
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("9:16 Vertical Framing", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VerticalCropMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = cropMode == mode,
                                    onClick = { cropMode = mode },
                                    label = { Text(mode.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VioletDeepContainer,
                                        selectedLabelColor = LavenderAccent
                                    )
                                )
                            }
                        }
                        Text(cropMode.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }

                    // Burned-in Subtitles & Template
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Burn-in Synchronized Subtitles", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Render crisp Bengali & English captions onto the MP4", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Switch(
                            checked = burnInSubtitles,
                            onCheckedChange = { burnInSubtitles = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = VioletAccent)
                        )
                    }

                    if (burnInSubtitles) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Caption Template", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubtitleTemplate.entries.forEach { tpl ->
                                    FilterChip(
                                        selected = selectedTemplate == tpl,
                                        onClick = { selectedTemplate = tpl },
                                        label = { Text(tpl.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VioletDeepContainer,
                                            selectedLabelColor = LavenderAccent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onStartGeneration(
                                projectTitle,
                                durationPreset,
                                customMinSec,
                                customMaxSec,
                                clipCount,
                                languageOption,
                                selectionMode,
                                customPrompt,
                                cropMode,
                                burnInSubtitles,
                                selectedTemplate
                            )
                        },
                        enabled = selectedVideoState is NewProjectUiInput.VideoReady && !processingState.isRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletAccent,
                            contentColor = TextPrimary,
                            disabledContainerColor = StudioCardElevated,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_ai_clipping_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (processingState.isRunning) "Processing in Progress..." else "Generate 9:16 AI Shorts",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
