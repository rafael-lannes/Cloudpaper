package com.cloudpaper.app.data.model

/**
 * Result of a synchronization operation between Google Drive and offline storage.
 */
sealed class SyncResult {
    object Idle : SyncResult()
    data class InProgress(val progressMessage: String, val current: Int, val total: Int) : SyncResult()
    data class Success(
        val downloadedCount: Int,
        val totalCount: Int,
        val message: String = "Sincronização concluída com sucesso!"
    ) : SyncResult()
    data class Error(val errorMessage: String, val throwable: Throwable? = null) : SyncResult()
}
