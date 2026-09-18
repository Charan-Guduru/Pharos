package com.vnrvjiet.attendancemonitor.data.repository

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.vnrvjiet.attendancemonitor.BuildConfig
import com.vnrvjiet.attendancemonitor.MainActivity
import com.vnrvjiet.attendancemonitor.R
import com.vnrvjiet.attendancemonitor.data.model.GitHubRelease
import com.vnrvjiet.attendancemonitor.data.model.UpdateManifest
import com.vnrvjiet.attendancemonitor.data.remote.UpdateApi
import com.vnrvjiet.attendancemonitor.data.repository.SettingsRepository
import com.vnrvjiet.attendancemonitor.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val manifest: UpdateManifest, val isManual: Boolean) : UpdateState()
    object NotAvailable : UpdateState()
    data class Error(val message: String) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class ReadyToInstall(val uri: Uri, val manifest: UpdateManifest) : UpdateState()
    object Installing : UpdateState()
    data class DownloadFailed(val message: String) : UpdateState()
}

class UpdateRepository private constructor(private val context: Context) {

    private val TAG = "UpdateRepository"

    companion object {
        @Volatile
        private var INSTANCE: UpdateRepository? = null

        fun getInstance(context: Context): UpdateRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UpdateRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val api: UpdateApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateApi::class.java)
    }

    suspend fun checkForUpdate(manualCheck: Boolean = false) {
        if (_updateState.value is UpdateState.Checking || _updateState.value is UpdateState.Downloading) {
            return
        }

        if (manualCheck) {
            _updateState.value = UpdateState.Checking
        }
        
        try {
            val response = api.getLatestRelease(BuildConfig.GITHUB_OWNER, BuildConfig.GITHUB_REPO)
            if (response.isSuccessful) {
                val release = response.body()
                if (release != null) {
                    val manifest = parseGitHubRelease(release)
                    if (manifest != null) {
                        val currentVersionCode = BuildConfig.VERSION_CODE
                        if (manifest.versionCode > currentVersionCode) {
                            if (manualCheck) {
                                _updateState.value = UpdateState.Available(manifest, true)
                            } else {
                                handleAutomaticUpdateDetection(manifest)
                            }
                        } else {
                            if (manualCheck) {
                                _updateState.value = UpdateState.NotAvailable
                            }
                        }
                    } else {
                        if (manualCheck) _updateState.value = UpdateState.Error("Invalid release information")
                    }
                } else {
                    if (manualCheck) _updateState.value = UpdateState.Error("No release found")
                }
            } else {
                if (manualCheck) _updateState.value = UpdateState.Error("GitHub API Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            if (manualCheck) {
                _updateState.value = UpdateState.Error("Unable to check for updates. Please try again later.")
            }
        }
    }

    private fun parseGitHubRelease(release: GitHubRelease): UpdateManifest? {
        // 1. Extract versionCode from name or tag
        // Regex looks for numbers in parentheses, e.g. "Pharos v1.1.0 (12)" -> 12
        val codeRegex = "\\((\\d+)\\)".toRegex()
        val codeMatch = codeRegex.find(release.name) ?: codeRegex.find(release.tagName)
        val remoteVersionCode = codeMatch?.groupValues?.get(1)?.toIntOrNull() 

        // 2. Find APK asset
        val apkAsset = release.assets.find { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return null

        val remoteVersionName = release.tagName.removePrefix("v")

        // 3. Fallback logic: If versionCode is not in title, use semantic comparison of versionName
        val finalVersionCode = if (remoteVersionCode != null) {
            remoteVersionCode
        } else {
            // If we can't find a versionCode, we compare versionNames.
            // If remote is newer semantically, we return a "fake" high versionCode
            // to trigger the update check 'manifest.versionCode > currentVersionCode'
            if (isNewerVersion(remoteVersionName, BuildConfig.VERSION_NAME)) {
                BuildConfig.VERSION_CODE + 1
            } else {
                BuildConfig.VERSION_CODE
            }
        }

        return UpdateManifest(
            versionCode = finalVersionCode,
            versionName = remoteVersionName,
            downloadUrl = apkAsset.downloadUrl,
            releaseNotes = release.body
        )
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        try {
            val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
            val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
            
            val maxLength = maxOf(remoteParts.size, localParts.size)
            for (i in 0 until maxLength) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Version comparison failed", e)
        }
        return false
    }

    private suspend fun handleAutomaticUpdateDetection(manifest: UpdateManifest) {
        val settings = SettingsRepository.getInstance(context)
        val lastPromptAt = settings.lastUpdatePromptAt.first()
        val oneMonthMillis = 30L * 24 * 60 * 60 * 1000
        
        // We only show the AUTOMATIC prompt once per month.
        if (System.currentTimeMillis() - lastPromptAt > oneMonthMillis) {
            _updateState.value = UpdateState.Available(manifest, isManual = false)
            showUpdateNotification(manifest)
            settings.setLastUpdatePromptAt(System.currentTimeMillis())
        } else {
            Log.d(TAG, "Automatic update prompt suppressed by monthly timer.")
        }
    }

    private fun showUpdateNotification(manifest: UpdateManifest) {
        // We reuse the existing notification channel for simplicity, or create a new one.
        val title = "Pharos update available"
        val message = "Version ${manifest.versionName} is ready to download."
        
        // We need a specific action to open the update dialog.
        // We can just open the MainActivity and let it handle the UpdateState
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SHOW_UPDATE_DIALOG", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, "pharos_notifications")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(2001, builder.build())
            } catch (e: SecurityException) {
                // Ignore missing POST_NOTIFICATIONS permission
            }
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    fun downloadAndInstallUpdate(manifest: UpdateManifest) {
        _updateState.value = UpdateState.Downloading(0)
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(manifest.downloadUrl)
        
        val fileName = "pharos_update_${manifest.versionName}.apk"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(uri)
            .setTitle("Downloading Pharos Update")
            .setDescription("Version ${manifest.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == id) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIndex >= 0) {
                            val status = cursor.getInt(statusIndex)
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                // Double-check file existence at our known path
                                if (file.exists()) {
                                    val contentUri = FileProvider.getUriForFile(
                                        context, 
                                        "${BuildConfig.APPLICATION_ID}.fileprovider", 
                                        file
                                    )
                                    _updateState.value = UpdateState.ReadyToInstall(contentUri, manifest)
                                    // We don't call installApk immediately to give UI a chance to transition
                                } else {
                                    _updateState.value = UpdateState.DownloadFailed("Update file missing after download")
                                }
                            } else {
                                _updateState.value = UpdateState.DownloadFailed("Download interrupted")
                            }
                        }
                        cursor.close()
                    }
                    context.unregisterReceiver(this)
                }
            }
        }
        
        // Use RECEIVER_EXPORTED for system broadcasts on API 33+
        ContextCompat.registerReceiver(
            context, 
            onComplete, 
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), 
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun installUpdate(uri: Uri) {
        _updateState.value = UpdateState.Installing
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Installation intent failed", e)
            _updateState.value = UpdateState.DownloadFailed("Could not launch package installer")
        }
    }
}
