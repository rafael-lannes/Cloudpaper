package com.cloudpaper.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.cloudpaper.app.ui.components.FullscreenPreviewDialog
import com.cloudpaper.app.ui.components.WallpaperFramingDialog
import com.cloudpaper.app.ui.viewmodel.MainViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Início", Icons.Filled.Home, Icons.Outlined.Home),
    SCHEDULE("Agenda", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    GALLERY("Galeria", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    SETTINGS("Ajustes", Icons.Filled.Tune, Icons.Outlined.Tune)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsState()
    val selectedPreviewItem by viewModel.selectedPreviewWallpaper.collectAsState()
    val selectedFramingState by viewModel.selectedFramingState.collectAsState()

    // Show snackbars when userMessage changes
    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentTab.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        if (currentTab == NavigationTab.HOME || currentTab == NavigationTab.SCHEDULE) {
                            IconButton(onClick = { viewModel.changeWallpaperNow() }) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Trocar Wallpaper",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) }
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            when (currentTab) {
                NavigationTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSchedule = { currentTab = NavigationTab.SCHEDULE },
                    onNavigateToGallery = { currentTab = NavigationTab.GALLERY },
                    onNavigateToSettings = { currentTab = NavigationTab.SETTINGS },
                    modifier = Modifier.padding(paddingValues)
                )

                NavigationTab.SCHEDULE -> ScheduleScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )

                NavigationTab.GALLERY -> GalleryScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )

                NavigationTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Fullscreen Preview Overlay
        AnimatedVisibility(
            visible = selectedPreviewItem != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedPreviewItem?.let { item ->
                FullscreenPreviewDialog(
                    item = item,
                    onDismiss = { viewModel.clearPreviewWallpaper() },
                    onApply = { target -> viewModel.applySpecificWallpaper(item, target) },
                    onDelete = { viewModel.deleteWallpaper(item) },
                    onOpenFraming = { wallpaper ->
                        viewModel.openFramingEditor(wallpaper)
                    }
                )
            }
        }

        // Interactive Wallpaper Framing / Repositioning Dialog Overlay
        AnimatedVisibility(
            visible = selectedFramingState != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedFramingState?.let { state ->
                WallpaperFramingDialog(
                    item = state.item,
                    imageDimensions = state.imageDimensions,
                    targetDay = state.targetDay,
                    initialScale = state.initialScale,
                    initialOffsetX = state.initialOffsetX,
                    initialOffsetY = state.initialOffsetY,
                    initialTarget = state.initialTarget,
                    onDismiss = { viewModel.closeFramingEditor() },
                    onApplyNow = { isParallaxMode, scale, panX, panY, frameWidth, frameHeight, target ->
                        viewModel.applyFramedWallpaper(
                            item = state.item,
                            isParallaxMode = isParallaxMode,
                            scale = scale,
                            panX = panX,
                            panY = panY,
                            frameWidth = frameWidth,
                            frameHeight = frameHeight,
                            target = target
                        )
                    },
                    onSaveToSchedule = { day, isParallaxMode, scale, panX, panY, frameWidth, frameHeight, target ->
                        viewModel.saveFramedWallpaperForDay(
                            day = day,
                            item = state.item,
                            isParallaxMode = isParallaxMode,
                            scale = scale,
                            panX = panX,
                            panY = panY,
                            frameWidth = frameWidth,
                            frameHeight = frameHeight,
                            target = target
                        )
                    }
                )
            }
        }
    }
}
