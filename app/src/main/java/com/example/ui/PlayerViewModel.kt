package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.util.Log
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.database.AppDatabase
import com.example.data.model.VideoEntity
import com.example.data.repository.VideoRepository
import com.example.util.ScreenshotHelper
import com.example.util.SubtitleCue
import com.example.util.SubtitleParser
import com.example.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PlayerScreenType {
    HOME, FOLDERS, SETTINGS, PLAYER
}

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class SeekDirection {
    BACKWARD, FORWARD
}

data class SeekFeedback(
    val direction: SeekDirection,
    val seconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class ResumePlaybackEvent(
    val videoId: String,
    val positionMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository
    val videos: StateFlow<List<VideoEntity>>

    // Screen Navigation Router
    private val _currentScreen = MutableStateFlow(PlayerScreenType.HOME)
    val currentScreen: StateFlow<PlayerScreenType> = _currentScreen.asStateFlow()

    // Library Grid ↔ List View Switcher
    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Screenshot Banner State
    private val _screenshotSuccess = MutableStateFlow<String?>(null)
    val screenshotSuccess: StateFlow<String?> = _screenshotSuccess.asStateFlow()

    // Settings State
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _gestureSensitivity = MutableStateFlow(1.0f)
    val gestureSensitivity: StateFlow<Float> = _gestureSensitivity.asStateFlow()

    private val _isHapticsEnabled = MutableStateFlow(true)
    val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

    private val _isBackgroundPlaybackEnabled = MutableStateFlow(false)
    val isBackgroundPlaybackEnabled: StateFlow<Boolean> = _isBackgroundPlaybackEnabled.asStateFlow()

    // Double-Tap Seek Interval in Seconds (5s, 10s, 15s, 20s, 30s, 60s)
    private val _doubleTapSeekSeconds = MutableStateFlow(10)
    val doubleTapSeekSeconds: StateFlow<Int> = _doubleTapSeekSeconds.asStateFlow()

    // Default Playback Speed
    private val _defaultPlaybackSpeed = MutableStateFlow(1.0f)
    val defaultPlaybackSpeed: StateFlow<Float> = _defaultPlaybackSpeed.asStateFlow()

    // Auto-Play Next Video
    private val _isAutoPlayNextEnabled = MutableStateFlow(true)
    val isAutoPlayNextEnabled: StateFlow<Boolean> = _isAutoPlayNextEnabled.asStateFlow()

    // Auto-Resume Watch Progress
    private val _isAutoResumeEnabled = MutableStateFlow(true)
    val isAutoResumeEnabled: StateFlow<Boolean> = _isAutoResumeEnabled.asStateFlow()

    // Controls Auto-Hide Duration (seconds)
    private val _autoHideControlsDelaySeconds = MutableStateFlow(3)
    val autoHideControlsDelaySeconds: StateFlow<Int> = _autoHideControlsDelaySeconds.asStateFlow()

    // Screen Keep Awake
    private val _isKeepScreenOnEnabled = MutableStateFlow(true)
    val isKeepScreenOnEnabled: StateFlow<Boolean> = _isKeepScreenOnEnabled.asStateFlow()

    private val _isScanningStorage = MutableStateFlow(false)
    val isScanningStorage: StateFlow<Boolean> = _isScanningStorage.asStateFlow()

    private val _storageScanResult = MutableStateFlow<String?>(null)
    val storageScanResult: StateFlow<String?> = _storageScanResult.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoEntity?>(null)
    val currentVideo: StateFlow<VideoEntity?> = _currentVideo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isControlsVisible = MutableStateFlow(true)
    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()

    private val _isScreenLocked = MutableStateFlow(false)
    val isScreenLocked: StateFlow<Boolean> = _isScreenLocked.asStateFlow()

    private val _isSubtitlesEnabled = MutableStateFlow(true)
    val isSubtitlesEnabled: StateFlow<Boolean> = _isSubtitlesEnabled.asStateFlow()

    private val _currentSubtitleText = MutableStateFlow<String?>(null)
    val currentSubtitleText: StateFlow<String?> = _currentSubtitleText.asStateFlow()

    private var currentSubtitleCues = listOf<SubtitleCue>()

    // Gesture HUD states (null when dismissed, 0.0 - 1.0 when active)
    private val _brightnessPercent = MutableStateFlow<Float?>(null)
    val brightnessPercent: StateFlow<Float?> = _brightnessPercent.asStateFlow()

    private val _volumePercent = MutableStateFlow<Float?>(null)
    val volumePercent: StateFlow<Float?> = _volumePercent.asStateFlow()

    private val _seekFeedback = MutableStateFlow<SeekFeedback?>(null)
    val seekFeedback: StateFlow<SeekFeedback?> = _seekFeedback.asStateFlow()

    // Auto-Resume State
    private val _resumeEvent = MutableStateFlow<ResumePlaybackEvent?>(null)
    val resumeEvent: StateFlow<ResumePlaybackEvent?> = _resumeEvent.asStateFlow()

    private val _resumeNotification = MutableStateFlow<String?>(null)
    val resumeNotification: StateFlow<String?> = _resumeNotification.asStateFlow()

    private var dismissResumeNoticeJob: Job? = null
    private var lastSavedDbPositionMs: Long = 0L

    // Sheet / Dialogs
    private val _isSpeedSheetVisible = MutableStateFlow(false)
    val isSpeedSheetVisible: StateFlow<Boolean> = _isSpeedSheetVisible.asStateFlow()

    private val _isSpeedOverlayVisible = MutableStateFlow(false)
    val isSpeedOverlayVisible: StateFlow<Boolean> = _isSpeedOverlayVisible.asStateFlow()

    private val _isDetailsDialogVisible = MutableStateFlow(false)
    val isDetailsDialogVisible: StateFlow<Boolean> = _isDetailsDialogVisible.asStateFlow()

    private val _isLandscape = MutableStateFlow(false)
    val isLandscape: StateFlow<Boolean> = _isLandscape.asStateFlow()

    private var autoHideControlsJob: Job? = null
    private var dismissHudJob: Job? = null
    private var dismissSeekFeedbackJob: Job? = null
    private var progressJob: Job? = null
    private var lastErrorRetryTimestamp = 0L
    private var errorRetryCount = 0

    @OptIn(UnstableApi::class)
    val exoPlayer: ExoPlayer by lazy {
        val renderersFactory = DefaultRenderersFactory(getApplication())
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

        ExoPlayer.Builder(getApplication())
            .setRenderersFactory(renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playWhenReady = false
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            errorRetryCount = 0
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            errorRetryCount = 0
                        }
                        if (playbackState == Player.STATE_ENDED) {
                            onVideoCompleted()
                            when (_repeatMode.value) {
                                RepeatMode.ONE -> {
                                    seekTo(0L)
                                    play()
                                }
                                RepeatMode.ALL -> {
                                    if (_currentScreen.value == PlayerScreenType.PLAYER || _isBackgroundPlaybackEnabled.value) {
                                        playNextVideo(loopAround = true)
                                    } else {
                                        _isPlaying.value = false
                                    }
                                }
                                RepeatMode.OFF -> {
                                    if (_isAutoPlayNextEnabled.value && (_currentScreen.value == PlayerScreenType.PLAYER || _isBackgroundPlaybackEnabled.value)) {
                                        val next = playNextVideo(loopAround = false)
                                        if (next == null) {
                                            _isPlaying.value = false
                                        }
                                    } else {
                                        _isPlaying.value = false
                                    }
                                }
                            }
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("PlayerViewModel", "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}", error)
                        val now = System.currentTimeMillis()
                        if (now - lastErrorRetryTimestamp < 3000L) {
                            errorRetryCount++
                        } else {
                            errorRetryCount = 1
                        }
                        lastErrorRetryTimestamp = now

                        // Recover from MediaCodec resource manager reclamation or decoder init failures (max 2 retries)
                        if (errorRetryCount <= 2 && (
                            error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                            error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED ||
                            error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED
                        )) {
                            viewModelScope.launch {
                                val currentPos = currentPosition
                                prepare()
                                seekTo(currentPos)
                                if (_isPlaying.value) {
                                    play()
                                }
                            }
                        } else if (errorRetryCount > 2) {
                            Log.w("PlayerViewModel", "Exceeded max error recovery retries for codec error, pausing playback")
                            _isPlaying.value = false
                        }
                    }
                })
            }
    }

    init {
        val database = AppDatabase.getInstance(application)
        repository = VideoRepository(application, database.videoDao())
        videos = repository.allVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        startProgressPolling()

        viewModelScope.launch {
            repository.purgeDemoVideos()
            videos.collect { list ->
                if (_currentVideo.value != null && list.none { it.id == _currentVideo.value?.id }) {
                    _currentVideo.value = list.firstOrNull()
                    if (_currentVideo.value == null) {
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                    }
                } else if (_currentVideo.value == null && list.isNotEmpty()) {
                    _currentVideo.value = list.first()
                }
            }
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                try {
                    val current = exoPlayer.currentPosition
                    val duration = exoPlayer.duration
                    val buffered = exoPlayer.bufferedPosition
                    if (duration > 0) {
                        updateProgress(current, duration, buffered)
                    }
                } catch (e: Exception) {
                    // Suppress harmless transient read errors during prep/release
                }
                delay(250)
            }
        }
    }

    /**
     * Automatically resumes videos from the last saved playback position using the room database when a video is re-opened.
     *
     * Queries the Room database for the latest saved position for the given video ID.
     * If a saved position exists (> 1 second and not at the very end of the video),
     * playback is automatically resumed from that timestamp, and a helpful visual notification is shown.
     *
     * @param video The video to open and resume
     * @param forceRestart If true, restarts from 0:00 regardless of the saved position
     */
    fun resumeVideoFromLastPosition(video: VideoEntity, forceRestart: Boolean = false) {
        viewModelScope.launch {
            // Persist previous video's progress to Room before switching
            saveCurrentPlaybackPosition()

            // Fetch freshest video state directly from Room database
            val dbVideo = repository.getVideoById(video.id) ?: video
            val savedDbPos = if (forceRestart) 0L else (repository.getLastPlaybackPosition(video.id).takeIf { it > 0L } ?: dbVideo.lastPositionMs)
            val totalDuration = if (dbVideo.durationMs > 0L) dbVideo.durationMs else video.durationMs

            // If the video was watched near the very end (within 2 seconds of the end), restart from 0:00
            val targetPositionMs = if (forceRestart || !_isAutoResumeEnabled.value) {
                0L
            } else if (totalDuration > 3000L && savedDbPos >= totalDuration - 2000L) {
                0L
            } else if (savedDbPos > 1000L) {
                savedDbPos
            } else {
                0L
            }

            lastSavedDbPositionMs = targetPositionMs
            _currentVideo.value = dbVideo.copy(lastPositionMs = targetPositionMs)
            _playbackPositionMs.value = targetPositionMs
            _currentSubtitleText.value = null

            // Dispatch resume event to video player
            _resumeEvent.value = ResumePlaybackEvent(
                videoId = video.id,
                positionMs = targetPositionMs
            )

            // Setup ExoPlayer safely with unified single call
            try {
                val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
                if (currentUri != video.uriString) {
                    val mediaItem = MediaItem.fromUri(Uri.parse(video.uriString))
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                }
                if (targetPositionMs > 0L) {
                    exoPlayer.seekTo(targetPositionMs)
                } else {
                    exoPlayer.seekTo(0L)
                }
                exoPlayer.play()
                _isPlaying.value = true
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error loading video in ExoPlayer: ${e.message}", e)
            }

            // Show resume notification if resumed from a saved position
            if (targetPositionMs > 1000L && !forceRestart) {
                showResumeNotification("Resumed from ${TimeUtils.formatDuration(targetPositionMs)}")
            } else {
                _resumeNotification.value = null
            }

            loadSubtitlesForVideo(dbVideo)
            showControls()
        }
    }

    /**
     * Automatically resumes a video by ID from the last saved playback position using the Room database.
     */
    fun resumeVideoFromLastPosition(videoId: String, forceRestart: Boolean = false) {
        viewModelScope.launch {
            val video = repository.getVideoById(videoId)
                ?: videos.value.find { it.id == videoId }
            if (video != null) {
                resumeVideoFromLastPosition(video, forceRestart)
            }
        }
    }

    fun selectVideo(video: VideoEntity) {
        resumeVideoFromLastPosition(video)
    }

    fun restartCurrentVideo() {
        val current = _currentVideo.value ?: return
        resumeVideoFromLastPosition(current, forceRestart = true)
    }

    fun saveCurrentPlaybackPosition() {
        val video = _currentVideo.value ?: return
        val pos = _playbackPositionMs.value
        val dur = _durationMs.value
        if (pos > 0) {
            viewModelScope.launch {
                repository.savePlaybackPosition(video.id, pos, dur)
            }
        }
    }

    fun onVideoCompleted() {
        val video = _currentVideo.value ?: return
        viewModelScope.launch {
            repository.resetPlaybackPosition(video.id)
            _currentVideo.value = video.copy(lastPositionMs = 0L)
            lastSavedDbPositionMs = 0L
        }
    }

    private fun showResumeNotification(message: String) {
        _resumeNotification.value = message
        dismissResumeNoticeJob?.cancel()
        dismissResumeNoticeJob = viewModelScope.launch {
            delay(3500)
            _resumeNotification.value = null
        }
    }

    fun dismissResumeNotification() {
        dismissResumeNoticeJob?.cancel()
        _resumeNotification.value = null
    }

    private fun loadSubtitlesForVideo(video: VideoEntity) {
        val context = getApplication<Application>()
        if (video.subtitleUri != null) {
            try {
                val uri = Uri.parse(video.subtitleUri)
                currentSubtitleCues = SubtitleParser.loadSubtitleFromUri(context, uri)
            } catch (e: Exception) {
                currentSubtitleCues = emptyList()
            }
        } else {
            currentSubtitleCues = emptyList()
        }
    }

    fun loadExternalSubtitle(uri: Uri) {
        val context = getApplication<Application>()
        val cues = SubtitleParser.loadSubtitleFromUri(context, uri)
        if (cues.isNotEmpty()) {
            currentSubtitleCues = cues
            _isSubtitlesEnabled.value = true
            val video = _currentVideo.value
            if (video != null) {
                viewModelScope.launch {
                    val updated = video.copy(subtitleUri = uri.toString())
                    repository.addVideo(updated)
                    _currentVideo.value = updated
                }
            }
        }
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        if (isPlaying) {
            scheduleAutoHideControls()
        } else {
            autoHideControlsJob?.cancel()
            _isControlsVisible.value = true
        }
    }

    fun updateProgress(currentMs: Long, durationMs: Long, bufferedMs: Long) {
        _playbackPositionMs.value = currentMs
        if (durationMs > 0) {
            _durationMs.value = durationMs
        }
        _bufferedPositionMs.value = bufferedMs

        // Update active subtitle
        if (_isSubtitlesEnabled.value && currentSubtitleCues.isNotEmpty()) {
            _currentSubtitleText.value = SubtitleParser.getActiveSubtitle(currentSubtitleCues, currentMs)
        } else {
            _currentSubtitleText.value = null
        }

        // Periodically persist playback position to Room database without re-emitting entire entity
        val video = _currentVideo.value
        if (video != null && currentMs > 0 && Math.abs(currentMs - lastSavedDbPositionMs) >= 2000L) {
            lastSavedDbPositionMs = currentMs
            viewModelScope.launch {
                repository.savePlaybackPosition(video.id, currentMs, durationMs)
            }
        }
    }

    fun togglePlayPause(): Boolean {
        val willPlay = !exoPlayer.isPlaying
        if (willPlay) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
            saveCurrentPlaybackPosition()
        }
        _isPlaying.value = willPlay
        showControls()
        return willPlay
    }

    fun pausePlayback() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _isPlaying.value = false
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playbackPositionMs.value = positionMs
    }

    fun toggleControls() {
        if (_isScreenLocked.value) {
            // If screen locked, flash the lock pill/button
            _isControlsVisible.value = true
            scheduleAutoHideControls(2500)
            return
        }
        val target = !_isControlsVisible.value
        _isControlsVisible.value = target
        if (target && _isPlaying.value) {
            scheduleAutoHideControls()
        }
    }

    fun showControls() {
        _isControlsVisible.value = true
        if (_isPlaying.value && !_isScreenLocked.value) {
            scheduleAutoHideControls()
        }
    }

    private fun scheduleAutoHideControls(timeoutMs: Long? = null) {
        autoHideControlsJob?.cancel()
        val delayMs = timeoutMs ?: (_autoHideControlsDelaySeconds.value * 1000L)
        autoHideControlsJob = viewModelScope.launch {
            delay(delayMs)
            _isControlsVisible.value = false
        }
    }

    fun cancelAutoHide() {
        autoHideControlsJob?.cancel()
    }

    fun toggleScreenLock() {
        val locked = !_isScreenLocked.value
        _isScreenLocked.value = locked
        if (locked) {
            _isControlsVisible.value = false
        } else {
            showControls()
        }
    }

    fun unlockScreen() {
        _isScreenLocked.value = false
        showControls()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.playbackParameters = PlaybackParameters(speed)
        _isSpeedSheetVisible.value = false
        _isSpeedOverlayVisible.value = false
        showControls()
    }

    fun applyPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    fun openSpeedOverlay() {
        _isSpeedOverlayVisible.value = true
        _isSpeedSheetVisible.value = true
    }

    fun closeSpeedOverlay() {
        _isSpeedOverlayVisible.value = false
        _isSpeedSheetVisible.value = false
    }

    fun toggleSpeedOverlay() {
        val willShow = !_isSpeedOverlayVisible.value
        _isSpeedOverlayVisible.value = willShow
        _isSpeedSheetVisible.value = willShow
    }

    fun stepSpeedDown() {
        val current = _playbackSpeed.value
        val prev = AVAILABLE_SPEEDS.reversed().find { it < current - 0.02f } ?: AVAILABLE_SPEEDS.first()
        setPlaybackSpeed(prev)
    }

    fun stepSpeedUp() {
        val current = _playbackSpeed.value
        val next = AVAILABLE_SPEEDS.find { it > current + 0.02f } ?: AVAILABLE_SPEEDS.last()
        setPlaybackSpeed(next)
    }

    fun openSpeedSheet() {
        _isSpeedSheetVisible.value = true
        _isSpeedOverlayVisible.value = true
    }

    fun closeSpeedSheet() {
        _isSpeedSheetVisible.value = false
        _isSpeedOverlayVisible.value = false
    }

    fun openDetailsDialog() {
        _isDetailsDialogVisible.value = true
    }

    fun closeDetailsDialog() {
        _isDetailsDialogVisible.value = false
    }

    fun toggleSubtitles() {
        _isSubtitlesEnabled.value = !_isSubtitlesEnabled.value
    }

    fun toggleRepeatMode() {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
        exoPlayer.repeatMode = if (next == RepeatMode.ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun setLandscape(landscape: Boolean) {
        _isLandscape.value = landscape
    }

    fun toggleOrientation(): Boolean {
        val newMode = !_isLandscape.value
        _isLandscape.value = newMode
        return newMode
    }

    fun navigateTo(screen: PlayerScreenType) {
        if (screen != PlayerScreenType.PLAYER && !_isBackgroundPlaybackEnabled.value) {
            pausePlayback()
            saveCurrentPlaybackPosition()
        }
        _currentScreen.value = screen
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun setGridView(isGrid: Boolean) {
        _isGridView.value = isGrid
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setGestureSensitivity(sensitivity: Float) {
        _gestureSensitivity.value = sensitivity.coerceIn(0.5f, 2.0f)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _isHapticsEnabled.value = enabled
    }

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        _isBackgroundPlaybackEnabled.value = enabled
    }

    fun clearScreenshotSuccess() {
        _screenshotSuccess.value = null
    }

    fun setDoubleTapSeekSeconds(seconds: Int) {
        _doubleTapSeekSeconds.value = seconds.coerceIn(5, 60)
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        _defaultPlaybackSpeed.value = speed
        setPlaybackSpeed(speed)
    }

    fun setAutoPlayNextEnabled(enabled: Boolean) {
        _isAutoPlayNextEnabled.value = enabled
    }

    fun setAutoResumeEnabled(enabled: Boolean) {
        _isAutoResumeEnabled.value = enabled
    }

    fun setAutoHideControlsDelaySeconds(seconds: Int) {
        _autoHideControlsDelaySeconds.value = seconds.coerceIn(1, 15)
    }

    fun setKeepScreenOnEnabled(enabled: Boolean) {
        _isKeepScreenOnEnabled.value = enabled
    }

    fun playVideo(video: VideoEntity) {
        _currentScreen.value = PlayerScreenType.PLAYER
        selectVideo(video)
        exoPlayer.play()
        _isPlaying.value = true
    }

    fun renameVideo(id: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameVideo(id, newTitle)
            if (_currentVideo.value?.id == id) {
                _currentVideo.value = _currentVideo.value?.copy(title = newTitle)
            }
        }
    }

    fun scanDeviceStorage() {
        viewModelScope.launch {
            _isScanningStorage.value = true
            val added = repository.scanDeviceStorage()
            _isScanningStorage.value = false
            _storageScanResult.value = if (added > 0) {
                "Discovered $added new video(s) from storage"
            } else {
                "Video library is up to date"
            }
            delay(3500)
            _storageScanResult.value = null
        }
    }

    fun captureScreenshot(capturedBitmap: Bitmap? = null) {
        viewModelScope.launch {
            try {
                val video = _currentVideo.value
                val title = video?.title ?: "Video"
                var bitmap = capturedBitmap

                if (bitmap == null && video != null) {
                    withContext(Dispatchers.IO) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            val uri = Uri.parse(video.uriString)
                            if (video.uriString.startsWith("android.resource://") || video.uriString.startsWith("content://")) {
                                retriever.setDataSource(getApplication(), uri)
                            } else {
                                retriever.setDataSource(video.uriString)
                            }
                            val timeUs = (_playbackPositionMs.value * 1000L).coerceAtLeast(0L)
                            bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        } catch (e: Exception) {
                            Log.w("PlayerViewModel", "MediaMetadataRetriever frame extraction failed: ${e.message}")
                        } finally {
                            try { retriever.release() } catch (_: Exception) {}
                        }
                    }
                }

                if (bitmap == null) {
                    bitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888).apply {
                        val canvas = android.graphics.Canvas(this)
                        canvas.drawColor(android.graphics.Color.DKGRAY)
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 28f
                            isAntiAlias = true
                        }
                        canvas.drawText("Video Player by Tahmid: $title", 40f, 180f, paint)
                    }
                }

                val savedName = ScreenshotHelper.saveBitmapToMediaStore(
                    context = getApplication(),
                    bitmap = bitmap!!,
                    videoTitle = title
                )

                if (savedName != null) {
                    performHapticFeedback()
                    _screenshotSuccess.value = savedName
                    viewModelScope.launch {
                        delay(3500)
                        if (_screenshotSuccess.value == savedName) {
                            _screenshotSuccess.value = null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Screenshot capture failed: ${e.message}", e)
            }
        }
    }

    private fun performHapticFeedback() {
        if (!_isHapticsEnabled.value) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15)
            }
        } catch (_: Exception) {}
    }

    fun onDoubleTapSeek(direction: SeekDirection): Long {
        val currentPos = exoPlayer.currentPosition
        val totalDuration = exoPlayer.duration
        val seekSec = _doubleTapSeekSeconds.value
        val delta = if (direction == SeekDirection.FORWARD) seekSec * 1000L else -seekSec * 1000L
        val newPos = (currentPos + delta).coerceIn(0L, totalDuration.coerceAtLeast(0L))
        exoPlayer.seekTo(newPos)
        _playbackPositionMs.value = newPos
        performHapticFeedback()

        _seekFeedback.value = SeekFeedback(direction, seekSec)
        dismissSeekFeedbackJob?.cancel()
        dismissSeekFeedbackJob = viewModelScope.launch {
            delay(800)
            _seekFeedback.value = null
        }
        return newPos
    }

    fun onDoubleTapSeek(direction: SeekDirection, currentPos: Long, totalDuration: Long): Long {
        return onDoubleTapSeek(direction)
    }

    fun adjustBrightness(delta: Float, windowManager: WindowManager?, windowAttributes: WindowManager.LayoutParams?) {
        if (_isScreenLocked.value) return
        val effectiveDelta = delta * _gestureSensitivity.value
        val current = windowAttributes?.screenBrightness ?: 0.5f
        val effectiveCurrent = if (current < 0f) 0.5f else current
        val newBrightness = (effectiveCurrent + effectiveDelta).coerceIn(0.01f, 1.0f)
        windowAttributes?.screenBrightness = newBrightness
        _brightnessPercent.value = newBrightness
        performHapticFeedback()

        scheduleDismissHud()
    }

    fun adjustVolume(delta: Float, audioManager: AudioManager) {
        if (_isScreenLocked.value) return
        val effectiveDelta = delta * _gestureSensitivity.value
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val currentRatio = currentVolume.toFloat() / maxVolume.toFloat()

        val newRatio = (currentRatio + effectiveDelta).coerceIn(0.0f, 1.0f)
        val newVolume = (newRatio * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        _volumePercent.value = newRatio
        performHapticFeedback()

        scheduleDismissHud()
    }

    private fun scheduleDismissHud() {
        dismissHudJob?.cancel()
        dismissHudJob = viewModelScope.launch {
            delay(1200)
            _brightnessPercent.value = null
            _volumePercent.value = null
        }
    }

    fun playNextVideo(loopAround: Boolean = false): VideoEntity? {
        val list = videos.value
        if (list.isEmpty()) return null
        val current = _currentVideo.value ?: return list.first().also { selectVideo(it) }
        val currentIndex = list.indexOfFirst { it.id == current.id }

        val nextIndex = if (_isShuffleEnabled.value) {
            val remaining = list.indices.filter { it != currentIndex }
            if (remaining.isNotEmpty()) remaining.random() else currentIndex
        } else {
            if (currentIndex + 1 < list.size) {
                currentIndex + 1
            } else if (loopAround || _repeatMode.value == RepeatMode.ALL) {
                0
            } else {
                return null
            }
        }

        val next = list[nextIndex]
        selectVideo(next)
        return next
    }

    fun playPreviousVideo(): VideoEntity? {
        val list = videos.value
        if (list.isEmpty()) return null
        val current = _currentVideo.value ?: return list.first().also { selectVideo(it) }
        val currentIndex = list.indexOfFirst { it.id == current.id }

        val prevIndex = if (_isShuffleEnabled.value) {
            val remaining = list.indices.filter { it != currentIndex }
            if (remaining.isNotEmpty()) remaining.random() else currentIndex
        } else {
            if (currentIndex <= 0) list.size - 1 else currentIndex - 1
        }

        val prev = list[prevIndex]
        selectVideo(prev)
        return prev
    }

    fun addImportedVideo(uri: Uri) {
        addImportedVideos(listOf(uri), autoPlayFirst = true)
    }

    fun addImportedVideos(uris: List<Uri>, autoPlayFirst: Boolean = true) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isScanningStorage.value = true
            val context = getApplication<Application>()
            val currentMaxOrder = videos.value.maxOfOrNull { it.orderIndex } ?: 0
            val importedList = withContext(Dispatchers.IO) {
                val results = mutableListOf<VideoEntity>()
                for ((index, uri) in uris.withIndex()) {
                    try {
                        // Persist URI read permission across device reboots
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                            // Some content providers / temporary URIs don't support persistable permissions
                        }

                        var fileName = uri.lastPathSegment ?: "Imported Video"
                        try {
                            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1 && cursor.moveToFirst()) {
                                    val name = cursor.getString(nameIndex)
                                    if (!name.isNullOrBlank()) {
                                        fileName = name
                                    }
                                }
                            }
                        } catch (_: Exception) {}

                        var durationMs = 0L
                        var resolution = "Local Video"
                        try {
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(context, uri)
                                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                durationMs = durStr?.toLongOrNull() ?: 0L

                                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                                if (width != null && height != null && width > 0 && height > 0) {
                                    resolution = when {
                                        height >= 2160 || width >= 3840 -> "4K UHD"
                                        height >= 1440 || width >= 2560 -> "2K QHD"
                                        height >= 1080 || width >= 1920 -> "1080p FHD"
                                        height >= 720 || width >= 1280 -> "720p HD"
                                        height >= 480 -> "480p SD"
                                        else -> "${width}x${height}"
                                    }
                                }
                            } finally {
                                try { retriever.release() } catch (_: Exception) {}
                            }
                        } catch (_: Exception) {}

                        val newVideo = VideoEntity(
                            id = uri.toString(),
                            title = fileName,
                            uriString = uri.toString(),
                            durationMs = durationMs,
                            resolution = resolution,
                            subtitleUri = null,
                            lastPlayedTimestamp = System.currentTimeMillis(),
                            orderIndex = currentMaxOrder + index + 1
                        )
                        repository.addVideo(newVideo)
                        results.add(newVideo)
                    } catch (e: Exception) {
                        Log.e("PlayerViewModel", "Failed to import video from URI: $uri", e)
                    }
                }
                results
            }

            _isScanningStorage.value = false
            if (importedList.isNotEmpty()) {
                _storageScanResult.value = if (importedList.size == 1) {
                    "Imported: ${importedList[0].title}"
                } else {
                    "Imported ${importedList.size} video files from storage"
                }
                if (autoPlayFirst) {
                    selectVideo(importedList.first())
                }
            } else {
                _storageScanResult.value = "No video files were imported"
            }
            delay(3500)
            _storageScanResult.value = null
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
            if (_currentVideo.value?.id == id) {
                _currentVideo.value = _currentVideo.value?.copy(
                    isFavorite = !(_currentVideo.value?.isFavorite ?: false)
                )
            }
        }
    }

    fun deleteVideo(id: String) {
        viewModelScope.launch {
            repository.deleteVideo(id)
            if (_currentVideo.value?.id == id) {
                val remaining = videos.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    selectVideo(remaining.first())
                } else {
                    _currentVideo.value = null
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        saveCurrentPlaybackPosition()
        exoPlayer.stop()
        exoPlayer.release()
    }

    companion object {
        val AVAILABLE_SPEEDS = listOf(0.5f, 0.8f, 1.0f, 1.5f, 1.75f, 1.85f, 2.0f)
    }
}
