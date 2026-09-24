package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.VideoEntity
import com.example.ui.PlayerScreenType
import com.example.ui.PlayerViewModel
import com.example.ui.components.LocalFilePickerBottomSheet
import com.example.ui.theme.YouTubeBlue
import com.example.ui.theme.YouTubeDarkBorder
import com.example.ui.theme.YouTubeDarkCard
import com.example.ui.theme.YouTubeRed
import com.example.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanningStorage.collectAsStateWithLifecycle()
    val scanResult by viewModel.storageScanResult.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var videoToRename by remember { mutableStateOf<VideoEntity?>(null) }
    var videoToDelete by remember { mutableStateOf<VideoEntity?>(null) }
    var videoForDetails by remember { mutableStateOf<VideoEntity?>(null) }
    var showFilePickerSheet by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImportedVideo(it) }
    }

    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isBlank()) {
            videos
        } else {
            videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search videos...", color = Color.Gray, fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_field")
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(YouTubeRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    // Search toggle button
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        modifier = Modifier.testTag("search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Pick Video Files Button
                    IconButton(
                        onClick = { showFilePickerSheet = true },
                        modifier = Modifier.testTag("top_bar_pick_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFolderUpload,
                            contentDescription = "Select Local Video Files",
                            tint = YouTubeRed
                        )
                    }

                    // Scan Storage button
                    IconButton(
                        onClick = { viewModel.scanDeviceStorage() },
                        enabled = !isScanning,
                        modifier = Modifier.testTag("scan_storage_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Device Storage",
                            tint = if (isScanning) YouTubeBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Grid / List View Switcher
                    IconButton(
                        onClick = { viewModel.toggleGridView() },
                        modifier = Modifier.testTag("grid_list_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Settings navigation button
                    IconButton(
                        onClick = { viewModel.navigateTo(PlayerScreenType.SETTINGS) },
                        modifier = Modifier.testTag("settings_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFilePickerSheet = true },
                containerColor = YouTubeRed,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("import_video_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Select Local Video Files"
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("home_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { viewModel.navigateTo(PlayerScreenType.HOME) },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Videos") },
                    label = { Text("Videos") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = YouTubeRed,
                        selectedTextColor = YouTubeRed,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(PlayerScreenType.FOLDERS) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Folders") },
                    label = { Text("Folders") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_folders")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(PlayerScreenType.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Scan feedback notification
                AnimatedVisibility(
                    visible = scanResult != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("scan_result_banner")
                    ) {
                        Text(
                            text = scanResult ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = YouTubeBlue,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (filteredVideos.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching videos found" else "No videos in library",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Scan device storage or import a video to start playing",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showFilePickerSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.testTag("empty_state_select_videos_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Select Videos",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.scanDeviceStorage() },
                                    enabled = !isScanning,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.testTag("empty_state_scan_storage_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isScanning) "Scanning..." else "Scan Storage",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                } else if (isGridView) {
                    // 2-Column Adaptive Grid Layout
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("video_lazy_vertical_grid")
                    ) {
                        items(filteredVideos, key = { it.id }) { video ->
                            VideoGridCard(
                                video = video,
                                onClick = { viewModel.playVideo(video) },
                                onRename = { videoToRename = video },
                                onDelete = { videoToDelete = video },
                                onDetails = { videoForDetails = video }
                            )
                        }
                    }
                } else {
                    // Linear List Layout
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("video_lazy_column_list")
                    ) {
                        items(filteredVideos, key = { it.id }) { video ->
                            VideoListCard(
                                video = video,
                                onClick = { viewModel.playVideo(video) },
                                onRename = { videoToRename = video },
                                onDelete = { videoToDelete = video },
                                onDetails = { videoForDetails = video }
                            )
                        }
                    }
                }
            }

            // Rename Video Dialog
            videoToRename?.let { video ->
                var newTitle by remember { mutableStateOf(video.title) }
                AlertDialog(
                    onDismissRequest = { videoToRename = null },
                    title = { Text("Rename Video") },
                    text = {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rename_title_input")
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newTitle.isNotBlank()) {
                                    viewModel.renameVideo(video.id, newTitle.trim())
                                }
                                videoToRename = null
                            },
                            modifier = Modifier.testTag("rename_confirm_button")
                        ) {
                            Text("Save", color = YouTubeBlue, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { videoToRename = null }) {
                            Text("Cancel")
                        }
                    },
                    modifier = Modifier.testTag("rename_dialog")
                )
            }

            // Delete Confirmation Dialog
            videoToDelete?.let { video ->
                AlertDialog(
                    onDismissRequest = { videoToDelete = null },
                    title = { Text("Delete Video") },
                    text = { Text("Are you sure you want to remove \"${video.title}\" from your library?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteVideo(video.id)
                                videoToDelete = null
                            },
                            modifier = Modifier.testTag("delete_confirm_button")
                        ) {
                            Text("Delete", color = YouTubeRed, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { videoToDelete = null }) {
                            Text("Cancel")
                        }
                    },
                    modifier = Modifier.testTag("delete_confirm_dialog")
                )
            }

            // Video Details Dialog
            videoForDetails?.let { video ->
                AlertDialog(
                    onDismissRequest = { videoForDetails = null },
                    title = { Text("Video Details") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Title: ${video.title}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Duration: ${TimeUtils.formatDuration(video.durationMs)}", fontSize = 13.sp)
                            Text(text = "Last Watched: ${TimeUtils.formatDuration(video.lastPositionMs)}", fontSize = 13.sp)
                            Text(text = "Quality: ${video.resolution}", fontSize = 13.sp)
                            Text(text = "Path: ${video.uriString}", fontSize = 11.sp, color = Color.Gray, maxLines = 2)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { videoForDetails = null }) {
                            Text("Close", color = YouTubeBlue)
                        }
                    }
                )
            }

            // Local File Picker Bottom Sheet
            if (showFilePickerSheet) {
                LocalFilePickerBottomSheet(
                    onVideosSelected = { uris ->
                        viewModel.addImportedVideos(uris, autoPlayFirst = true)
                    },
                    onScanStorage = {
                        viewModel.scanDeviceStorage()
                    },
                    onDismiss = { showFilePickerSheet = false },
                    isImporting = isScanning
                )
            }
        }
    }
}

@Composable
fun VideoGridCard(
    video: VideoEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val progress = if (video.durationMs > 0L) {
        (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val formatTag = remember(video.uriString, video.title) {
        getVideoFormatTag(video.uriString, video.title)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("video_grid_card_${video.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail Area (16:9 aspect ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF2C2C38), Color(0xFF16161E))
                        )
                    )
            ) {
                // Async image thumbnail attempt, with graceful fallback
                AsyncImage(
                    model = video.uriString,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Central Play Icon Indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Format Tag (Pill Badge in Top-Right)
                Surface(
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .testTag("format_tag_${video.id}")
                ) {
                    Text(
                        text = formatTag,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Duration Badge (Bottom-Right)
                Surface(
                    color = Color(0xDD000000),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 6.dp, end = 6.dp)
                        .testTag("duration_badge_${video.id}")
                ) {
                    Text(
                        text = TimeUtils.formatDuration(video.durationMs),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Linear Resume Playback Progress Bar (at very bottom of thumbnail)
                if (progress > 0.02f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                            .testTag("progress_bar_${video.id}"),
                        color = YouTubeRed,
                        trackColor = Color(0x44FFFFFF)
                    )
                }
            }

            // Title and Overflow Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (video.lastPositionMs > 1000L) {
                            "Watched ${TimeUtils.formatDuration(video.lastPositionMs)}"
                        } else {
                            video.resolution
                        },
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("video_menu_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Details") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = YouTubeRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = YouTubeRed) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoListCard(
    video: VideoEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val progress = if (video.durationMs > 0L) {
        (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val formatTag = remember(video.uriString, video.title) {
        getVideoFormatTag(video.uriString, video.title)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("video_list_card_${video.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF22222E))
            ) {
                AsyncImage(
                    model = video.uriString,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Format Tag
                Surface(
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                ) {
                    Text(
                        text = formatTag,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }

                // Duration Badge
                Surface(
                    color = Color(0xDD000000),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                ) {
                    Text(
                        text = TimeUtils.formatDuration(video.durationMs),
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }

                // Resume progress bar
                if (progress > 0.02f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomCenter),
                        color = YouTubeRed,
                        trackColor = Color(0x44FFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${video.resolution} • ${if (video.lastPositionMs > 1000L) "Resumed at ${TimeUtils.formatDuration(video.lastPositionMs)}" else "Unwatched"}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("video_list_menu_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play") },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Details") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDetails()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = YouTubeRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = YouTubeRed) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

fun getVideoFormatTag(uriString: String, title: String): String {
    val combined = "$title $uriString".lowercase()
    return when {
        combined.contains(".mp4") -> "MP4"
        combined.contains(".mkv") -> "MKV"
        combined.contains(".webm") -> "WEBM"
        combined.contains(".mov") -> "MOV"
        combined.contains(".avi") -> "AVI"
        combined.contains(".3gp") -> "3GP"
        combined.contains("sample_") -> "RAW"
        else -> "VIDEO"
    }
}
