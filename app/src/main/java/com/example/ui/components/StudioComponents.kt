package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.model.NavDestination
import com.example.model.ProcessingJobState
import com.example.model.ProcessingStage
import com.example.model.SubtitleCue
import com.example.model.SubtitleFontFamily
import com.example.model.SubtitlePosition
import com.example.model.SubtitleStyleConfig
import com.example.ui.theme.BengaliFontFamily
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.MontserratFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletDeepContainer
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

fun NavDestination.icon(selected: Boolean): ImageVector = when (this) {
    NavDestination.DASHBOARD -> if (selected) Icons.Rounded.Dashboard else Icons.Outlined.Dashboard
    NavDestination.NEW_PROJECT -> if (selected) Icons.Rounded.AddBox else Icons.Outlined.AddBox
    NavDestination.PROJECTS -> if (selected) Icons.Rounded.FolderCopy else Icons.Outlined.FolderCopy
    NavDestination.CLIPS -> if (selected) Icons.Rounded.MovieFilter else Icons.Outlined.MovieFilter
    NavDestination.SUBTITLE_STUDIO -> if (selected) Icons.Rounded.ClosedCaption else Icons.Outlined.ClosedCaption
    NavDestination.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
}

@Composable
fun ClipForgeSidebar(
    currentDestination: NavDestination,
    onSelectDestination: (NavDestination) -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioCard,
        border = BorderStroke(1.dp, StudioOutline),
        modifier = modifier
            .width(236.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VioletAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "ClipForge Logo",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CLIPFORGE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "AI Video Studio",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            NavDestination.entries.forEach { dest ->
                val selected = dest == currentDestination
                val bg by animateColorAsState(
                    targetValue = if (selected) VioletDeepContainer else Color.Transparent,
                    label = "sidebarBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) LavenderAccent else TextMuted,
                    label = "sidebarContent"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .then(
                            if (selected) Modifier.border(1.dp, VioletAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clickable { onSelectDestination(dest) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("nav_sidebar_${dest.route}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = dest.icon(selected),
                        contentDescription = dest.label,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) TextPrimary else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (dest == NavDestination.NEW_PROJECT && isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = LavenderAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = StudioCardElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StudioOutline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Personal Studio v1.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = LavenderAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sequential CPU-safe 9:16 H.264 rendering with Gemini AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ClipForgeBottomNav(
    currentDestination: NavDestination,
    onSelectDestination: (NavDestination) -> Unit
) {
    NavigationBar(
        containerColor = StudioCard,
        contentColor = TextPrimary,
        windowInsets = WindowInsets.navigationBars,
        tonalElevation = 0.dp
    ) {
        NavDestination.entries.forEach { dest ->
            val selected = dest == currentDestination
            val shortLabel = when (dest) {
                NavDestination.DASHBOARD -> "Home"
                NavDestination.NEW_PROJECT -> "Create"
                NavDestination.PROJECTS -> "Projects"
                NavDestination.CLIPS -> "Clips"
                NavDestination.SUBTITLE_STUDIO -> "Subtitles"
                NavDestination.SETTINGS -> "Settings"
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectDestination(dest) },
                icon = {
                    Icon(
                        imageVector = dest.icon(selected),
                        contentDescription = dest.label
                    )
                },
                label = {
                    Text(
                        text = shortLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LavenderAccent,
                    selectedTextColor = TextPrimary,
                    indicatorColor = VioletDeepContainer,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag("nav_bottom_${dest.route}")
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = VioletAccent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        border = BorderStroke(1.dp, StudioOutline),
        shape = RoundedCornerShape(16.dp)
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ProcessingStagesCard(
    jobState: ProcessingJobState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        border = BorderStroke(1.dp, StudioOutline),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = jobState.projectTitle.ifBlank { "Processing Studio Pipeline" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = jobState.detailMessage.ifBlank { jobState.currentStage.label },
                        style = MaterialTheme.typography.bodySmall,
                        color = LavenderAccent
                    )
                }
                Text(
                    text = "${(jobState.overallProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (jobState.isCompleted) SuccessGreen else VioletAccent
                )
            }

            LinearProgressIndicator(
                progress = { jobState.overallProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = if (jobState.isCompleted) SuccessGreen else VioletAccent,
                trackColor = StudioCardElevated
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProcessingStage.entries.forEach { stage ->
                    val isDone = jobState.isCompleted || stage.stepNumber < jobState.currentStage.stepNumber
                    val isActive = !jobState.isCompleted && stage == jobState.currentStage && jobState.errorMessage == null
                    val isFailed = !jobState.isCompleted && stage == jobState.currentStage && jobState.errorMessage != null

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when {
                            isDone -> Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Completed",
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            isFailed -> Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Failed",
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                            isActive -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LavenderAccent
                            )
                            else -> Icon(
                                imageVector = Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = "Pending",
                                tint = StudioOutline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${stage.stepNumber}. ${stage.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isDone -> TextPrimary
                                isActive -> LavenderAccent
                                isFailed -> ErrorRed
                                else -> TextMuted
                            },
                            fontWeight = if (isActive || isDone) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isActive && jobState.stageProgress > 0f) {
                            Text(
                                text = "${(jobState.stageProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = LavenderAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalClipPlayer(
    videoPath: String,
    cues: List<SubtitleCue> = emptyList(),
    overlayStyle: SubtitleStyleConfig? = null,
    showLiveComposeOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            val file = File(videoPath)
            if (file.exists()) {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer, isPlaying, showLiveComposeOverlay) {
        while (showLiveComposeOverlay) {
            currentPosMs = exoPlayer.currentPosition
            delay(80L)
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBg)
            .border(1.dp, StudioOutline, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (File(videoPath).exists()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Live Compose subtitle style preview overlay (used in Subtitle Studio)
            if (showLiveComposeOverlay && overlayStyle != null) {
                val activeCue = cues.firstOrNull { currentPosMs in it.startMs..it.endMs } ?: cues.firstOrNull()
                if (activeCue != null) {
                    val alignment = when (overlayStyle.position) {
                        SubtitlePosition.TOP -> Alignment.TopCenter
                        SubtitlePosition.CENTER -> Alignment.Center
                        SubtitlePosition.BOTTOM -> Alignment.BottomCenter
                    }
                    val fontFamily = when {
                        activeCue.text.any { it in '\u0980'..'\u09FF' } -> BengaliFontFamily
                        overlayStyle.fontFamily == SubtitleFontFamily.NOTO_SANS_BENGALI -> BengaliFontFamily
                        overlayStyle.fontFamily == SubtitleFontFamily.MONTSERRAT -> MontserratFontFamily
                        overlayStyle.fontFamily == SubtitleFontFamily.ROBOTO_MONO -> RobotoMonoFontFamily
                        else -> InterFontFamily
                    }
                    val activeWordIdx = activeCue.words.indexOfFirst { currentPosMs in it.startMs..it.endMs }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 48.dp),
                        contentAlignment = alignment
                    ) {
                        val bgAlpha = ((overlayStyle.backgroundColorHex ushr 24) and 0xFF) / 255f
                        Surface(
                            color = if (bgAlpha > 0.02f) Color(overlayStyle.backgroundColorHex) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            val annotated = buildAnnotatedString {
                                if (activeCue.words.isEmpty()) {
                                    append(activeCue.text)
                                } else {
                                    activeCue.words.forEachIndexed { idx, w ->
                                        val isCurrent = idx == activeWordIdx
                                        withStyle(
                                            SpanStyle(
                                                color = if (isCurrent) Color(overlayStyle.highlightColorHex) else Color(overlayStyle.textColorHex),
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append(w.word)
                                        }
                                        if (idx < activeCue.words.lastIndex) append(" ")
                                    }
                                }
                            }
                            Text(
                                text = annotated,
                                fontFamily = fontFamily,
                                fontSize = (overlayStyle.fontSizeSp * 0.72f).sp,
                                textAlign = TextAlign.Center,
                                color = Color(overlayStyle.textColorHex),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(ObsidianBg.copy(alpha = 0.65f))
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                    tint = TextPrimary
                )
            }
        } else {
            Text(
                text = "Video file unavailable on disk",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}
