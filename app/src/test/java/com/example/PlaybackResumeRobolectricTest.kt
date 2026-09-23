package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.VideoDao
import com.example.data.database.AppDatabase
import com.example.data.model.VideoEntity
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackResumeRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var videoDao: VideoDao
    private lateinit var repository: VideoRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        videoDao = database.videoDao()
        repository = VideoRepository(context, videoDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRoomSavesAndRetrievesPlaybackPosition() = runBlocking {
        val testVideo = VideoEntity(
            id = "test_video_1",
            title = "Test Video 1",
            uriString = "android.resource://com.example/123",
            durationMs = 60000L,
            lastPositionMs = 0L
        )

        videoDao.insertVideo(testVideo)

        // Save position in Room
        repository.savePlaybackPosition("test_video_1", 25400L, 60000L)

        // Query saved position from Room directly
        val savedPosition = repository.getLastPlaybackPosition("test_video_1")
        assertEquals(25400L, savedPosition)

        val retrievedVideo = repository.getVideoById("test_video_1")
        assertNotNull(retrievedVideo)
        assertEquals(25400L, retrievedVideo?.lastPositionMs)
        assertEquals(60000L, retrievedVideo?.durationMs)
    }

    @Test
    fun testRoomResetsPlaybackPositionOnCompletion() = runBlocking {
        val testVideo = VideoEntity(
            id = "test_video_2",
            title = "Test Video 2",
            uriString = "android.resource://com.example/456",
            durationMs = 30000L,
            lastPositionMs = 28000L
        )

        videoDao.insertVideo(testVideo)
        assertEquals(28000L, repository.getLastPlaybackPosition("test_video_2"))

        // Reset position (e.g. video finished)
        repository.resetPlaybackPosition("test_video_2")
        val resetPosition = repository.getLastPlaybackPosition("test_video_2")
        assertEquals(0L, resetPosition)
    }
}
