package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceType: String, // "LOCAL" or "YOUTUBE_ATTACHED"
    val sourceUrl: String? = null,
    val sourceFilePath: String,
    val thumbnailPath: String? = null,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val languageCode: String,
    val selectionMode: String,
    val customPrompt: String,
    val targetMinSec: Int,
    val targetMaxSec: Int,
    val requestedClipCount: Int,
    val cropMode: String,
    val status: String, // "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "clips",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class ClipEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long,
    val highlightScore: Int,
    val highlightReason: String,
    val tagsCsv: String,
    val outputVideoPath: String,
    val srtFilePath: String,
    val thumbnailPath: String? = null,
    val fileSizeBytes: Long,
    val subtitleStatus: String, // "BURNED_IN", "SRT_ONLY", "NONE"
    val cropMode: String,
    val focusCenterX: Float = 0.5f,
    val subtitleTemplateName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subtitle_cues",
    foreignKeys = [
        ForeignKey(
            entity = ClipEntity::class,
            parentColumns = ["id"],
            childColumns = ["clipId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clipId")]
)
data class SubtitleCueEntity(
    @PrimaryKey val id: String,
    val clipId: String,
    val startMs: Long, // Relative to clip start
    val endMs: Long,   // Relative to clip start
    val text: String,
    val speaker: String,
    val language: String,
    val wordsJson: String // JSON array of {w, s, e}
)
