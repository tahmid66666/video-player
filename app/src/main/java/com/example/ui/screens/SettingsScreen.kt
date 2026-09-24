package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PlayerViewModel
import com.example.ui.ThemeMode
import com.example.ui.theme.YouTubeBlue
import com.example.ui.theme.YouTubeRed
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val gestureSensitivity by viewModel.gestureSensitivity.collectAsStateWithLifecycle()
    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsStateWithLifecycle()
    val isBackgroundPlaybackEnabled by viewModel.isBackgroundPlaybackEnabled.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanningStorage.collectAsStateWithLifecycle()
    val storageScanResult by viewModel.storageScanResult.collectAsStateWithLifecycle()

    val doubleTapSeekSeconds by viewModel.doubleTapSeekSeconds.collectAsStateWithLifecycle()
    val defaultPlaybackSpeed by viewModel.defaultPlaybackSpeed.collectAsStateWithLifecycle()
    val isAutoPlayNextEnabled by viewModel.isAutoPlayNextEnabled.collectAsStateWithLifecycle()
    val isAutoResumeEnabled by viewModel.isAutoResumeEnabled.collectAsStateWithLifecycle()
    val autoHideControlsDelaySeconds by viewModel.autoHideControlsDelaySeconds.collectAsStateWithLifecycle()
    val isKeepScreenOnEnabled by viewModel.isKeepScreenOnEnabled.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("settings_top_app_bar")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Double-Tap Seek Interval (User Requested Option)
            SettingsSectionCard(
                title = "Double-Tap to Seek",
                icon = Icons.Default.FastForward
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seek Skip Interval",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${doubleTapSeekSeconds}s",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = YouTubeRed,
                        modifier = Modifier.testTag("double_tap_seek_label")
                    )
                }
                Text(
                    text = "Seconds skipped forward or backward when double-tapping video sides",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val intervals = listOf(5, 10, 15, 20, 30, 60)
                    intervals.forEach { sec ->
                        FilterChip(
                            selected = doubleTapSeekSeconds == sec,
                            onClick = { viewModel.setDoubleTapSeekSeconds(sec) },
                            label = { Text("${sec}s") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YouTubeRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("seek_interval_${sec}s")
                        )
                    }
                }
            }

            // 2. Playback & Autoplay Preferences
            SettingsSectionCard(
                title = "Playback Preferences",
                icon = Icons.Default.PlayCircle
            ) {
                // Default Playback Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Default Playback Speed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${defaultPlaybackSpeed}x",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = YouTubeBlue,
                        modifier = Modifier.testTag("default_speed_label")
                    )
                }
                Text(
                    text = "Starting speed for all opened videos",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val speeds = PlayerViewModel.AVAILABLE_SPEEDS
                    speeds.forEach { speed ->
                        val label = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x"
                        FilterChip(
                            selected = defaultPlaybackSpeed == speed,
                            onClick = { viewModel.setDefaultPlaybackSpeed(speed) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YouTubeBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("default_speed_${speed.toString().replace('.', '_')}x")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto-Play Next Video Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Play Next Video",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Seamlessly play the next video in playlist when finished",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isAutoPlayNextEnabled,
                        onCheckedChange = { viewModel.setAutoPlayNextEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = YouTubeRed
                        ),
                        modifier = Modifier.testTag("autoplay_next_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto-Resume Watch Progress Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Resume Watch Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automatically jump to last watched timestamp from Room database",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isAutoResumeEnabled,
                        onCheckedChange = { viewModel.setAutoResumeEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = YouTubeRed
                        ),
                        modifier = Modifier.testTag("autoresume_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Keep Screen Awake Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep Screen Awake",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Prevent screen from dimming or locking while video is playing",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isKeepScreenOnEnabled,
                        onCheckedChange = { viewModel.setKeepScreenOnEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = YouTubeRed
                        ),
                        modifier = Modifier.testTag("keep_screen_on_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Background Playback Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Audio Playback",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Continue audio stream when leaving the app or turning screen off",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isBackgroundPlaybackEnabled,
                        onCheckedChange = { viewModel.setBackgroundPlaybackEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = YouTubeRed
                        ),
                        modifier = Modifier.testTag("background_playback_switch")
                    )
                }
            }

            // 3. Gestures & Controls Display
            SettingsSectionCard(
                title = "Player Gestures & Touch",
                icon = Icons.Default.TouchApp
            ) {
                // Gesture Sensitivity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vertical Swipe Sensitivity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format(Locale.US, "%.1fx", gestureSensitivity),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = YouTubeBlue,
                        modifier = Modifier.testTag("gesture_sensitivity_label")
                    )
                }
                Text(
                    text = "Controls how fast swipes change brightness (left) and volume (right)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Slider(
                    value = gestureSensitivity,
                    onValueChange = { viewModel.setGestureSensitivity(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = YouTubeRed,
                        activeTrackColor = YouTubeRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gesture_sensitivity_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Haptic Feedback Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic Tactile Feedback",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Vibrate lightly during scrubbing and gesture changes",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isHapticsEnabled,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = YouTubeRed
                        ),
                        modifier = Modifier.testTag("haptics_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Controls Auto-Hide Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Controls Auto-Hide Timeout",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${autoHideControlsDelaySeconds}s",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = YouTubeBlue,
                        modifier = Modifier.testTag("autohide_delay_label")
                    )
                }
                Text(
                    text = "How quickly player overlay controls automatically disappear",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val timeouts = listOf(2, 3, 5, 8)
                    timeouts.forEach { timeout ->
                        FilterChip(
                            selected = autoHideControlsDelaySeconds == timeout,
                            onClick = { viewModel.setAutoHideControlsDelaySeconds(timeout) },
                            label = { Text("${timeout}s") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YouTubeBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("autohide_${timeout}s")
                        )
                    }
                }
            }

            // 4. Appearance & Theme Section
            SettingsSectionCard(
                title = "Appearance & Theme",
                icon = Icons.Default.Palette
            ) {
                Text(
                    text = "Choose visual theme for the video player interface",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        label = { Text("Dark") },
                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YouTubeRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_dark_button")
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text("Light") },
                        leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YouTubeRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_light_button")
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        label = { Text("System") },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YouTubeRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_system_button")
                    )
                }
            }

            // 5. Storage & Scanner Section
            SettingsSectionCard(
                title = "Device Storage & Scanner",
                icon = Icons.Default.Storage
            ) {
                Text(
                    text = "Automatically discover offline video files stored on your device's internal storage or SD card.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.scanDeviceStorage() },
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YouTubeRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_storage_trigger_button")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning MediaStore...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Device Storage")
                    }
                }

                if (storageScanResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = storageScanResult ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = YouTubeBlue,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // 6. About & Version Info Card
            SettingsSectionCard(
                title = "About Video Player",
                icon = Icons.Default.Info
            ) {
                Column(
                    modifier = Modifier.testTag("about_version_card"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Video Player by Tahmid",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version 1.3 (Build 4)",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = YouTubeBlue,
                        modifier = Modifier.testTag("app_version_label")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "High-performance offline video player powered by Jetpack Compose and AndroidX Media3 ExoPlayer. Features double-tap seek intervals, gesture HUD controls, floating screenshot capture, Room database watch progress, and Picture-in-Picture mode.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Licensed under Apache License 2.0 • MediaStore Pictures/VideoPlayer",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = YouTubeRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
