package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.ui.PlayerViewModel
import com.example.ui.SeekDirection
import com.example.ui.theme.YouTubeBlue
import com.example.ui.theme.YouTubeRed

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    viewModel: PlayerViewModel,
    onOrientationToggle: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val currentVideo by viewModel.currentVideo.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val bufferedPositionMs by viewModel.bufferedPositionMs.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val isControlsVisible by viewModel.isControlsVisible.collectAsStateWithLifecycle()
    val isScreenLocked by viewModel.isScreenLocked.collectAsStateWithLifecycle()
    val isSubtitlesEnabled by viewModel.isSubtitlesEnabled.collectAsStateWithLifecycle()
    val currentSubtitleText by viewModel.currentSubtitleText.collectAsStateWithLifecycle()
    val brightnessPercent by viewModel.brightnessPercent.collectAsStateWithLifecycle()
    val volumePercent by viewModel.volumePercent.collectAsStateWithLifecycle()
    val seekFeedback by viewModel.seekFeedback.collectAsStateWithLifecycle()
    val isLandscape by viewModel.isLandscape.collectAsStateWithLifecycle()
    val resumeNotification by viewModel.resumeNotification.collectAsStateWithLifecycle()
    val screenshotSuccess by viewModel.screenshotSuccess.collectAsStateWithLifecycle()
    val doubleTapSeekSeconds by viewModel.doubleTapSeekSeconds.collectAsStateWithLifecycle()
    val isKeepScreenOnEnabled by viewModel.isKeepScreenOnEnabled.collectAsStateWithLifecycle()

    val exoPlayer = viewModel.exoPlayer
    var dragStartX by remember { mutableFloatStateOf(0f) }

    DisposableEffect(exoPlayer) {
        onDispose {
            if (!viewModel.isBackgroundPlaybackEnabled.value) {
                viewModel.pausePlayback()
                viewModel.saveCurrentPlaybackPosition()
            }
            exoPlayer.clearVideoSurface()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .testTag("video_player_container")
            // Tap gestures: Double-tap seek & single tap toggle overlay
            .pointerInput(isScreenLocked) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isScreenLocked) {
                            val width = size.width
                            if (offset.x < width * 0.35f) {
                                viewModel.onDoubleTapSeek(SeekDirection.BACKWARD)
                            } else if (offset.x > width * 0.65f) {
                                viewModel.onDoubleTapSeek(SeekDirection.FORWARD)
                            } else {
                                viewModel.togglePlayPause()
                            }
                        }
                    },
                    onTap = {
                        viewModel.toggleControls()
                    }
                )
            }
            // Vertical Drag gestures: Brightness (left side) and Volume (right side)
            .pointerInput(isScreenLocked) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartX = offset.x
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!isScreenLocked && size.height > 0) {
                            val deltaFraction = -dragAmount.y / (size.height * 0.65f)
                            if (dragStartX < size.width * 0.5f) {
                                // Left side: Brightness
                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    viewModel.adjustBrightness(deltaFraction, act.windowManager, lp)
                                    act.window.attributes = lp
                                }
                            } else {
                                // Right side: Volume
                                viewModel.adjustVolume(deltaFraction, audioManager)
                            }
                        }
                    }
                )
            }
    ) {
        // ExoPlayer Native Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = exoPlayer
                    this.useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
                playerView.keepScreenOn = isKeepScreenOnEnabled && isPlaying
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtitles Overlay
        SubtitleOverlay(
            text = if (isSubtitlesEnabled) currentSubtitleText else null,
            isControlsVisible = isControlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // YouTube Controls Overlay
        if (!isScreenLocked) {
            YouTubeOverlay(
                isVisible = isControlsVisible,
                isPlaying = isPlaying,
                title = currentVideo?.title ?: "Offline Video",
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                playbackSpeed = playbackSpeed,
                repeatMode = repeatMode,
                isShuffleEnabled = isShuffleEnabled,
                isSubtitlesEnabled = isSubtitlesEnabled,
                isLandscape = isLandscape,
                seekSeconds = doubleTapSeekSeconds,
                onPlayPauseClick = {
                    viewModel.togglePlayPause()
                },
                onRewind10 = {
                    viewModel.onDoubleTapSeek(SeekDirection.BACKWARD)
                },
                onForward10 = {
                    viewModel.onDoubleTapSeek(SeekDirection.FORWARD)
                },
                onPreviousClick = {
                    viewModel.playPreviousVideo()
                },
                onNextClick = {
                    viewModel.playNextVideo()
                },
                onSeekStarted = {
                    viewModel.cancelAutoHide()
                },
                onSeekChanged = { seekMs ->
                    viewModel.seekTo(seekMs)
                },
                onSeekFinished = { finalMs ->
                    viewModel.seekTo(finalMs)
                    viewModel.showControls()
                },
                onSpeedClick = {
                    viewModel.openSpeedOverlay()
                },
                onSubtitlesToggle = {
                    viewModel.toggleSubtitles()
                },
                onRepeatToggle = {
                    viewModel.toggleRepeatMode()
                },
                onShuffleToggle = {
                    viewModel.toggleShuffle()
                },
                onLockClick = {
                    viewModel.toggleScreenLock()
                },
                onPipClick = onPipClick,
                onOrientationToggle = onOrientationToggle,
                onDetailsClick = {
                    viewModel.openDetailsDialog()
                },
                onScreenshotClick = {
                    viewModel.captureScreenshot()
                },
                onManualRotateClick = onOrientationToggle,
                onCollapseClick = {
                    viewModel.navigateTo(com.example.ui.PlayerScreenType.HOME)
                }
            )
        }

        // Screen Lock Overlay (when screen is locked)
        ScreenLockOverlay(
            isLocked = isScreenLocked,
            isPromptVisible = isControlsVisible,
            onUnlock = { viewModel.unlockScreen() }
        )

        // Double Tap Ripple Seek Feedback (YouTube style +10s / -10s animation)
        DoubleTapSeekFeedback(
            seekFeedback = seekFeedback,
            modifier = Modifier.fillMaxSize()
        )

        // Brightness & Volume Floating HUD
        GestureHud(
            brightnessPercent = brightnessPercent,
            volumePercent = volumePercent,
            modifier = Modifier.align(Alignment.Center)
        )

        // Floating Auto-Resume Pill ("Resumed from MM:SS • Start Over")
        AnimatedVisibility(
            visible = resumeNotification != null && !isScreenLocked,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Surface(
                color = Color(0xEE1E1E28),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.testTag("resume_notification_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Resume progress",
                        tint = YouTubeRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = resumeNotification ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Start Over",
                        color = YouTubeBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                viewModel.restartCurrentVideo()
                            }
                            .testTag("resume_restart_button")
                    )
                }
            }
        }

        // Floating Screenshot Saved Banner
        ScreenshotSavedBanner(
            screenshotName = screenshotSuccess,
            onDismiss = { viewModel.clearScreenshotSuccess() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (resumeNotification != null) 64.dp else 16.dp)
        )
    }
}
