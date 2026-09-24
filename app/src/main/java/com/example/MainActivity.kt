package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PlayerScreen
import com.example.ui.PlayerViewModel
import com.example.ui.ThemeMode
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by playerViewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayerScreen(viewModel = playerViewModel)
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            playerViewModel.cancelAutoHide()
        } else {
            playerViewModel.showControls()
        }
    }

    override fun onPause() {
        super.onPause()
        playerViewModel.saveCurrentPlaybackPosition()
        if (!playerViewModel.isBackgroundPlaybackEnabled.value) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                if (!isInPictureInPictureMode) {
                    playerViewModel.pausePlayback()
                }
            } else {
                playerViewModel.pausePlayback()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        playerViewModel.saveCurrentPlaybackPosition()
        if (!playerViewModel.isBackgroundPlaybackEnabled.value) {
            playerViewModel.pausePlayback()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If playing, user pressing Home button can enter PiP mode smoothly
        if (playerViewModel.isPlaying.value && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
