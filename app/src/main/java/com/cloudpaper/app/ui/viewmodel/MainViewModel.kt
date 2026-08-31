package com.cloudpaper.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudpaper.app.data.model.ScheduleConfig
import com.cloudpaper.app.data.model.WallpaperItem
import com.cloudpaper.app.data.model.WallpaperSource
import com.cloudpaper.app.data.model.WallpaperTarget
import com.cloudpaper.app.data.repository.WallpaperRepository
import com.cloudpaper.app.worker.WorkScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WallpaperRepository(application)
    private val preferences = repository.preferences

    val scheduleConfig: StateFlow<ScheduleConfig> = preferences.scheduleConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleConfig())

    val customFolderUri: StateFlow<String?> = preferences.customFolderUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customFolderDisplayName: StateFlow<String?> = preferences.customFolderDisplayNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _wallpapers = MutableStateFlow<List<WallpaperItem>>(emptyList())
    val wallpapers: StateFlow<List<WallpaperItem>> = _wallpapers.asStateFlow()

    private val _isChangingWallpaper = MutableStateFlow(false)
    val isChangingWallpaper: StateFlow<Boolean> = _isChangingWallpaper.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        refreshWallpapers()
        viewModelScope.launch {
            scheduleConfig.collect {
                refreshWallpapers()
            }
        }
    }

    fun refreshWallpapers() {
        viewModelScope.launch {
            _wallpapers.value = repository.getActiveWallpapers()
        }
    }

    fun changeWallpaperNow() {
        viewModelScope.launch {
            _isChangingWallpaper.value = true
            try {
                val success = repository.changeRandomWallpaper()
                if (success) {
                    _userMessage.value = "Papel de parede alterado com sucesso!"
                    refreshWallpapers()
                } else {
                    _userMessage.value = "Nenhum papel de parede encontrado na pasta ativa. Adicione imagens ou selecione outra pasta."
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
            val success = repository.applyWallpaper(item, target)
            if (success) {
                _userMessage.value = "Papel de parede aplicado (${target.title})!"
            } else {
                _userMessage.value = "Não foi possível aplicar o papel de parede."
            }
        }
    }

    fun importImage(uri: Uri) {
        viewModelScope.launch {
            val success = repository.importImage(uri)
            if (success) {
                _userMessage.value = "Imagem importada com sucesso para a pasta do aplicativo!"
                refreshWallpapers()
            } else {
                _userMessage.value = "Falha ao importar imagem."
            }
        }
    }

    fun deleteWallpaper(item: WallpaperItem) {
        viewModelScope.launch {
            val success = repository.deleteWallpaper(item)
            if (success) {
                _userMessage.value = "Papel de parede removido."
                refreshWallpapers()
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
            refreshWallpapers()
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

    fun setCustomFolder(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            repository.takePersistableUriPermission(uri)
            val name = displayName ?: (uri.lastPathSegment?.substringAfterLast(':') ?: "Pasta Selecionada")
            preferences.setCustomFolder(uri.toString(), name)
            _userMessage.value = "Pasta '$name' selecionada como fonte de papéis de parede!"
            refreshWallpapers()
        }
    }

    fun resetToDefaultFolder() {
        viewModelScope.launch {
            preferences.setWallpaperSource(WallpaperSource.DEFAULT_APP_FOLDER)
            _userMessage.value = "Fonte alterada para a pasta padrão do aplicativo."
            refreshWallpapers()
        }
    }

    val defaultFolderPath: String
        get() = repository.getDefaultFolderPath()

    val readableDefaultFolderPath: String
        get() = repository.getReadableDefaultFolderPath()

    fun openFolder(context: android.content.Context) {
        val config = scheduleConfig.value
        val customUri = if (config.source == WallpaperSource.CUSTOM_DEVICE_FOLDER && !config.customFolderUriString.isNullOrBlank()) {
            Uri.parse(config.customFolderUriString)
        } else {
            null
        }

        val opened = repository.openFolderInFileManager(context, customUri)
        if (!opened) {
            _userMessage.value = "Caminho da pasta copiado para a área de transferência!"
        }
    }

    fun copyFolderPath(context: android.content.Context) {
        val config = scheduleConfig.value
        val path = if (config.source == WallpaperSource.CUSTOM_DEVICE_FOLDER && !config.customFolderDisplayName.isNullOrBlank()) {
            config.customFolderDisplayName ?: repository.getDefaultFolderPath()
        } else {
            repository.getDefaultFolderPath()
        }
        repository.copyFolderPathToClipboard(context, path)
        _userMessage.value = "Caminho copiado para a área de transferência!"
    }

    fun dismissMessage() {
        _userMessage.value = null
    }
}
