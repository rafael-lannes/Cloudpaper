package com.cloudpaper.app.ui.viewmodel

import android.app.Application
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudpaper.app.data.model.*
import com.cloudpaper.app.data.repository.WallpaperRepository
import com.cloudpaper.app.worker.WorkScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WallpaperFramingState(
    val item: WallpaperItem,
    val imageDimensions: Pair<Int, Int> = Pair(1080, 1920),
    val targetDay: DayOfWeekItem? = null,
    val initialScale: Float = 1.0f,
    val initialOffsetX: Float = 0.0f,
    val initialOffsetY: Float = 0.0f,
    val initialTarget: WallpaperTarget = WallpaperTarget.BOTH
)

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

    private val _selectedPreviewWallpaper = MutableStateFlow<WallpaperItem?>(null)
    val selectedPreviewWallpaper: StateFlow<WallpaperItem?> = _selectedPreviewWallpaper.asStateFlow()

    private val _selectedFramingState = MutableStateFlow<WallpaperFramingState?>(null)
    val selectedFramingState: StateFlow<WallpaperFramingState?> = _selectedFramingState.asStateFlow()

    private val _selectedDayForPicker = MutableStateFlow<DayOfWeekItem?>(null)
    val selectedDayForPicker: StateFlow<DayOfWeekItem?> = _selectedDayForPicker.asStateFlow()

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

    fun selectPreviewWallpaper(item: WallpaperItem) {
        _selectedPreviewWallpaper.value = item
    }

    fun clearPreviewWallpaper() {
        _selectedPreviewWallpaper.value = null
    }

    fun openFramingEditor(
        item: WallpaperItem,
        targetDay: DayOfWeekItem? = null,
        initialScale: Float = 1.0f,
        initialOffsetX: Float = 0.0f,
        initialOffsetY: Float = 0.0f,
        initialTarget: WallpaperTarget = WallpaperTarget.BOTH
    ) {
        viewModelScope.launch {
            val dimensions = repository.getImageDimensions(item)
            _selectedFramingState.value = WallpaperFramingState(
                item = item,
                imageDimensions = dimensions,
                targetDay = targetDay,
                initialScale = initialScale,
                initialOffsetX = initialOffsetX,
                initialOffsetY = initialOffsetY,
                initialTarget = initialTarget
            )
        }
    }

    fun closeFramingEditor() {
        _selectedFramingState.value = null
    }

    fun openDayWallpaperPicker(day: DayOfWeekItem) {
        _selectedDayForPicker.value = day
    }

    fun closeDayWallpaperPicker() {
        _selectedDayForPicker.value = null
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
                val config = scheduleConfig.value
                val success = when (config.changeMode) {
                    AutoChangeMode.DAILY_SCHEDULE -> repository.changeScheduledDailyWallpaper()
                    AutoChangeMode.INTERVAL -> repository.changeRandomWallpaper()
                }

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

    fun changeScheduledDayWallpaperNow(day: DayOfWeekItem) {
        viewModelScope.launch {
            _isChangingWallpaper.value = true
            try {
                val success = repository.changeScheduledDailyWallpaper(forcedDay = day)
                if (success) {
                    _userMessage.value = "Wallpaper de ${day.fullName} aplicado com sucesso!"
                    refreshWallpapers()
                } else {
                    _userMessage.value = "Não foi possível aplicar o wallpaper de ${day.fullName}."
                }
            } catch (e: Exception) {
                _userMessage.value = "Erro: ${e.localizedMessage}"
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

    fun applyFramedWallpaper(
        item: WallpaperItem,
        isParallaxMode: Boolean,
        scale: Float,
        panX: Float,
        panY: Float,
        frameWidth: Float,
        frameHeight: Float,
        target: WallpaperTarget
    ) {
        viewModelScope.launch {
            val success = repository.applyFramedWallpaper(
                item = item,
                isParallaxMode = isParallaxMode,
                scale = scale,
                panX = panX,
                panY = panY,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                target = target
            )
            if (success) {
                val modeText = if (isParallaxMode) "com rolagem de telas" else "fixo"
                _userMessage.value = "Wallpaper aplicado $modeText (${target.title})!"
            } else {
                _userMessage.value = "Não foi possível aplicar o wallpaper."
            }
        }
    }

    fun saveFramedWallpaperForDay(
        day: DayOfWeekItem,
        item: WallpaperItem,
        isParallaxMode: Boolean,
        scale: Float,
        panX: Float,
        panY: Float,
        frameWidth: Float,
        frameHeight: Float,
        target: WallpaperTarget
    ) {
        viewModelScope.launch {
            val cachedPath = repository.saveFramedWallpaperForSchedule(
                day = day,
                item = item,
                isParallaxMode = isParallaxMode,
                scale = scale,
                panX = panX,
                panY = panY,
                frameWidth = frameWidth,
                frameHeight = frameHeight
            )

            val dailyConfig = DailyWallpaperConfig(
                day = day,
                wallpaperId = item.id,
                wallpaperName = item.name,
                fileUriString = item.fileUri?.toString(),
                filePath = item.filePath,
                target = target,
                cropScale = scale,
                cropOffsetX = panX,
                cropOffsetY = panY,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                isParallaxMode = isParallaxMode,
                customBitmapPath = cachedPath,
                isEnabled = true
            )

            preferences.setDayWallpaper(dailyConfig)
            _userMessage.value = "Wallpaper enquadrado e salvo para ${day.fullName}!"
        }
    }

    fun setWallpaperForDayDirectly(day: DayOfWeekItem, item: WallpaperItem) {
        viewModelScope.launch {
            val currentSchedule = scheduleConfig.value.weeklySchedule[day]
            val dailyConfig = DailyWallpaperConfig(
                day = day,
                wallpaperId = item.id,
                wallpaperName = item.name,
                fileUriString = item.fileUri?.toString(),
                filePath = item.filePath,
                target = currentSchedule?.target ?: WallpaperTarget.BOTH,
                cropScale = 1.0f,
                cropOffsetX = 0.0f,
                cropOffsetY = 0.0f,
                isEnabled = true
            )
            preferences.setDayWallpaper(dailyConfig)
            _userMessage.value = "Wallpaper definido para ${day.fullName}!"
        }
    }

    fun removeWallpaperForDay(day: DayOfWeekItem) {
        viewModelScope.launch {
            preferences.removeDayWallpaper(day)
            _userMessage.value = "Wallpaper removido de ${day.fullName}."
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

    fun setChangeMode(mode: AutoChangeMode) {
        viewModelScope.launch {
            preferences.setChangeMode(mode)
            val currentConfig = scheduleConfig.value.copy(changeMode = mode)
            if (currentConfig.isAutoChangeEnabled) {
                WorkScheduler.scheduleWallpaperRotation(getApplication(), currentConfig)
            }
            _userMessage.value = "Modo alterado para: ${mode.title}"
        }
    }

    fun setIntervalMinutes(minutes: Long) {
        viewModelScope.launch {
            preferences.setIntervalMinutes(minutes)
            val currentConfig = scheduleConfig.value.copy(intervalMinutes = minutes)
            if (currentConfig.isAutoChangeEnabled && currentConfig.changeMode == AutoChangeMode.INTERVAL) {
                WorkScheduler.scheduleWallpaperRotation(getApplication(), currentConfig)
            }
        }
    }

    fun setScheduledTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferences.setScheduledTime(hour, minute)
            val currentConfig = scheduleConfig.value.copy(scheduledHour = hour, scheduledMinute = minute)
            if (currentConfig.isAutoChangeEnabled && currentConfig.changeMode == AutoChangeMode.DAILY_SCHEDULE) {
                WorkScheduler.scheduleWallpaperRotation(getApplication(), currentConfig)
            }
            val formattedTime = String.format("%02d:%02d", hour, minute)
            _userMessage.value = "Horário da troca diária agendado para $formattedTime"
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
