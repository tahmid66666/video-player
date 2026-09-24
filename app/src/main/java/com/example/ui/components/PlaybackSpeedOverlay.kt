package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PlayerViewModel
import com.example.ui.theme.YouTubeDarkBorder
import com.example.ui.theme.YouTubeDarkCard
import com.example.ui.theme.YouTubeOverlayScrim
import com.example.ui.theme.YouTubeRed

/**
 * Playback Speed Control Overlay for ExoPlayer.
 *
 * Allows users to adjust playback speed between:
 * 0.5x, 0.8x, 1.0x, 1.5x, 1.75x, 1.85x, and 2.0x
 * using ExoPlayer playback parameters.
 */
@Composable
fun PlaybackSpeedOverlay(
    isVisible: Boolean,
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onSpeedChangedLive: (Float) -> Unit = onSpeedSelected,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier.fillMaxSize()
    ) {
        // Scrim background: tap outside to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(YouTubeOverlayScrim)
                .clickable { onDismiss() }
                .testTag("playback_speed_control_overlay"),
            contentAlignment = Alignment.Center
        ) {
            // Speed card container: consume clicks so tapping inside does not dismiss
            Surface(
                color = YouTubeDarkCard,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YouTubeDarkBorder),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth(0.92f)
                    .clickable(enabled = false) {}
                    .testTag("speed_overlay_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header: Icon, Title, Current Speed Badge, Close button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FF0000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed Icon",
                                tint = YouTubeRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Playback Speed",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ExoPlayer PlaybackParameters",
                                color = Color(0xFFAAAAAA),
                                fontSize = 11.sp
                            )
                        }

                        // Current speed pill badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(YouTubeRed)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("speed_current_badge")
                        ) {
                            Text(
                                text = "${formatSpeed(currentSpeed)}x",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_speed_overlay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close speed overlay",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = YouTubeDarkBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset chips for 0.5x, 0.8x, 1.0x, 1.5x, 1.75x, 1.85x, 2.0x
                    Text(
                        text = "SPEED PRESETS",
                        color = Color(0xFF888888),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row of quick preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlayerViewModel.AVAILABLE_SPEEDS.forEach { speed ->
                            val isSelected = Math.abs(currentSpeed - speed) < 0.02f
                            val tag = "speed_preset_${speed.toString().replace('.', '_')}x"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) YouTubeRed else Color(0x22FFFFFF))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) YouTubeRed else YouTubeDarkBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSpeedSelected(speed) }
                                    .padding(vertical = 8.dp)
                                    .testTag(tag),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${formatSpeed(speed)}x",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Interactive Stepper & Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                val prev = PlayerViewModel.AVAILABLE_SPEEDS.reversed()
                                    .find { it < currentSpeed - 0.02f }
                                    ?: PlayerViewModel.AVAILABLE_SPEEDS.first()
                                onSpeedSelected(prev)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .testTag("speed_decrease_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Speed",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Slider(
                            value = currentSpeed.coerceIn(0.5f, 2.0f),
                            onValueChange = { newValue ->
                                // Snap to nearest requested speed or live update
                                val nearest = PlayerViewModel.AVAILABLE_SPEEDS.minByOrNull {
                                    Math.abs(it - newValue)
                                } ?: newValue
                                onSpeedChangedLive(nearest)
                            },
                            valueRange = 0.5f..2.0f,
                            steps = 5, // 7 points total: 0.5, 0.8, 1.0, 1.5, 1.75, 1.85, 2.0
                            colors = SliderDefaults.colors(
                                thumbColor = YouTubeRed,
                                activeTrackColor = YouTubeRed,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                                .testTag("speed_slider")
                        )

                        IconButton(
                            onClick = {
                                val next = PlayerViewModel.AVAILABLE_SPEEDS
                                    .find { it > currentSpeed + 0.02f }
                                    ?: PlayerViewModel.AVAILABLE_SPEEDS.last()
                                onSpeedSelected(next)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .testTag("speed_increase_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Speed",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = YouTubeDarkBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Detailed speed list items with radio selection and checkmarks
                    val speedLabels = mapOf(
                        0.5f to "0.5x (Slow Motion)",
                        0.8f to "0.8x (Slightly Slower)",
                        1.0f to "1.0x (Normal Speed)",
                        1.5f to "1.5x (Faster)",
                        1.75f to "1.75x (Brisk)",
                        1.85f to "1.85x (High Speed)",
                        2.0f to "2.0x (Double Speed)"
                    )

                    PlayerViewModel.AVAILABLE_SPEEDS.forEach { speed ->
                        val isSelected = Math.abs(currentSpeed - speed) < 0.02f
                        val label = speedLabels[speed] ?: "${formatSpeed(speed)}x"
                        val tag = "speed_option_${speed.toString().replace('.', '_')}x"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0x22FF0000) else Color.Transparent)
                                .clickable { onSpeedSelected(speed) }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag(tag),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) YouTubeRed else Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(YouTubeRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSpeed(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}"
    } else {
        String.format(java.util.Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')
    }
}
