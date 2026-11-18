package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ClipEntity
import com.example.data.local.ProjectEntity
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBytes
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.WarningAmber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>,
    clips: List<ClipEntity>,
    onCreateNewProject: () -> Unit,
    onOpenProjectClips: (String) -> Unit,
    onDeleteProject: (ProjectEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var projectPendingDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    val filtered = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects
        else projects.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    if (projectPendingDelete != null) {
        val target = projectPendingDelete!!
        AlertDialog(
            onDismissRequest = { projectPendingDelete = null },
            containerColor = StudioCard,
            title = { Text("Delete Project?", color = TextPrimary) },
            text = {
                Text(
                    "This removes \"${target.title}\" and all generated 9:16 MP4 and SRT files from your studio workspace.",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(target)
                        projectPendingDelete = null
                    }
                ) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectPendingDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
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
                Column {
                    Text("My Projects", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("${projects.size} studio projects saved locally", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Button(
                    onClick = onCreateNewProject,
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search projects by title...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VioletAccent,
                    unfocusedBorderColor = StudioOutline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filtered.isEmpty()) {
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
                        Icon(Icons.Rounded.FolderCopy, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(36.dp))
                        Text("No matching projects", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Create a new video clipping project to get started.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { project ->
                val projectClips = clips.filter { it.projectId == project.id }
                val totalClipBytes = projectClips.sumOf { it.fileSizeBytes }
                val dateText = remember(project.createdAt) {
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(project.createdAt))
                }
                val statusColor = when (project.status) {
                    "COMPLETED" -> SuccessGreen
                    "PROCESSING" -> WarningAmber
                    "FAILED" -> ErrorRed
                    else -> TextMuted
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCard),
                    border = BorderStroke(1.dp, StudioOutline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProjectClips(project.id) }
                        .testTag("project_card_${project.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 96.dp, height = 60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioCardElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                val thumb = project.thumbnailPath?.let { File(it) }
                                if (thumb != null && thumb.exists()) {
                                    AsyncImage(
                                        model = thumb,
                                        contentDescription = project.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Rounded.MovieFilter, contentDescription = null, tint = TextMuted)
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(project.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "$dateText • ${formatDurationMs(project.durationMs)} • ${project.width}×${project.height}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${projectClips.size} shorts (${formatBytes(totalClipBytes)}) • Lang: ${project.languageCode.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LavenderAccent
                                )
                            }

                            StatusPill(label = project.status, color = statusColor)
                        }

                        if (!project.errorMessage.isNullOrBlank()) {
                            Text(project.errorMessage, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onOpenProjectClips(project.id) },
                                border = BorderStroke(1.dp, StudioOutline),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.MovieFilter, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open ${projectClips.size} Clips", color = TextPrimary)
                            }
                            IconButton(
                                onClick = { projectPendingDelete = project },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete project", tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
