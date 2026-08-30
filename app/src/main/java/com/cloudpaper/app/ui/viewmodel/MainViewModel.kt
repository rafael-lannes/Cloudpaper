package com.cloudpaper.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudpaper.app.data.drive.DriveAuthHelper
import com.cloudpaper.app.data.model.ScheduleConfig
import com.cloudpaper.app.data.model.SyncResult
import com.cloudpaper.app.data.model.WallpaperItem
import com.cloudpaper.app.data.model.WallpaperSource
import com.cloudpaper.app.data.model.WallpaperTarget
import com.cloudpaper.app.data.repository.WallpaperRepository
import com.cloudpaper.app.worker.WorkScheduler
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WallpaperRepository(application)
    private val preferences = repository.preferences
    val driveAuthHelper = repository.driveAuthHelper

    val scheduleConfig: StateFlow<ScheduleConfig> = preferences.scheduleConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleConfig())

    val driveFolderId: StateFlow<String?> = preferences.driveFolderIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val driveFolderName: StateFlow<String?> = preferences.driveFolderNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googleAccountEmail: StateFlow<String?> = preferences.googleAccountEmailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googleAccountDisplayName: StateFlow<String?> = preferences.googleAccountDisplayNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _offlineWallpapers = MutableStateFlow<List<WallpaperItem>>(emptyList())
    val offlineWallpapers: StateFlow<List<WallpaperItem>> = _offlineWallpapers.asStateFlow()

    private val _syncState = MutableStateFlow<SyncResult>(SyncResult.Idle)
    val syncState: StateFlow<SyncResult> = _syncState.asStateFlow()

    private val _isChangingWallpaper = MutableStateFlow(false)
    val isChangingWallpaper: StateFlow<Boolean> = _isChangingWallpaper.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        refreshOfflineWallpapers()
        checkExistingGoogleAccount()
    }

    private fun checkExistingGoogleAccount() {
        val account = driveAuthHelper.getLastSignedInAccount()
        if (account != null) {
            viewModelScope.launch {
                preferences.setGoogleAccount(account.email, account.displayName)
            }
        }
    }

    fun refreshOfflineWallpapers() {
        viewModelScope.launch {
            _offlineWallpapers.value = repository.getOfflineWallpapers()
        }
    }

    fun changeWallpaperNow() {
        viewModelScope.launch {
            _isChangingWallpaper.value = true
            try {
                val success = repository.changeRandomWallpaper()
                if (success) {
                    _userMessage.value = "Papel de parede alterado com sucesso!"
                    refreshOfflineWallpapers()
                } else {
                    _userMessage.value = "Nenhum papel de parede disponível. Adicione imagens ou sincronize o Drive."
                }
            } catch (e: Exception) {
                _userMessage.value = "Erro ao alterar wallpaper: ${e.localizedMessage}"
            } finally {
                _isChangingWallpaper.value = false
            }
        }
    }

    fun applySpecificWallpaper(item: WallpaperItem, target: WallpaperTarget) {
        viewModelScope.launch {
            item.filePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val success = repository.applyWallpaper(file, target)
                    if (success) {
                        _userMessage.value = "Papel de parede aplicado (${target.title})!"
                    } else {
                        _userMessage.value = "Não foi possível aplicar o papel de parede."
                    }
                }
            }
        }
    }

    fun syncGoogleDrive() {
        viewModelScope.launch {
            val account = driveAuthHelper.getLastSignedInAccount()
            if (account == null) {
                _userMessage.value = "Por favor, conecte sua conta Google primeiro."
                _syncState.value = SyncResult.Error("Conta Google não conectada.")
                return@launch
            }

            val folder = driveFolderId.value
            if (folder.isNullOrBlank()) {
                _userMessage.value = "Configure a pasta do Google Drive antes de sincronizar."
                _syncState.value = SyncResult.Error("Pasta do Google Drive não configurada.")
                return@launch
            }

            _syncState.value = SyncResult.InProgress("Verificando pasta do Google Drive...", 0, 0)

            val result = repository.syncGoogleDrive { current, total, fileName ->
                _syncState.value = SyncResult.InProgress("Baixando ($current/$total): $fileName", current, total)
            }

            _syncState.value = result
            when (result) {
                is SyncResult.Success -> {
                    _userMessage.value = result.message
                    refreshOfflineWallpapers()
                }
                is SyncResult.Error -> {
                    _userMessage.value = result.errorMessage
                }
                else -> {}
            }
        }
    }

    fun importImage(uri: Uri) {
        viewModelScope.launch {
            val success = repository.importImage(uri)
            if (success) {
                _userMessage.value = "Imagem importada com sucesso para a pasta offline!"
                refreshOfflineWallpapers()
            } else {
                _userMessage.value = "Falha ao importar imagem."
            }
        }
    }

    fun deleteWallpaper(item: WallpaperItem) {
        viewModelScope.launch {
            val success = repository.deleteOfflineWallpaper(item)
            if (success) {
                _userMessage.value = "Papel de parede removido."
                refreshOfflineWallpapers()
            }
        }
    }

    fun setAutoChangeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoChangeEnabled(enabled)
            val currentConfig = scheduleConfig.value.copy(isAutoChangeEnabled = enabled)
            WorkScheduler.scheduleWallpaperRotation(getApplication(), currentConfig)
            _userMessage.value = if (enabled) "Troca automática ativada!" else "Troca automática desativada."
        }
    }

    fun setIntervalMinutes(minutes: Long) {
        viewModelScope.launch {
            preferences.setIntervalMinutes(minutes)
            val currentConfig = scheduleConfig.value.copy(intervalMinutes = minutes)
            if (currentConfig.isAutoChangeEnabled) {
                WorkScheduler.scheduleWallpaperRotation(getApplication(), currentConfig)
            }
        }
    }

    fun setWallpaperTarget(target: WallpaperTarget) {
        viewModelScope.launch {
            preferences.setWallpaperTarget(target)
        }
    }

    fun setWallpaperSource(source: WallpaperSource) {
        viewModelScope.launch {
            preferences.setWallpaperSource(source)
            if (source == WallpaperSource.AUTO_SYNC) {
                WorkScheduler.schedulePeriodicDriveSync(getApplication())
            } else {
                WorkScheduler.cancelDriveSync(getApplication())
            }
        }
    }

    fun setRequireWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            preferences.setRequireWifiOnly(wifiOnly)
            val currentConfig = scheduleConfig.value.copy(requireWifiOnly = wifiOnly)
            if (currentConfig.isAutoChangeEnabled) {
                WorkScheduler.scheduleWallpaperRotation(getApplication(), currentConfig)
            }
        }
    }

    fun setRequireChargingOnly(chargingOnly: Boolean) {
        viewModelScope.launch {
            preferences.setRequireChargingOnly(chargingOnly)
            val currentConfig = scheduleConfig.value.copy(requireChargingOnly = chargingOnly)
            if (currentConfig.isAutoChangeEnabled) {
                WorkScheduler.scheduleWallpaperRotation(getApplication(), currentConfig)
            }
        }
    }

    fun setDriveFolder(folderInput: String, folderName: String = "") {
        viewModelScope.launch {
            val extractedId = repository.driveService.extractFolderId(folderInput)
            preferences.setDriveFolder(extractedId, folderName)
            _userMessage.value = "Pasta do Google Drive configurada!"
        }
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            if (account != null) {
                preferences.setGoogleAccount(account.email, account.displayName)
                _userMessage.value = "Conectado como ${account.displayName ?: account.email}!"
            } else {
                _userMessage.value = "Falha ao autenticar com a Conta Google."
            }
        }
    }

    fun signOutGoogle() {
        driveAuthHelper.signOut {
            viewModelScope.launch {
                preferences.setGoogleAccount(null, null)
                _userMessage.value = "Conta Google desconectada."
            }
        }
    }

    val offlineFolderPath: String
        get() = repository.getOfflineFolderPath()

    val readableOfflineFolderPath: String
        get() = repository.getReadableOfflineFolderPath()

    fun openOfflineFolder(context: android.content.Context) {
        val opened = repository.openOfflineFolderInFileManager(context)
        if (!opened) {
            _userMessage.value = "Caminho copiado! Cole no seu gerenciador de arquivos."
        }
    }

    fun copyFolderPath(context: android.content.Context) {
        repository.copyFolderPathToClipboard(context)
        _userMessage.value = "Caminho da pasta copiado para a área de transferência!"
    }

    fun dismissMessage() {
        _userMessage.value = null
    }
}
