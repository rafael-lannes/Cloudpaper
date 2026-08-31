package com.cloudpaper.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.cloudpaper.app.ui.viewmodel.MainViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Início", Icons.Filled.Home, Icons.Outlined.Home),
    FOLDER("Pastas", Icons.Filled.Folder, Icons.Outlined.Folder),
    GALLERY("Galeria", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    SETTINGS("Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsState()

    // Show snackbars when userMessage changes
    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

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
                    if (currentTab == NavigationTab.HOME) {
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
                onNavigateToGallery = { currentTab = NavigationTab.GALLERY },
                onNavigateToFolderSelect = { currentTab = NavigationTab.FOLDER },
                onNavigateToSettings = { currentTab = NavigationTab.SETTINGS },
                modifier = Modifier.padding(paddingValues)
            )

            NavigationTab.FOLDER -> FolderSelectScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )

            NavigationTab.GALLERY -> GalleryScreen(
                viewModel = viewModel,
                onNavigateToFolderSelect = { currentTab = NavigationTab.FOLDER },
                modifier = Modifier.padding(paddingValues)
            )

            NavigationTab.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                onNavigateToFolderSelect = { currentTab = NavigationTab.FOLDER },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
