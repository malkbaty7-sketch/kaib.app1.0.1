package com.example.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.model.SnapshotEntity
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest

class SnapshotWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val projects = db.projectDao().getAllProjects().firstOrNull() ?: emptyList()
            val recentProject = projects.maxByOrNull { it.lastModified }

            if (recentProject != null && recentProject.contentJson.isNotBlank()) {
                // Parse chapters and check if snapshots needed
                val hash = md5(recentProject.contentJson)
                val snapshot = SnapshotEntity(
                    chapterId = "proj_${recentProject.id}",
                    contentHash = hash,
                    deltaContent = recentProject.contentJson,
                    wordCount = DocumentParser.countWords(recentProject.contentJson),
                    chapterTitle = "نسخة احتياطية دورية: ${recentProject.projectName}",
                    timestamp = System.currentTimeMillis()
                )
                db.snapshotDao().insertSnapshot(snapshot)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
