package com.cloudpaper.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cloudpaper.app.data.model.WallpaperItem
import com.cloudpaper.app.data.model.WallpaperSource
import com.cloudpaper.app.ui.components.FullscreenPreviewDialog
import com.cloudpaper.app.ui.components.StatusBanner
import com.cloudpaper.app.ui.components.WallpaperCard
import com.cloudpaper.app.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToGallery: () -> Unit,
    onNavigateToFolderSelect: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.scheduleConfig.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()
    val isChanging by viewModel.isChangingWallpaper.collectAsState()

    var selectedPreviewItem by remember { mutableStateOf<WallpaperItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status & Overview Banner
        StatusBanner(
            config = config,
            totalOfflineWallpapers = wallpapers.size
        )

        // Primary Action: Quick Change Button
        Button(
            onClick = { viewModel.changeWallpaperNow() },
            enabled = !isChanging,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isChanging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Alterando papel de parede...", style = MaterialTheme.typography.titleMedium)
            } else {
                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Trocar Papel de Parede Agora", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Source Mode Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pasta Fonte dos Papéis de Parede",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToFolderSelect) {
                        Text("Configurar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Escolha de onde o app sorteará os papéis de parede:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                WallpaperSource.entries.forEach { source ->
                    Surface(
                        onClick = { viewModel.setWallpaperSource(source) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (config.source == source)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surface,
                        border = if (config.source == source)
                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = config.source == source,
                                onClick = { viewModel.setWallpaperSource(source) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (source == WallpaperSource.CUSTOM_DEVICE_FOLDER && config.customFolderDisplayName != null)
                                        "Pasta vinculada: ${config.customFolderDisplayName}"
                                    else
                                        source.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Offline Wallpapers Carousel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Galeria de Wallpapers (${wallpapers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (config.source == WallpaperSource.CUSTOM_DEVICE_FOLDER)
                                "📁 " + (config.customFolderDisplayName ?: "Pasta do Dispositivo")
                            else
                                "📁 " + viewModel.readableDefaultFolderPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = onNavigateToGallery) {
                        Text("Ver Todos")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                    }
                }

                if (wallpapers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum papel de parede encontrado na pasta ativa.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FilledTonalButton(onClick = onNavigateToFolderSelect) {
                                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apontar Pasta no Celular")
                            }
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(wallpapers.take(10)) { item ->
                            WallpaperCard(
                                item = item,
                                onClick = { selectedPreviewItem = item },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToFolderSelect,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pastas")
            }

            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajustes")
            }
        }
    }

    // Fullscreen Preview Dialog
    selectedPreviewItem?.let { item ->
        FullscreenPreviewDialog(
            item = item,
            onDismiss = { selectedPreviewItem = null },
            onApply = { target -> viewModel.applySpecificWallpaper(item, target) },
            onDelete = { viewModel.deleteWallpaper(item) }
        )
    }
}
