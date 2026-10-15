package com.example.domain.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.CloudProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudAccountState(
    val provider: CloudProvider,
    val isConnected: Boolean,
    val accountEmail: String? = null,
    val usedSpaceMb: Double = 0.0,
    val totalSpaceMb: Double = 15000.0,
    val lastSyncTime: Long? = null
)

class CloudBackupEngine(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("scanflow_cloud_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREF_AUTO_BACKUP = "pref_auto_backup"
        private const val PREF_BACKUP_WIFI_ONLY = "pref_backup_wifi_only"
        private const val PREF_LAST_BACKUP_TIME = "pref_last_backup_time"
        private const val PREF_ACTIVE_PROVIDER = "pref_active_cloud_provider"
    }

    fun isAutoBackupEnabled(): Boolean = prefs.getBoolean(PREF_AUTO_BACKUP, false)

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_BACKUP, enabled).apply()
    }

    fun isWifiOnlyEnabled(): Boolean = prefs.getBoolean(PREF_BACKUP_WIFI_ONLY, true)

    fun setWifiOnlyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_BACKUP_WIFI_ONLY, enabled).apply()
    }

    fun getLastBackupTime(): Long = prefs.getLong(PREF_LAST_BACKUP_TIME, 0L)

    fun recordBackupComplete() {
        prefs.edit().putLong(PREF_LAST_BACKUP_TIME, System.currentTimeMillis()).apply()
    }

    fun getActiveProvider(): CloudProvider {
        val name = prefs.getString(PREF_ACTIVE_PROVIDER, CloudProvider.GOOGLE_DRIVE.name)
        return try {
            CloudProvider.valueOf(name ?: CloudProvider.GOOGLE_DRIVE.name)
        } catch (e: Exception) {
            CloudProvider.GOOGLE_DRIVE
        }
    }

    fun setActiveProvider(provider: CloudProvider) {
        prefs.edit().putString(PREF_ACTIVE_PROVIDER, provider.name).apply()
    }

    fun getAccountsState(): List<CloudAccountState> {
        val active = getActiveProvider()
        val lastTime = getLastBackupTime()
        return listOf(
            CloudAccountState(
                provider = CloudProvider.GOOGLE_DRIVE,
                isConnected = active == CloudProvider.GOOGLE_DRIVE,
                accountEmail = if (active == CloudProvider.GOOGLE_DRIVE) "scanflow.user@gmail.com" else null,
                usedSpaceMb = 234.5,
                totalSpaceMb = 15000.0,
                lastSyncTime = if (active == CloudProvider.GOOGLE_DRIVE && lastTime > 0) lastTime else null
            ),
            CloudAccountState(
                provider = CloudProvider.DROPBOX,
                isConnected = active == CloudProvider.DROPBOX,
                accountEmail = if (active == CloudProvider.DROPBOX) "user@dropbox.com" else null,
                usedSpaceMb = 84.0,
                totalSpaceMb = 2000.0,
                lastSyncTime = if (active == CloudProvider.DROPBOX && lastTime > 0) lastTime else null
            ),
            CloudAccountState(
                provider = CloudProvider.ONE_DRIVE,
                isConnected = active == CloudProvider.ONE_DRIVE,
                accountEmail = if (active == CloudProvider.ONE_DRIVE) "user@outlook.com" else null,
                usedSpaceMb = 120.0,
                totalSpaceMb = 5000.0,
                lastSyncTime = if (active == CloudProvider.ONE_DRIVE && lastTime > 0) lastTime else null
            )
        )
    }

    suspend fun createFullBackupArchive(documentsCount: Int): File = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupFile = File(backupDir, "ScanFlow_Backup_$dateStr.bak")

        FileOutputStream(backupFile).use { out ->
            val metadata = "SCANFLOW_BACKUP_V1\nDATE=${System.currentTimeMillis()}\nDOCS=$documentsCount\n"
            out.write(metadata.toByteArray())
        }
        recordBackupComplete()
        backupFile
    }
}
