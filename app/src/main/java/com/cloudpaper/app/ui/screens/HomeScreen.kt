package com.cloudpaper.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import coil3.compose.AsyncImage
import com.cloudpaper.app.data.model.*
import com.cloudpaper.app.ui.components.WallpaperCard
import com.cloudpaper.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

private val COMMON_INTERVALS = listOf(
    15L to "15m",
    30L to "30m",
    60L to "1h",
    120L to "2h",
    240L to "4h",
    360L to "6h",
    720L to "12h",
    1440L to "24h"
)

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSchedule: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.scheduleConfig.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()
    val isChanging by viewModel.isChangingWallpaper.collectAsState()

    val today = remember { DayOfWeekItem.currentDay() }
    val todayScheduled = config.weeklySchedule[today]

    // Launcher for folder selection directly from Home
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let { uri ->
            val docFile = DocumentFile.fromTreeUri(context, uri)
            val displayName = docFile?.name ?: uri.lastPathSegment?.substringAfterLast(':') ?: "Pasta Selecionada"
            viewModel.setCustomFolder(uri, displayName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HERO PREVIEW CARD
        val currentWallpaperModel = remember(config, wallpapers) {
            val matched = wallpapers.find { item ->
                (config.currentWallpaperUri != null && item.fileUri?.toString() == config.currentWallpaperUri) ||
                (config.currentWallpaperPath != null && item.filePath == config.currentWallpaperPath) ||
                (config.currentWallpaperTitle != null && item.name == config.currentWallpaperTitle)
            }
            if (matched != null) {
                matched
            } else if (config.currentWallpaperUri != null || config.currentWallpaperPath != null) {
                WallpaperItem(
                    id = "current_applied",
                    name = config.currentWallpaperTitle ?: "Wallpaper Atual",
                    fileUri = config.currentWallpaperUri?.let { Uri.parse(it) },
                    filePath = config.currentWallpaperPath
                )
            } else {
                wallpapers.firstOrNull()
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (currentWallpaperModel != null) {
                    AsyncImage(
                        model = currentWallpaperModel.fileUri ?: currentWallpaperModel.filePath,
                        contentDescription = "Wallpaper Atual",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum wallpaper na pasta",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Top Badges Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge
                    Surface(
                        shape = CircleShape,
                        color = if (config.isAutoChangeEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Black.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (config.isAutoChangeEnabled) Color.Green else Color.LightGray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val modeLabel = if (config.changeMode == AutoChangeMode.DAILY_SCHEDULE) "Agenda" else "Intervalo"
                            Text(
                                text = if (config.isAutoChangeEnabled) "$modeLabel: Ativo" else "Auto: Pausado",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Enquadrar / Reposicionar Button on Hero
                    if (currentWallpaperModel != null) {
                        FilledTonalIconButton(
                            onClick = { viewModel.openFramingEditor(currentWallpaperModel) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.55f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Enquadrar",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Bottom Content on Hero
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentWallpaperModel?.name ?: "Cloudpaper",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (config.lastChangedTimestamp > 0L) {
                        val timeFormat = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())
                        Text(
                            text = "Última alteração: ${timeFormat.format(Date(config.lastChangedTimestamp))}",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // 2. PRIMARY ACTION: TROCAR AGORA
        Button(
            onClick = { viewModel.changeWallpaperNow() },
            enabled = !isChanging && wallpapers.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isChanging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Aplicando wallpaper...", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(imageVector = Icons.Default.Shuffle, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                val buttonText = if (config.changeMode == AutoChangeMode.DAILY_SCHEDULE)
                    "Aplicar Wallpaper de Hoje (${today.shortName})"
                else
                    "Trocar Wallpaper Agora"
                Text(buttonText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        // 3. CONTROLE DE TROCA AUTOMÁTICA & MODOS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (config.changeMode == AutoChangeMode.DAILY_SCHEDULE)
                                Icons.Default.CalendarMonth
                            else
                                Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Troca Automática",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val statusSubtitle = if (!config.isAutoChangeEnabled) {
                                "Pausado"
                            } else if (config.changeMode == AutoChangeMode.DAILY_SCHEDULE) {
                                val formattedTime = String.format("%02d:%02d", config.scheduledHour, config.scheduledMinute)
                                "Agenda diária às $formattedTime"
                            } else {
                                val currentIntervalLabel = COMMON_INTERVALS.find { it.first == config.intervalMinutes }?.second
                                    ?: "${config.intervalMinutes}m"
                                "A cada $currentIntervalLabel"
                            }
                            Text(
                                text = statusSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = config.isAutoChangeEnabled,
                        onCheckedChange = { viewModel.setAutoChangeEnabled(it) }
                    )
                }

                AnimatedVisibility(visible = config.isAutoChangeEnabled) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Mode Selector
                        Text(
                            text = "Modo de Operação:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = config.changeMode == AutoChangeMode.DAILY_SCHEDULE,
                                onClick = { viewModel.setChangeMode(AutoChangeMode.DAILY_SCHEDULE) },
                                label = { Text("Agenda Semanal", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            FilterChip(
                                selected = config.changeMode == AutoChangeMode.INTERVAL,
                                onClick = { viewModel.setChangeMode(AutoChangeMode.INTERVAL) },
                                label = { Text("Por Intervalo", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (config.changeMode == AutoChangeMode.DAILY_SCHEDULE) {
                            // Schedule Summary Card & Shortcut
                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Hoje: ${today.fullName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        TextButton(onClick = onNavigateToSchedule) {
                                            Text("Abrir Agenda")
                                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    if (todayScheduled != null) {
                                        Text(
                                            text = "Wallpaper: ${todayScheduled.wallpaperName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Text(
                                            text = "Nenhum fixado para hoje (será sorteado)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            // Interval Mode Chips
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Frequência:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                COMMON_INTERVALS.forEach { (minutes, label) ->
                                    FilterChip(
                                        selected = config.intervalMinutes == minutes,
                                        onClick = { viewModel.setIntervalMinutes(minutes) },
                                        label = { Text(label, fontWeight = FontWeight.Medium) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Screen Chips
                        Text(
                            text = "Aplicar na:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WallpaperTarget.entries.forEach { target ->
                                FilterChip(
                                    selected = config.target == target,
                                    onClick = { viewModel.setWallpaperTarget(target) },
                                    label = {
                                        Text(
                                            text = when (target) {
                                                WallpaperTarget.HOME_SCREEN -> "Inicial"
                                                WallpaperTarget.LOCK_SCREEN -> "Bloqueio"
                                                WallpaperTarget.BOTH -> "Ambas"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. PASTA FONTE ATIVA
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        val folderName = if (config.source == WallpaperSource.CUSTOM_DEVICE_FOLDER)
                            config.customFolderDisplayName ?: "Pasta do Celular"
                        else
                            "Pasta Padrão do App"
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${wallpapers.size} fotos encontradas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Trocar", fontSize = 13.sp)
                }
            }
        }

        // 5. CARROSSEL DE WALLPAPERS RECENTES
        if (wallpapers.isNotEmpty()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Galeria de Fotos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToGallery) {
                        Text("Ver Todas (${wallpapers.size})")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(wallpapers.take(8)) { item ->
                        WallpaperCard(
                            item = item,
                            onClick = { viewModel.selectPreviewWallpaper(item) },
                            modifier = Modifier.width(130.dp)
                        )
                    }
                }
            }
        }
    }
}
