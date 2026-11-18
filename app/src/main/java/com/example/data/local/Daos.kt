package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects")
    fun observeProjectCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM projects WHERE status = 'PROCESSING'")
    fun observeProcessingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)
}

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY createdAt DESC")
    fun observeAllClips(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY startMs ASC")
    fun observeClipsForProject(projectId: String): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY startMs ASC")
    suspend fun getClipsForProject(projectId: String): List<ClipEntity>

    @Query("SELECT * FROM clips WHERE id = :clipId")
    suspend fun getClipById(clipId: String): ClipEntity?

    @Query("SELECT COUNT(*) FROM clips")
    fun observeClipCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<ClipEntity>)

    @Update
    suspend fun updateClip(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE id = :clipId")
    suspend fun deleteClipById(clipId: String)
}

@Dao
interface SubtitleCueDao {
    @Query("SELECT * FROM subtitle_cues WHERE clipId = :clipId ORDER BY startMs ASC")
    fun observeCuesForClip(clipId: String): Flow<List<SubtitleCueEntity>>

    @Query("SELECT * FROM subtitle_cues WHERE clipId = :clipId ORDER BY startMs ASC")
    suspend fun getCuesForClip(clipId: String): List<SubtitleCueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCues(cues: List<SubtitleCueEntity>)

    @Query("DELETE FROM subtitle_cues WHERE clipId = :clipId")
    suspend fun deleteCuesForClip(clipId: String)
}
