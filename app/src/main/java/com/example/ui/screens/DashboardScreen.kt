package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ClipEntity
import com.example.data.local.ProjectEntity
import com.example.model.ProcessingJobState
import com.example.ui.components.StatCard
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
import com.example.ui.theme.WarningAmber
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    projects: List<ProjectEntity>,
    clips: List<ClipEntity>,
    totalProjects: Int,
    totalClips: Int,
    processingJobsCount: Int,
    storageUsageBytes: Long,
    activeJob: ProcessingJobState,
    isWideScreen: Boolean,
    onCreateNewProject: () -> Unit,
    onOpenProjectClips: (String) -> Unit,
    onOpenAllProjects: () -> Unit,
    onOpenProcessingView: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Banner Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, StudioOutline),
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.studio_hero_banner_1795010956526),
                        contentDescription = "ClipForge Studio Banner",
                        contentScale = ContentScale.Crop,
                        alpha = 0.42f,
                        modifier = Modifier
                            .matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ObsidianBg.copy(alpha = 0.92f),
                                        ObsidianBg.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusPill(
                            label = "PERSONAL AI CLIPPING STUDIO",
                            color = LavenderAccent
                        )
                        Text(
                            text = "One Long Video. Multiple Viral Shorts.",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Turn long-form podcasts, tutorials, and interviews into vertical 9:16 H.264 shorts with synchronized Bengali & English captions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("YouTube Shorts", "Instagram Reels", "Facebook Reels", "TikTok").forEach { platform ->
                                Surface(
                                    color = StudioCardElevated.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, StudioOutline)
                                ) {
                                    Text(
                                        text = platform,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = onCreateNewProject,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VioletAccent,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("dashboard_create_project_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create New Project",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // Active Processing Banner
        if (activeJob.isRunning) {
            item {
                Card(
                    onClick = onOpenProcessingView,
                    colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
                    border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = LavenderAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Processing: ${activeJob.projectTitle}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "${(activeJob.overallProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = LavenderAccent
                            )
                        }
                        LinearProgressIndicator(
                            progress = { activeJob.overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = VioletAccent,
                            trackColor = StudioCard
                        )
                        Text(
                            text = "${activeJob.currentStage.stepNumber}/8 • ${activeJob.detailMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // 4 Studio Metrics
        item {
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Projects",
                        value = totalProjects.toString(),
                        subtitle = "Workspace videos",
                        icon = Icons.Rounded.FolderCopy,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Generated Clips",
                        value = totalClips.toString(),
                        subtitle = "9:16 H.264 shorts",
                        icon = Icons.Rounded.MovieFilter,
                        accentColor = LavenderAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Processing Jobs",
                        value = (if (activeJob.isRunning) maxOf(1, processingJobsCount) else processingJobsCount).toString(),
                        subtitle = if (activeJob.isRunning) "Pipeline active" else "Queue idle",
                        icon = Icons.Rounded.Schedule,
                        accentColor = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Storage Usage",
                        value = formatBytes(storageUsageBytes),
                        subtitle = "Local studio disk",
                        icon = Icons.Rounded.Storage,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Projects",
                            value = totalProjects.toString(),
                            subtitle = "Workspace videos",
                            icon = Icons.Rounded.FolderCopy,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Generated Clips",
                            value = totalClips.toString(),
                            subtitle = "9:16 H.264 shorts",
                            icon = Icons.Rounded.MovieFilter,
                            accentColor = LavenderAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Processing Jobs",
                            value = (if (activeJob.isRunning) maxOf(1, processingJobsCount) else processingJobsCount).toString(),
                            subtitle = if (activeJob.isRunning) "Pipeline active" else "Queue idle",
                            icon = Icons.Rounded.Schedule,
                            accentColor = WarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Storage Usage",
                            value = formatBytes(storageUsageBytes),
                            subtitle = "Local studio disk",
                            icon = Icons.Rounded.Storage,
                            accentColor = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Recent Projects Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Projects",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                if (projects.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onOpenAllProjects,
                        border = BorderStroke(1.dp, StudioOutline),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("View All", color = LavenderAccent)
                    }
                }
            }
        }

        if (projects.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            tint = LavenderAccent,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No projects yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Upload an MP4/MOV/MKV/WEBM/AVI video or validate a YouTube URL to extract speech, detect highlights, and render 9:16 vertical shorts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(projects.take(5), key = { it.id }) { project ->
                val projectClipsCount = clips.count { it.projectId == project.id }
                RecentProjectRow(
                    project = project,
                    clipsCount = projectClipsCount,
                    onClick = { onOpenProjectClips(project.id) }
                )
            }
        }
    }
}

@Composable
private fun RecentProjectRow(
    project: ProjectEntity,
    clipsCount: Int,
    onClick: () -> Unit
) {
    val statusColor = when (project.status) {
        "COMPLETED" -> SuccessGreen
        "PROCESSING" -> WarningAmber
        "FAILED" -> ErrorRed
        else -> TextMuted
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        border = BorderStroke(1.dp, StudioOutline),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("recent_project_${project.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 88.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCardElevated),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = project.thumbnailPath?.let { File(it) }
                if (thumbFile != null && thumbFile.exists()) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = project.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MovieFilter,
                        contentDescription = null,
                        tint = TextMuted
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDurationMs(project.durationMs)} • ${formatBytes(project.fileSizeBytes)} • $clipsCount clips",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            StatusPill(label = project.status, color = statusColor)
        }
    }
}
