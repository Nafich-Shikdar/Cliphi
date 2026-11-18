package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ClipEntity
import com.example.model.SubtitleCue
import com.example.model.SubtitleFontFamily
import com.example.model.SubtitlePosition
import com.example.model.SubtitleStyleConfig
import com.example.model.SubtitleTemplate
import com.example.model.VerticalCropMode
import com.example.ui.components.VerticalClipPlayer
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.BengaliFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.MontserratFontFamily
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletDeepContainer
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubtitleStudioScreen(
    allClips: List<ClipEntity>,
    editingClip: ClipEntity?,
    cues: List<SubtitleCue>,
    subtitleStyle: SubtitleStyleConfig,
    reRenderProgress: Float?,
    onSelectClip: (ClipEntity) -> Unit,
    onApplyTemplate: (SubtitleTemplate) -> Unit,
    onUpdateStyle: (SubtitleStyleConfig) -> Unit,
    onSaveCues: (ClipEntity, List<SubtitleCue>) -> Unit,
    onReRenderClip: (ClipEntity, Long, Long, VerticalCropMode, List<SubtitleCue>) -> Unit,
    onDownloadSrt: (ClipEntity) -> Unit
) {
    val activeClip = editingClip ?: allClips.firstOrNull()
    val editableCues = remember(activeClip?.id, cues) {
        mutableStateListOf<SubtitleCue>().apply { addAll(cues) }
    }

    var trimStartMs by remember(activeClip?.id) { mutableLongStateOf(activeClip?.startMs ?: 0L) }
    var trimEndMs by remember(activeClip?.id) { mutableLongStateOf(activeClip?.endMs ?: 30_000L) }
    var cropMode by remember(activeClip?.id) {
        mutableStateOf(
            activeClip?.cropMode?.let { runCatching { VerticalCropMode.valueOf(it) }.getOrNull() }
                ?: VerticalCropMode.SMART_FACE_TRACK
        )
    }

    LaunchedEffect(activeClip?.id, cues) {
        editableCues.clear()
        editableCues.addAll(cues)
    }

    val colorSwatches = remember {
        listOf(
            0xFFF8FAFC, // Crisp White
            0xFFFACC15, // Creator Yellow
            0xFF8B5CF6, // ClipForge Violet
            0xFFA78BFA, // Lavender
            0xFF38BDF8, // Cyan
            0xFF10B981, // Emerald
            0xFF000000  // Black
        )
    }

    val bgSwatches = remember {
        listOf(
            0x00000000, // Transparent
            0x990B0D14, // Obsidian Glass
            0xD92E1E59, // Deep Violet
            0xCC141927, // Studio Card
            0xD9000000  // Solid Dark
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Subtitle Studio", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    text = "Customize Bengali (বাংলা) & English captions, templates, trim, and burn-in style.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        if (allClips.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allClips, key = { it.id }) { clip ->
                        FilterChip(
                            selected = activeClip?.id == clip.id,
                            onClick = { onSelectClip(clip) },
                            label = { Text(clip.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VioletDeepContainer,
                                selectedLabelColor = LavenderAccent
                            )
                        )
                    }
                }
            }
        }

        if (activeClip == null) {
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
                        Icon(Icons.Rounded.ClosedCaption, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(38.dp))
                        Text("No clip selected", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = "Generate clips in New Project first, then select any short to style subtitles and re-render.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // Live 9:16 Video & Subtitle Style Preview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Live 9:16 Preview • ${activeClip.title}",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    VerticalClipPlayer(
                        videoPath = activeClip.outputVideoPath,
                        cues = editableCues,
                        overlayStyle = subtitleStyle,
                        showLiveComposeOverlay = true,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                }
            }
        }

        // 5 Caption Templates
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Caption Templates", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubtitleTemplate.entries.forEach { tpl ->
                            FilterChip(
                                selected = subtitleStyle.template == tpl,
                                onClick = { onApplyTemplate(tpl) },
                                label = { Text(tpl.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletDeepContainer,
                                    selectedLabelColor = LavenderAccent
                                ),
                                modifier = Modifier.testTag("template_${tpl.name.lowercase()}")
                            )
                        }
                    }

                    // Font Family (including Bengali font)
                    Text("Font Family", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubtitleFontFamily.entries.forEach { fam ->
                            FilterChip(
                                selected = subtitleStyle.fontFamily == fam,
                                onClick = { onUpdateStyle(subtitleStyle.copy(fontFamily = fam)) },
                                label = { Text(fam.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletDeepContainer,
                                    selectedLabelColor = LavenderAccent
                                )
                            )
                        }
                    }

                    // Font Size
                    Text(
                        text = "Font Size: ${subtitleStyle.fontSizeSp.roundToInt()} sp",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Slider(
                        value = subtitleStyle.fontSizeSp,
                        onValueChange = { onUpdateStyle(subtitleStyle.copy(fontSizeSp = it)) },
                        valueRange = 16f..34f,
                        colors = SliderDefaults.colors(thumbColor = VioletAccent, activeTrackColor = VioletAccent)
                    )

                    // Position
                    Text("Position", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubtitlePosition.entries.forEach { pos ->
                            FilterChip(
                                selected = subtitleStyle.position == pos,
                                onClick = { onUpdateStyle(subtitleStyle.copy(position = pos)) },
                                label = { Text(pos.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletDeepContainer,
                                    selectedLabelColor = LavenderAccent
                                )
                            )
                        }
                    }

                    // Color Swatches
                    ColorSwatchRow("Text Color", colorSwatches, subtitleStyle.textColorHex) {
                        onUpdateStyle(subtitleStyle.copy(textColorHex = it))
                    }
                    ColorSwatchRow("Background Pill", bgSwatches, subtitleStyle.backgroundColorHex) {
                        onUpdateStyle(subtitleStyle.copy(backgroundColorHex = it))
                    }
                    ColorSwatchRow("Outline Stroke", colorSwatches, subtitleStyle.outlineColorHex) {
                        onUpdateStyle(subtitleStyle.copy(outlineColorHex = it))
                    }
                    ColorSwatchRow("Active Word Highlight", colorSwatches, subtitleStyle.highlightColorHex) {
                        onUpdateStyle(subtitleStyle.copy(highlightColorHex = it))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Burn-in Subtitles on Re-Render", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Disable to export clean video + external SRT only", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Switch(
                            checked = subtitleStyle.burnInSubtitles,
                            onCheckedChange = { onUpdateStyle(subtitleStyle.copy(burnInSubtitles = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = VioletAccent)
                        )
                    }
                }
            }
        }

        // Clip Trim & Framing Adjustment
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Clip Trim & Vertical Framing", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = "Original Window: ${formatDurationMs(trimStartMs)} – ${formatDurationMs(trimEndMs)} (${formatDurationMs(trimEndMs - trimStartMs)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = LavenderAccent
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { trimStartMs = (trimStartMs - 1000L).coerceAtLeast(0L) },
                            border = BorderStroke(1.dp, StudioOutline)
                        ) { Text("-1s Start", color = TextPrimary) }
                        OutlinedButton(
                            onClick = { trimStartMs = (trimStartMs + 1000L).coerceAtMost(trimEndMs - 3000L) },
                            border = BorderStroke(1.dp, StudioOutline)
                        ) { Text("+1s Start", color = TextPrimary) }
                        OutlinedButton(
                            onClick = { trimEndMs = (trimEndMs - 1000L).coerceAtLeast(trimStartMs + 3000L) },
                            border = BorderStroke(1.dp, StudioOutline)
                        ) { Text("-1s End", color = TextPrimary) }
                        OutlinedButton(
                            onClick = { trimEndMs += 1000L },
                            border = BorderStroke(1.dp, StudioOutline)
                        ) { Text("+1s End", color = TextPrimary) }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
            }
        }

        // Transcript Cue Editor (Bengali + English)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Transcript Cues (${editableCues.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        OutlinedButton(
                            onClick = { onDownloadSrt(activeClip) },
                            border = BorderStroke(1.dp, StudioOutline)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export SRT", color = TextPrimary)
                        }
                    }

                    if (editableCues.isEmpty()) {
                        Text(
                            text = "No spoken subtitle cues were found for this clip.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        editableCues.forEachIndexed { index, cue ->
                            val cueFont = if (cue.text.any { it in '\u0980'..'\u09FF' }) BengaliFontFamily else InterFontFamily
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StudioCardElevated)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${formatDurationMs(cue.startMs)} → ${formatDurationMs(cue.endMs)} • ${cue.speaker}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LavenderAccent
                                )
                                OutlinedTextField(
                                    value = cue.text,
                                    onValueChange = { updated ->
                                        editableCues[index] = cue.copy(text = updated)
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = cueFont, color = TextPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = VioletAccent,
                                        unfocusedBorderColor = StudioOutline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (reRenderProgress != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Re-rendering 9:16 MP4 (${(reRenderProgress * 100).toInt()}%)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = LavenderAccent
                            )
                            LinearProgressIndicator(
                                progress = { reRenderProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = VioletAccent,
                                trackColor = StudioCardElevated
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSaveCues(activeClip, editableCues.toList()) },
                            border = BorderStroke(1.dp, StudioOutline),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_srt_cues_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save SRT", color = TextPrimary)
                        }
                        Button(
                            onClick = {
                                onReRenderClip(activeClip, trimStartMs, trimEndMs, cropMode, editableCues.toList())
                            },
                            enabled = reRenderProgress == null,
                            colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("rerender_clip_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Re-Render MP4", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    label: String,
    swatches: List<Long>,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.forEach { hex ->
                val isSelected = hex == selectedHex
                val isTransparent = ((hex ushr 24) and 0xFF) == 0L
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isTransparent) StudioCardElevated else Color(hex))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) LavenderAccent else StudioOutline,
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isTransparent) {
                        Text("Ø", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }
    }
}
