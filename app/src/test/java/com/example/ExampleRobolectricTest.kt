package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiVideoAiService
import com.example.data.media.VideoProcessingEngine
import com.example.data.media.YouTubeMetadataFetcher
import com.example.model.SubtitleCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ClipForge", appName)
    }

    @Test
    fun `youtube url parser extracts valid video ids`() {
        val fetcher = YouTubeMetadataFetcher()
        assertEquals("dQw4w9WgXcQ", fetcher.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", fetcher.extractVideoId("https://youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("dQw4w9WgXcQ", fetcher.extractVideoId("https://youtube.com/shorts/dQw4w9WgXcQ"))
        assertNull(fetcher.extractVideoId("https://example.com/not-youtube"))
    }

    @Test
    fun `srt generator formats bengali and english cues accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = VideoProcessingEngine(context)
        val cues = listOf(
            SubtitleCue(
                id = "1",
                startMs = 1250L,
                endMs = 4500L,
                text = "স্বাগতম ClipForge এ!"
            )
        )
        val srt = engine.generateSrtContent(cues)
        assertTrue(srt.contains("00:00:01,250 --> 00:00:04,500"))
        assertTrue(srt.contains("স্বাগতম ClipForge এ!"))
    }

    @Test
    fun `word timing estimator splits sentences across duration`() {
        val words = GeminiVideoAiService.estimateWordTimings("One two three", 0L, 3000L)
        assertEquals(3, words.size)
        assertEquals(0L, words.first().startMs)
        assertEquals(3000L, words.last().endMs)
    }
}
