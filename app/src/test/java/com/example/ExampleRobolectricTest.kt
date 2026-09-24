package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.PlayerScreenType
import com.example.ui.PlayerViewModel
import com.example.ui.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Application
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        viewModel = PlayerViewModel(context)
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Video Player by Tahmid", appName)
    }

    @Test
    fun `test grid and list view toggle behavior`() {
        // Initial state should default to Grid View
        assertTrue("Initial layout mode should be Grid view", viewModel.isGridView.value)

        // Toggle from Grid to List
        viewModel.toggleGridView()
        assertFalse("Toggled layout mode should be List view", viewModel.isGridView.value)

        // Toggle back from List to Grid
        viewModel.toggleGridView()
        assertTrue("Toggled back layout mode should be Grid view", viewModel.isGridView.value)

        // Explicit setGridView
        viewModel.setGridView(false)
        assertFalse("Explicit setGridView(false) should set List view", viewModel.isGridView.value)

        viewModel.setGridView(true)
        assertTrue("Explicit setGridView(true) should set Grid view", viewModel.isGridView.value)
    }

    @Test
    fun `test screen routing transitions between HOME, SETTINGS, and PLAYER`() {
        // Default initial screen should be HOME
        assertEquals("Initial screen should be HOME", PlayerScreenType.HOME, viewModel.currentScreen.value)

        // Navigate to SETTINGS
        viewModel.navigateTo(PlayerScreenType.SETTINGS)
        assertEquals("Screen should transition to SETTINGS", PlayerScreenType.SETTINGS, viewModel.currentScreen.value)

        // Navigate to FOLDERS
        viewModel.navigateTo(PlayerScreenType.FOLDERS)
        assertEquals("Screen should transition to FOLDERS", PlayerScreenType.FOLDERS, viewModel.currentScreen.value)

        // Navigate to PLAYER
        viewModel.navigateTo(PlayerScreenType.PLAYER)
        assertEquals("Screen should transition to PLAYER", PlayerScreenType.PLAYER, viewModel.currentScreen.value)

        // Navigate back to HOME
        viewModel.navigateTo(PlayerScreenType.HOME)
        assertEquals("Screen should transition back to HOME", PlayerScreenType.HOME, viewModel.currentScreen.value)
    }

    @Test
    fun `test theme mode StateFlow updates`() {
        // Default should be DARK theme
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        // Switch to LIGHT
        viewModel.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)

        // Switch to SYSTEM
        viewModel.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        // Switch back to DARK
        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `test gesture sensitivity clamping and updates`() {
        // Default sensitivity is 1.0f
        assertEquals(1.0f, viewModel.gestureSensitivity.value, 0.001f)

        // Set to 1.5f
        viewModel.setGestureSensitivity(1.5f)
        assertEquals(1.5f, viewModel.gestureSensitivity.value, 0.001f)

        // Test lower bound clamping (minimum is 0.5f)
        viewModel.setGestureSensitivity(0.1f)
        assertEquals(0.5f, viewModel.gestureSensitivity.value, 0.001f)

        // Test upper bound clamping (maximum is 2.0f)
        viewModel.setGestureSensitivity(3.5f)
        assertEquals(2.0f, viewModel.gestureSensitivity.value, 0.001f)
    }

    @Test
    fun `test haptic and background playback settings`() {
        // Haptics default to true
        assertTrue(viewModel.isHapticsEnabled.value)
        viewModel.setHapticsEnabled(false)
        assertFalse(viewModel.isHapticsEnabled.value)

        // Background playback default to false
        assertFalse(viewModel.isBackgroundPlaybackEnabled.value)
        viewModel.setBackgroundPlaybackEnabled(true)
        assertTrue(viewModel.isBackgroundPlaybackEnabled.value)
    }

    @Test
    fun `test double tap seek seconds configuration and feedback`() {
        // Default is 10 seconds
        assertEquals(10, viewModel.doubleTapSeekSeconds.value)

        // Set to 5 seconds
        viewModel.setDoubleTapSeekSeconds(5)
        assertEquals(5, viewModel.doubleTapSeekSeconds.value)

        // Set to 15 seconds
        viewModel.setDoubleTapSeekSeconds(15)
        assertEquals(15, viewModel.doubleTapSeekSeconds.value)

        // Test seek feedback emits updated seconds
        viewModel.onDoubleTapSeek(com.example.ui.SeekDirection.FORWARD)
        val feedback = viewModel.seekFeedback.value
        assertEquals(com.example.ui.SeekDirection.FORWARD, feedback?.direction)
        assertEquals(15, feedback?.seconds)

        // Test clamping (minimum 5s, maximum 60s)
        viewModel.setDoubleTapSeekSeconds(1)
        assertEquals(5, viewModel.doubleTapSeekSeconds.value)

        viewModel.setDoubleTapSeekSeconds(100)
        assertEquals(60, viewModel.doubleTapSeekSeconds.value)
    }

    @Test
    fun `test playback preferences autoplay autoresume and keep screen on`() {
        // Default playback speed
        assertEquals(1.0f, viewModel.defaultPlaybackSpeed.value, 0.001f)
        viewModel.setDefaultPlaybackSpeed(1.5f)
        assertEquals(1.5f, viewModel.defaultPlaybackSpeed.value, 0.001f)
        assertEquals(1.5f, viewModel.playbackSpeed.value, 0.001f)

        // Autoplay next video
        assertTrue(viewModel.isAutoPlayNextEnabled.value)
        viewModel.setAutoPlayNextEnabled(false)
        assertFalse(viewModel.isAutoPlayNextEnabled.value)

        // Autoresume watch progress
        assertTrue(viewModel.isAutoResumeEnabled.value)
        viewModel.setAutoResumeEnabled(false)
        assertFalse(viewModel.isAutoResumeEnabled.value)

        // Keep screen on
        assertTrue(viewModel.isKeepScreenOnEnabled.value)
        viewModel.setKeepScreenOnEnabled(false)
        assertFalse(viewModel.isKeepScreenOnEnabled.value)

        // Autohide controls delay
        assertEquals(3, viewModel.autoHideControlsDelaySeconds.value)
        viewModel.setAutoHideControlsDelaySeconds(5)
        assertEquals(5, viewModel.autoHideControlsDelaySeconds.value)
    }

    @Test
    fun `test playVideo navigates to PLAYER screen and sets playing state`() {
        val testVideo = com.example.data.model.VideoEntity(
            id = "test-video-1",
            title = "Test Local Video.mp4",
            uriString = "file:///storage/emulated/0/Movies/Test.mp4",
            durationMs = 60000L,
            resolution = "1080p FHD",
            subtitleUri = null,
            lastPlayedTimestamp = System.currentTimeMillis(),
            orderIndex = 1
        )
        // Initially on HOME screen
        assertEquals(PlayerScreenType.HOME, viewModel.currentScreen.value)

        // Calling playVideo synchronously updates screen and isPlaying state
        viewModel.playVideo(testVideo)
        assertEquals(PlayerScreenType.PLAYER, viewModel.currentScreen.value)
        assertTrue(viewModel.isPlaying.value)
    }

    @Test
    fun `test playback speed overlay and ExoPlayer playback parameters adjustment`() {
        val expectedSpeeds = listOf(0.5f, 0.8f, 1.0f, 1.5f, 1.75f, 1.85f, 2.0f)

        // Verify available speeds match user specification exactly
        assertEquals(expectedSpeeds, PlayerViewModel.AVAILABLE_SPEEDS)

        // Test overlay visibility controls
        assertFalse("Overlay initially closed", viewModel.isSpeedOverlayVisible.value)
        viewModel.openSpeedOverlay()
        assertTrue("Overlay opened", viewModel.isSpeedOverlayVisible.value)
        viewModel.closeSpeedOverlay()
        assertFalse("Overlay closed", viewModel.isSpeedOverlayVisible.value)

        // Test setting each requested speed updates ViewModel state AND ExoPlayer PlaybackParameters
        expectedSpeeds.forEach { targetSpeed ->
            viewModel.openSpeedOverlay()
            viewModel.setPlaybackSpeed(targetSpeed)

            // Playback speed in StateFlow updated
            assertEquals("StateFlow speed matches $targetSpeed", targetSpeed, viewModel.playbackSpeed.value, 0.001f)

            // ExoPlayer PlaybackParameters speed updated
            assertEquals("ExoPlayer playback parameters speed matches $targetSpeed", targetSpeed, viewModel.exoPlayer.playbackParameters.speed, 0.001f)

            // Overlay automatically dismissed after selecting preset
            assertFalse("Overlay dismissed after speed selected", viewModel.isSpeedOverlayVisible.value)
        }

        // Test step speed up and down
        viewModel.setPlaybackSpeed(1.0f)
        viewModel.stepSpeedUp()
        assertEquals(1.5f, viewModel.playbackSpeed.value, 0.001f)
        assertEquals(1.5f, viewModel.exoPlayer.playbackParameters.speed, 0.001f)

        viewModel.stepSpeedUp()
        assertEquals(1.75f, viewModel.playbackSpeed.value, 0.001f)

        viewModel.stepSpeedUp()
        assertEquals(1.85f, viewModel.playbackSpeed.value, 0.001f)

        viewModel.stepSpeedDown()
        assertEquals(1.75f, viewModel.playbackSpeed.value, 0.001f)
        assertEquals(1.75f, viewModel.exoPlayer.playbackParameters.speed, 0.001f)
    }

    @Test
    fun `test demo videos are purged and not seeded into library`() = kotlinx.coroutines.test.runTest {
        val database = com.example.data.database.AppDatabase.getInstance(context)
        val dao = database.videoDao()
        val repo = com.example.data.repository.VideoRepository(context, dao)

        // Insert mock legacy demo entities
        val demo1 = com.example.data.model.VideoEntity(
            id = "sample_cosmic",
            title = "Cosmic Odyssey (Offline Demo)",
            uriString = "android.resource://pkg/sample_cosmic",
            durationMs = 18000L,
            resolution = "720p HD",
            orderIndex = 0
        )
        val demo2 = com.example.data.model.VideoEntity(
            id = "sample_neon",
            title = "Neon Horizon (Synthwave Loop)",
            uriString = "android.resource://pkg/sample_neon",
            durationMs = 15000L,
            resolution = "720p HD",
            orderIndex = 1
        )
        dao.insertAll(listOf(demo1, demo2))

        // Ensure they existed
        assertNotNull(dao.getVideoById("sample_cosmic"))
        assertNotNull(dao.getVideoById("sample_neon"))

        // Run purgeDemoVideos
        repo.purgeDemoVideos()

        // Verify demo videos are completely removed from database
        assertNull(dao.getVideoById("sample_cosmic"))
        assertNull(dao.getVideoById("sample_neon"))

        // Run initializeDefaultVideosIfEmpty and ensure no demo videos are seeded
        repo.initializeDefaultVideosIfEmpty()
        assertEquals(0, dao.getVideoCount())
    }
}
