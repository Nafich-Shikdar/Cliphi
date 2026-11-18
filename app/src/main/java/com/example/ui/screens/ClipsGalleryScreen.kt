package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ClipEntity
import com.example.data.local.ProjectEntity
import com.example.ui.components.StatusPill
import com.example.ui.components.VerticalClipPlayer
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
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClipsGalleryScreen(
    clips: List<ClipEntity>,
    projects: List<ProjectEntity>,
    selectedProjectFilterId: String?,
    previewClip: ClipEntity?,
    onSelectProjectFilter: (String?) -> Unit,
    onPreviewClip: (ClipEntity) -> Unit,
    onClosePreview: () -> Unit,
    onDownloadMp4: (ClipEntity) -> Unit,
    onDownloadSrt: (ClipEntity) -> Unit,
    onDownloadAllZip: (List<ClipEntity>, String) -> Unit,
    onEditClip: (ClipEntity) -> Unit,
    onDeleteClip: (ClipEntity) -> Unit,
    onCreateNewProject: () -> Unit
) {
    var clipPendingDelete by remember { mutableStateOf<ClipEntity?>(null) }

    val activeProjectTitle = projects.firstOrNull { it.id == selectedProjectFilterId }?.title ?: "all_projects"

    if (clipPendingDelete != null) {
        val target = clipPendingDelete!!
        AlertDialog(
            onDismissRequest = { clipPendingDelete = null },
            containerColor = StudioCard,
            title = { Text("Delete Clip?", color = TextPrimary) },
            text = {
                Text("Delete \"${target.title}\" MP4 and SRT files from disk?", color = TextMuted)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClip(target)
                        clipPendingDelete = null
                    }
                ) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { clipPendingDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    if (previewClip != null) {
        Dialog(
            onDismissRequest = onClosePreview,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = StudioCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, StudioOutline),
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = previewClip.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatDurationMs(previewClip.durationMs)} • Orig ${formatDurationMs(previewClip.startMs)}–${formatDurationMs(previewClip.endMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LavenderAccent
                            )
                        }
                        IconButton(onClick = onClosePreview, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close preview", tint = TextPrimary)
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        VerticalClipPlayer(
                            videoPath = previewClip.outputVideoPath,
                            modifier = Modifier.widthIn(max = 260.dp)
                        )
                    }

                    Text(
                        text = previewClip.highlightReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onDownloadMp4(previewClip) },
                            colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("MP4")
                        }
                        OutlinedButton(
                            onClick = { onDownloadSrt(previewClip) },
                            border = BorderStroke(1.dp, StudioOutline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("SRT", color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                onClosePreview()
                                onEditClip(previewClip)
                            },
                            border = BorderStroke(1.dp, StudioOutline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Generated Clips", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(
                        text = stringResource(R.string.ai_score_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                if (clips.isNotEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onDownloadAllZip(clips, activeProjectTitle) },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("download_all_zip_button")
                    ) {
                        Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Download All ZIP")
                    }
                }
            }
        }

        if (projects.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedProjectFilterId == null,
                            onClick = { onSelectProjectFilter(null) },
                            label = { Text("All Projects") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VioletDeepContainer,
                                selectedLabelColor = LavenderAccent
                            )
                        )
                    }
                    items(projects, key = { it.id }) { proj ->
                        FilterChip(
                            selected = selectedProjectFilterId == proj.id,
                            onClick = { onSelectProjectFilter(proj.id) },
                            label = { Text(proj.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VioletDeepContainer,
                                selectedLabelColor = LavenderAccent
                            )
                        )
                    }
                }
            }
        }

        if (clips.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCard),
                    border = BorderStroke(1.dp, StudioOutline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.MovieFilter, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(40.dp))
                        Text("No generated shorts yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = "Start a new project to transcribe audio, detect highlights, and render vertical 9:16 clips.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Button(
                            onClick = onCreateNewProject,
                            colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create New Project")
                        }
                    }
                }
            }
        } else {
            items(clips, key = { it.id }) { clip ->
                ClipGalleryCard(
                    clip = clip,
                    onPreview = { onPreviewClip(clip) },
                    onDownloadMp4 = { onDownloadMp4(clip) },
                    onDownloadSrt = { onDownloadSrt(clip) },
                    onEdit = { onEditClip(clip) },
                    onDelete = { clipPendingDelete = clip }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClipGalleryCard(
    clip: ClipEntity,
    onPreview: () -> Unit,
    onDownloadMp4: () -> Unit,
    onDownloadSrt: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val scoreColor = when {
        clip.highlightScore >= 85 -> SuccessGreen
        clip.highlightScore >= 70 -> LavenderAccent
        else -> WarningAmber
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        border = BorderStroke(1.dp, StudioOutline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clip_card_${clip.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 9:16 Thumbnail Preview Button
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianBg)
                        .clickable(onClick = onPreview),
                    contentAlignment = Alignment.Center
                ) {
                    val thumb = clip.thumbnailPath?.let { File(it) }
                    if (thumb != null && thumb.exists()) {
                        AsyncImage(
                            model = thumb,
                            contentDescription = clip.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Preview clip",
                            tint = TextPrimary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = formatDurationMs(clip.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = clip.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusPill(
                            label = "Score ${clip.highlightScore}/100",
                            color = scoreColor
                        )
                        StatusPill(
                            label = when (clip.subtitleStatus) {
                                "BURNED_IN" -> "Subtitles Burned-In"
                                "SRT_ONLY" -> "SRT Ready"
                                else -> "No Speech"
                            },
                            color = if (clip.subtitleStatus == "NONE") TextMuted else LavenderAccent
                        )
                    }

                    Text(
                        text = "Original: ${formatDurationMs(clip.startMs)} – ${formatDurationMs(clip.endMs)} • ${formatBytes(clip.fileSizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    Text(
                        text = clip.highlightReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onPreview,
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Preview")
                }

                OutlinedButton(
                    onClick = onDownloadMp4,
                    border = BorderStroke(1.dp, StudioOutline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("MP4", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = onDownloadSrt,
                    border = BorderStroke(1.dp, StudioOutline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(Icons.Rounded.ClosedCaption, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SRT", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = onEdit,
                    border = BorderStroke(1.dp, StudioOutline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit Clip", color = TextPrimary)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete clip", tint = ErrorRed)
                }
            }
        }
    }
}
