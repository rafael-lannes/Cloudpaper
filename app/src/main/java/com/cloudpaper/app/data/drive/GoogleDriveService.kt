package com.cloudpaper.app.data.drive

import android.content.Context
import com.cloudpaper.app.data.model.SyncResult
import com.cloudpaper.app.data.model.WallpaperItem
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import java.util.regex.Pattern

class GoogleDriveService(private val context: Context) {

    /**
     * Extracts Google Drive folder ID from a direct ID or a full web URL.
     * Examples:
     * - "1A2B3C4D5E6F7G8H9I0J" -> "1A2B3C4D5E6F7G8H9I0J"
     * - "https://drive.google.com/drive/folders/1A2B3C4D5E6F7G8H9I0J?usp=sharing" -> "1A2B3C4D5E6F7G8H9I0J"
     * - "https://drive.google.com/drive/u/0/folders/1A2B3C4D5E6F7G8H9I0J" -> "1A2B3C4D5E6F7G8H9I0J"
     */
    fun extractFolderId(input: String): String {
        val trimmed = input.trim()
        val pattern = Pattern.compile("folders/([a-zA-Z0-9_-]+)")
        val matcher = pattern.matcher(trimmed)
        return if (matcher.find()) {
            matcher.group(1) ?: trimmed
        } else if (trimmed.startsWith("http")) {
            // Check for id= query parameter
            val idPattern = Pattern.compile("id=([a-zA-Z0-9_-]+)")
            val idMatcher = idPattern.matcher(trimmed)
            if (idMatcher.find()) {
                idMatcher.group(1) ?: trimmed
            } else {
                trimmed
            }
        } else {
            trimmed
        }
    }

    private fun getDriveClient(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_READONLY)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Cloudpaper")
            .build()
    }

    /**
     * Fetches list of images from the specified Google Drive folder.
     */
    suspend fun listImagesInFolder(
        account: GoogleSignInAccount,
        folderId: String
    ): List<WallpaperItem> = withContext(Dispatchers.IO) {
        val cleanFolderId = extractFolderId(folderId)
        if (cleanFolderId.isBlank()) return@withContext emptyList()

        val drive = getDriveClient(account)
        val query = "'$cleanFolderId' in parents and mimeType contains 'image/' and trashed = false"

        val resultList = mutableListOf<WallpaperItem>()
        var pageToken: String? = null

        do {
            val fileList = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name, mimeType, size, modifiedTime, thumbnailLink)")
                .setPageToken(pageToken)
                .execute()

            fileList.files?.forEach { file ->
                resultList.add(
                    WallpaperItem(
                        id = file.id,
                        name = file.name ?: "image_${file.id}",
                        sizeBytes = file.getSize() ?: 0L,
                        isCloud = true,
                        cloudFileId = file.id,
                        thumbnailUrl = file.thumbnailLink
                    )
                )
            }
            pageToken = fileList.nextPageToken
        } while (pageToken != null)

        resultList
    }

    /**
     * Downloads a specific file from Google Drive into a local destination file.
     */
    suspend fun downloadFile(
        account: GoogleSignInAccount,
        fileId: String,
        destinationFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveClient(account)
            destinationFile.parentFile?.mkdirs()
            FileOutputStream(destinationFile).use { outputStream ->
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Downloads a single random wallpaper from the Drive folder to the specified cache directory.
     */
    suspend fun downloadRandomWallpaper(
        account: GoogleSignInAccount,
        folderId: String,
        targetDir: File
    ): File? = withContext(Dispatchers.IO) {
        try {
            val images = listImagesInFolder(account, folderId)
            if (images.isEmpty()) return@withContext null

            val randomImage = images.random()
            val targetFile = File(targetDir, "drive_${randomImage.id}_${randomImage.name}")
            val success = downloadFile(account, randomImage.id, targetFile)
            if (success && targetFile.exists()) targetFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Performs full synchronization between the Google Drive folder and the local offline directory.
     */
    suspend fun syncFolderToLocal(
        account: GoogleSignInAccount,
        folderId: String,
        localFolder: File,
        onProgress: (current: Int, total: Int, currentFileName: String) -> Unit = { _, _, _ -> }
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val cleanFolderId = extractFolderId(folderId)
            if (cleanFolderId.isBlank()) {
                return@withContext SyncResult.Error("ID ou link da pasta do Google Drive não foi configurado.")
            }

            localFolder.mkdirs()

            // 1. Fetch remote files
            val remoteImages = listImagesInFolder(account, cleanFolderId)
            if (remoteImages.isEmpty()) {
                return@withContext SyncResult.Success(
                    downloadedCount = 0,
                    totalCount = 0,
                    message = "Nenhuma imagem encontrada na pasta do Google Drive."
                )
            }

            // 2. Identify local files already downloaded
            val existingFileNames = localFolder.listFiles()?.map { it.name }?.toSet() ?: emptySet()

            var downloadedCount = 0
            val totalCount = remoteImages.size

            // 3. Download missing files incrementally
            remoteImages.forEachIndexed { index, remoteItem ->
                val localFileName = "drive_${remoteItem.id}_${remoteItem.name}"
                val targetFile = File(localFolder, localFileName)

                onProgress(index + 1, totalCount, remoteItem.name)

                if (!existingFileNames.contains(localFileName) || targetFile.length() == 0L) {
                    val downloaded = downloadFile(account, remoteItem.id, targetFile)
                    if (downloaded) {
                        downloadedCount++
                    }
                }
            }

            SyncResult.Success(
                downloadedCount = downloadedCount,
                totalCount = totalCount,
                message = "Sincronizado: $downloadedCount novos papéis de parede baixados (Total: $totalCount)."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            SyncResult.Error(
                errorMessage = "Erro ao sincronizar com Google Drive: ${e.localizedMessage ?: "Erro desconhecido"}",
                throwable = e
            )
        }
    }
}
