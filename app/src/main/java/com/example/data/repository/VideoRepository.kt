package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.dao.VideoDao
import com.example.data.model.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VideoRepository(
    private val context: Context,
    private val videoDao: VideoDao
) {
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()

    suspend fun initializeDefaultVideosIfEmpty() = withContext(Dispatchers.IO) {
        // Automatically purge any remaining demo/sample videos so library starts clean
        videoDao.deleteDemoVideos()
    }

    suspend fun purgeDemoVideos() = withContext(Dispatchers.IO) {
        videoDao.deleteDemoVideos()
    }

    suspend fun addVideo(video: VideoEntity) = withContext(Dispatchers.IO) {
        videoDao.insertVideo(video)
    }

    suspend fun getVideoById(id: String): VideoEntity? = withContext(Dispatchers.IO) {
        videoDao.getVideoById(id)
    }

    suspend fun getLastPlaybackPosition(id: String): Long = withContext(Dispatchers.IO) {
        videoDao.getLastPlaybackPosition(id) ?: 0L
    }

    suspend fun savePlaybackPosition(id: String, positionMs: Long, durationMs: Long = 0L) = withContext(Dispatchers.IO) {
        videoDao.updatePlaybackPosition(id, positionMs, System.currentTimeMillis())
        if (durationMs > 0L) {
            videoDao.updateDuration(id, durationMs)
        }
    }

    suspend fun resetPlaybackPosition(id: String) = withContext(Dispatchers.IO) {
        videoDao.updatePlaybackPosition(id, 0L, System.currentTimeMillis())
    }

    suspend fun updatePlaybackPosition(id: String, positionMs: Long) = withContext(Dispatchers.IO) {
        videoDao.updatePlaybackPosition(id, positionMs, System.currentTimeMillis())
    }

    suspend fun updateDuration(id: String, durationMs: Long) = withContext(Dispatchers.IO) {
        videoDao.updateDuration(id, durationMs)
    }

    suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        videoDao.toggleFavorite(id)
    }

    suspend fun renameVideo(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        videoDao.updateTitle(id, newTitle)
    }

    suspend fun scanDeviceStorage(): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
            )
            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )
            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (it.moveToNext()) {
                    val mediaId = it.getLong(idCol)
                    val contentUri = Uri.withAppendedPath(uri, mediaId.toString()).toString()
                    val title = it.getString(titleCol) ?: it.getString(nameCol) ?: "Video $mediaId"
                    val duration = it.getLong(durCol)

                    val existing = videoDao.getVideoById(contentUri)
                    if (existing == null) {
                        val entity = VideoEntity(
                            id = contentUri,
                            title = title,
                            uriString = contentUri,
                            durationMs = duration,
                            resolution = "Local Storage",
                            orderIndex = 999
                        )
                        videoDao.insertVideo(entity)
                        addedCount++
                    }
                }
            }
        } catch (e: Exception) {
            // Handled gracefully if no media or permission not yet granted
        }
        addedCount
    }

    suspend fun deleteVideo(id: String) = withContext(Dispatchers.IO) {
        videoDao.deleteVideo(id)
    }
}
