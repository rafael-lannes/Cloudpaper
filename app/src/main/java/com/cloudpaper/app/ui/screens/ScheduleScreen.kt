package com.cloudpaper.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cloudpaper.app.data.model.*
import com.cloudpaper.app.ui.components.TimePickerDialog
import com.cloudpaper.app.ui.components.WallpaperPickerBottomSheet
import com.cloudpaper.app.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun ScheduleScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.scheduleConfig.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()
    val selectedDayForPicker by viewModel.selectedDayForPicker.collectAsState()

    var showTimePicker by remember { mutableStateOf(false) }
    val today = remember { DayOfWeekItem.currentDay() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. TOP HEADER & SETTINGS CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row with Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Trocas Agendadas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (config.isAutoChangeEnabled && config.changeMode == AutoChangeMode.DAILY_SCHEDULE)
                                        "Ativo diariamente"
                                    else
                                        "Modo agendado inativo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = config.isAutoChangeEnabled && config.changeMode == AutoChangeMode.DAILY_SCHEDULE,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    viewModel.setChangeMode(AutoChangeMode.DAILY_SCHEDULE)
                                    viewModel.setAutoChangeEnabled(true)
                                } else {
                                    viewModel.setAutoChangeEnabled(false)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Selector Chips
                    Text(
                        text = "Modo de Troca Automática:",
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
                            onClick = {
                                viewModel.setChangeMode(AutoChangeMode.DAILY_SCHEDULE)
                                if (!config.isAutoChangeEnabled) viewModel.setAutoChangeEnabled(true)
                            },
                            label = { Text("Agenda Semanal", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        FilterChip(
                            selected = config.changeMode == AutoChangeMode.INTERVAL,
                            onClick = {
                                viewModel.setChangeMode(AutoChangeMode.INTERVAL)
                                if (!config.isAutoChangeEnabled) viewModel.setAutoChangeEnabled(true)
                            },
                            label = { Text("Por Intervalo", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Time of Day Picker Pill
                    Surface(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Horário da Troca Diária",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val formattedTime = String.format("%02d:%02d", config.scheduledHour, config.scheduledMinute)
                                    Text(
                                        text = "Todos os dias às $formattedTime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { showTimePicker = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Alterar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. WEEKLY SCHEDULE TITLE
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wallpapers por Dia da Semana",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${config.weeklySchedule.size} de 7 definidos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. SEVEN DAYS LIST
        items(DayOfWeekItem.entries, key = { it.id }) { day ->
            val dayConfig = config.weeklySchedule[day]
            val isToday = day == today

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isToday)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (isToday)
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                else null,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Day Title Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = day.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (isToday) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "HOJE",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (dayConfig != null) {
                            IconButton(
                                onClick = { viewModel.removeWallpaperForDay(day) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remover",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (dayConfig != null) {
                        // Wallpaper Content Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail Preview
                            val imageModel: Any? = remember(dayConfig) {
                                if (!dayConfig.fileUriString.isNullOrBlank()) {
                                    dayConfig.fileUriString
                                } else {
                                    dayConfig.filePath
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.1f))
                                    .clickable { viewModel.openDayWallpaperPicker(day) }
                            ) {
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = dayConfig.wallpaperName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dayConfig.wallpaperName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = when (dayConfig.target) {
                                                WallpaperTarget.HOME_SCREEN -> "Tela Inicial"
                                                WallpaperTarget.LOCK_SCREEN -> "Bloqueio"
                                                WallpaperTarget.BOTH -> "Ambas as Telas"
                                            },
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (dayConfig.customBitmapPath != null || dayConfig.cropScale != 1.0f || dayConfig.cropOffsetX != 0.0f || dayConfig.cropOffsetY != 0.0f) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = if (dayConfig.isParallaxMode) "Enquadrado (Paralaxe)" else "Enquadrado (Fixo)",
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons for this day
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val item = WallpaperItem(
                                        id = dayConfig.wallpaperId,
                                        name = dayConfig.wallpaperName,
                                        fileUri = dayConfig.fileUriString?.let { android.net.Uri.parse(it) },
                                        filePath = dayConfig.filePath
                                    )
                                    viewModel.openFramingEditor(
                                        item = item,
                                        targetDay = day,
                                        initialScale = dayConfig.cropScale,
                                        initialOffsetX = dayConfig.cropOffsetX,
                                        initialOffsetY = dayConfig.cropOffsetY,
                                        initialTarget = dayConfig.target
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Enquadrar", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = { viewModel.openDayWallpaperPicker(day) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Trocar", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.changeScheduledDayWallpaperNow(day) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aplicar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Empty Day State: Prompt to select wallpaper
                        Surface(
                            onClick = { viewModel.openDayWallpaperPicker(day) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Escolher Wallpaper para ${day.shortName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. INFO FOOTER CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Dica: Nos dias em que nenhum wallpaper específico for definido, o Cloudpaper continuará sorteando uma imagem aleatória da sua pasta ativa para que você nunca fique sem trocar!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = config.scheduledHour,
            initialMinute = config.scheduledMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.setScheduledTime(hour, minute)
            }
        )
    }

    // Wallpaper Picker Bottom Sheet for a Day
    selectedDayForPicker?.let { day ->
        WallpaperPickerBottomSheet(
            day = day,
            wallpapers = wallpapers,
            onDismiss = { viewModel.closeDayWallpaperPicker() },
            onSelectWallpaper = { item ->
                // Open framing tool directly for this day so user can frame immediately
                viewModel.closeDayWallpaperPicker()
                viewModel.openFramingEditor(item = item, targetDay = day)
            },
            onImportImage = { uri ->
                viewModel.importImage(uri)
            }
        )
    }
}
