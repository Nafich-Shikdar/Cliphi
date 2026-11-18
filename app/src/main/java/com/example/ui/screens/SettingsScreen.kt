package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.model.DurationPreset
import com.example.model.ExportQuality
import com.example.model.HardwareProfile
import com.example.model.LanguageOption
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBytes
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VioletDeepContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    hardwareProfile: HardwareProfile,
    storageUsageBytes: Long,
    onUpdateSettings: (AppSettings) -> Unit,
    onClearExportCache: () -> Unit
) {
    val availableModels = listOf(
        "gemini-3.8-flash" to "Gemini 3.8 Flash (Recommended Default)",
        "gemini-3.5-flash-lite" to "Gemini 3.5 Flash-Lite (Fastest / Low Latency)",
        "gemini-4-argon" to "Gemini 4 Argon (Deep Reasoning)"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Studio Settings", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    text = "Configure Gemini AI models, video export quality, defaults, and hardware optimization.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Gemini AI Configuration (Firebase AI Logic + App Check)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Gemini AI Configuration", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        StatusPill(label = "App Check Active", color = SuccessGreen)
                    }
                    Text(
                        text = "Authenticated securely through Firebase AI Logic and App Check attestation. Zero client-side API keys are stored or exposed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Text("AI Model Selection", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableModels.forEach { (modelId, label) ->
                            FilterChip(
                                selected = settings.aiModelName == modelId,
                                onClick = { onUpdateSettings(settings.copy(aiModelName = modelId)) },
                                label = { Text(label) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletDeepContainer,
                                    selectedLabelColor = LavenderAccent
                                ),
                                modifier = Modifier.testTag("model_$modelId")
                            )
                        }
                    }
                }
            }
        }

        // Transcription & Clip Defaults
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Transcription & Clip Defaults", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                    Text("Default Speech Language", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LanguageOption.entries.forEach { lang ->
                            FilterChip(
                                selected = settings.defaultLanguage == lang,
                                onClick = { onUpdateSettings(settings.copy(defaultLanguage = lang)) },
                                label = { Text(lang.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletDeepContainer,
                                    selectedLabelColor = LavenderAccent
                                )
                            )
                        }
                    }

                    Text("Default Clip Duration", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DurationPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = settings.defaultDurationPreset == preset,
                                onClick = { onUpdateSettings(settings.copy(defaultDurationPreset = preset)) },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VioletDeepContainer,
                                    selectedLabelColor = LavenderAccent
                                )
                            )
                        }
                    }

                    Text("Default Number of Clips", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10, 15, 20).forEach { count ->
                            FilterChip(
                                selected = settings.defaultClipCount == count,
                                onClick = { onUpdateSettings(settings.copy(defaultClipCount = count)) },
                                label = { Text("$count") },
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

        // Video Export Quality
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Video Export Quality (9:16 H.264 + AAC)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    ExportQuality.entries.forEach { quality ->
                        FilterChip(
                            selected = settings.exportQuality == quality,
                            onClick = { onUpdateSettings(settings.copy(exportQuality = quality)) },
                            label = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(quality.label, style = MaterialTheme.typography.titleSmall)
                                    Text(quality.ramHint, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VioletDeepContainer,
                                selectedLabelColor = LavenderAccent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Storage & Hardware Optimization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Storage, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Storage & Export Location", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Text(
                        text = "Export folder: ${settings.storageLocationLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = "Studio workspace usage: ${formatBytes(storageUsageBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    OutlinedButton(
                        onClick = onClearExportCache,
                        border = BorderStroke(1.dp, StudioOutline),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("clear_cache_button")
                    ) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Temporary ZIP Cache", color = TextPrimary)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCard),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Memory, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Hardware Optimization Diagnostics", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Text(
                        text = "CPU Cores: ${hardwareProfile.cpuCores} • System RAM: ${hardwareProfile.totalRamMb} MB • Free Storage: ${hardwareProfile.availableStorageMb} MB • ABI: ${hardwareProfile.abi}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LavenderAccent
                    )
                    Text(
                        text = "• Sequential clip rendering prevents out-of-memory crashes.\n• Compact audio extraction (<11 MB chunks) avoids heavy video uploads.\n• No discrete NVIDIA GPU required; uses hardware MediaCodec when present with CPU fallback.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
