package com.cloudpaper.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cloudpaper.app.data.model.DayOfWeekItem
import com.cloudpaper.app.data.model.WallpaperItem
import com.cloudpaper.app.data.model.WallpaperTarget

@Composable
fun WallpaperFramingDialog(
    item: WallpaperItem,
    imageDimensions: Pair<Int, Int>,
    targetDay: DayOfWeekItem? = null,
    initialScale: Float = 1.0f,
    initialOffsetX: Float = 0.0f,
    initialOffsetY: Float = 0.0f,
    initialTarget: WallpaperTarget = WallpaperTarget.BOTH,
    onDismiss: () -> Unit,
    onApplyNow: (isParallaxMode: Boolean, scale: Float, panX: Float, panY: Float, frameWidth: Float, frameHeight: Float, target: WallpaperTarget) -> Unit,
    onSaveToSchedule: ((day: DayOfWeekItem, isParallaxMode: Boolean, scale: Float, panX: Float, panY: Float, frameWidth: Float, frameHeight: Float, target: WallpaperTarget) -> Unit)? = null
) {
    BackHandler(onBack = onDismiss)

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenAspect = remember(configuration) {
        configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    }

    val imgWidth = remember(imageDimensions) { maxOf(1, imageDimensions.first) }
    val imgHeight = remember(imageDimensions) { maxOf(1, imageDimensions.second) }

    var isParallaxMode by remember { mutableStateOf(imgWidth > imgHeight) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    var scale by remember { mutableFloatStateOf(initialScale) }
    var offsetX by remember { mutableFloatStateOf(initialOffsetX) }
    var offsetY by remember { mutableFloatStateOf(initialOffsetY) }
    var selectedTarget by remember { mutableStateOf(initialTarget) }
    var showDaySelectionMenu by remember { mutableStateOf(false) }

    // Central frame representing the main home screen viewport
    val frameHeightPx = remember(containerHeightPx) { containerHeightPx * 0.62f }
    val frameWidthPx = remember(frameHeightPx, screenAspect) { frameHeightPx * screenAspect }

    val frameLeft = remember(containerWidthPx, frameWidthPx) { (containerWidthPx - frameWidthPx) / 2f }
    val frameTop = remember(containerHeightPx, frameHeightPx) { (containerHeightPx - frameHeightPx) / 2f }

    // Base scale calculation
    val baseScale = remember(frameWidthPx, frameHeightPx, imgWidth, imgHeight, isParallaxMode) {
        if (imgWidth > 0 && imgHeight > 0 && frameWidthPx > 0 && frameHeightPx > 0) {
            if (isParallaxMode) {
                // Match frame height, width expands for scrolling
                frameHeightPx / imgHeight.toFloat()
            } else {
                // Fill the single screen frame
                maxOf(frameWidthPx / imgWidth.toFloat(), frameHeightPx / imgHeight.toFloat())
            }
        } else {
            1.0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101014))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* absorb clicks */ }
    ) {
        // 1. Interactive Full Image Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    containerWidthPx = coordinates.size.width.toFloat()
                    containerHeightPx = coordinates.size.height.toFloat()
                }
                .pointerInput(isParallaxMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.4f, 5.0f)
                        if (isParallaxMode) {
                            // In parallax mode, allow horizontal and vertical panning
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (baseScale > 0f && imgWidth > 0 && imgHeight > 0) {
                val imgDisplayWidthDp = with(density) { (imgWidth * baseScale).toDp() }
                val imgDisplayHeightDp = with(density) { (imgHeight * baseScale).toDp() }

                AsyncImage(
                    model = item.fileUri ?: item.filePath,
                    contentDescription = item.name,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .size(width = imgDisplayWidthDp, height = imgDisplayHeightDp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            }
        }

        // 2. Dimmed Cutout Mask Overlay with Phone Frame & Page Guides
        if (containerWidthPx > 0 && containerHeightPx > 0 && frameWidthPx > 0 && frameHeightPx > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maskColor = Color.Black.copy(alpha = 0.50f)
                val cornerRadius = 24.dp.toPx()

                // Draw 4 outer dimming rectangles
                // Top
                drawRect(
                    color = maskColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, frameTop)
                )
                // Bottom
                drawRect(
                    color = maskColor,
                    topLeft = Offset(0f, frameTop + frameHeightPx),
                    size = Size(size.width, size.height - (frameTop + frameHeightPx))
                )
                // Left
                drawRect(
                    color = maskColor,
                    topLeft = Offset(0f, frameTop),
                    size = Size(frameLeft, frameHeightPx)
                )
                // Right
                drawRect(
                    color = maskColor,
                    topLeft = Offset(frameLeft + frameWidthPx, frameTop),
                    size = Size(size.width - (frameLeft + frameWidthPx), frameHeightPx)
                )

                // If in Parallax Mode: Draw dashed side page indicators
                if (isParallaxMode) {
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    val sideFrameColor = Color.White.copy(alpha = 0.35f)

                    // Left page outline
                    drawRoundRect(
                        color = sideFrameColor,
                        topLeft = Offset(frameLeft - frameWidthPx * 0.85f, frameTop),
                        size = Size(frameWidthPx * 0.8f, frameHeightPx),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashedEffect)
                    )

                    // Right page outline
                    drawRoundRect(
                        color = sideFrameColor,
                        topLeft = Offset(frameLeft + frameWidthPx + frameWidthPx * 0.05f, frameTop),
                        size = Size(frameWidthPx * 0.8f, frameHeightPx),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashedEffect)
                    )
                }

                // Main Phone Frame Border
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(frameLeft, frameTop),
                    size = Size(frameWidthPx, frameHeightPx),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Safe Area Indicators inside Frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = with(density) { frameLeft.toDp() },
                        top = with(density) { frameTop.toDp() },
                        end = with(density) { (containerWidthPx - (frameLeft + frameWidthPx)).toDp() },
                        bottom = with(density) { (containerHeightPx - (frameTop + frameHeightPx)).toDp() }
                    )
            ) {
                // Top Status Bar Guide
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.30f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "08:00",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                        Icon(imageVector = Icons.Default.BatteryFull, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(13.dp))
                    }
                }

                // Center Label Hint
                if (isParallaxMode) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Tela Principal (Home)",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Bottom Nav Bar Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.30f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // 3. Top Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.7f))
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
            }

            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isParallaxMode) Icons.Default.ViewCarousel else Icons.Default.Crop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (targetDay != null) "Enquadrar: ${targetDay.shortName}" else "Enquadramento",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = {
                    scale = 1.0f
                    offsetX = 0.0f
                    offsetY = 0.0f
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.7f))
            ) {
                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Redefinir", tint = Color.White)
            }
        }

        // 4. Bottom Control & Action Panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 12.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Toggle: Rolagem / Paralaxe vs Tela Fixa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isParallaxMode,
                        onClick = {
                            isParallaxMode = true
                            scale = 1.0f
                            offsetX = 0.0f
                            offsetY = 0.0f
                        },
                        label = { Text("Rolagem / Paralaxe", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ViewCarousel,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilterChip(
                        selected = !isParallaxMode,
                        onClick = {
                            isParallaxMode = false
                            scale = 1.0f
                            offsetX = 0.0f
                            offsetY = 0.0f
                        },
                        label = { Text("Tela Fixa (1 Tela)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preset Alignment Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            scale = 1.0f
                            offsetX = 0.0f
                            offsetY = 0.0f
                        },
                        label = { Text("Centralizar", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    SuggestionChip(
                        onClick = {
                            scale = 1.0f
                            offsetY = 0.0f
                            val displayedW = imgWidth * baseScale
                            offsetX = (displayedW - frameWidthPx) / 2f
                        },
                        label = { Text("Início (Esquerda)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    SuggestionChip(
                        onClick = {
                            scale = 1.0f
                            offsetY = 0.0f
                            val displayedW = imgWidth * baseScale
                            offsetX = -(displayedW - frameWidthPx) / 2f
                        },
                        label = { Text("Fim (Direita)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    SuggestionChip(
                        onClick = {
                            if (imgWidth > 0 && imgHeight > 0 && frameWidthPx > 0 && frameHeightPx > 0) {
                                val fitScale = minOf(frameWidthPx / imgWidth.toFloat(), frameHeightPx / imgHeight.toFloat())
                                scale = fitScale / baseScale
                                offsetX = 0.0f
                                offsetY = 0.0f
                            }
                        },
                        label = { Text("Ver Toda", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Target Screen Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WallpaperTarget.entries.forEach { target ->
                        FilterChip(
                            selected = selectedTarget == target,
                            onClick = { selectedTarget = target },
                            label = {
                                Text(
                                    text = when (target) {
                                        WallpaperTarget.HOME_SCREEN -> "Inicial"
                                        WallpaperTarget.LOCK_SCREEN -> "Bloqueio"
                                        WallpaperTarget.BOTH -> "Ambas"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save to Schedule Button
                    if (targetDay != null && onSaveToSchedule != null) {
                        FilledTonalButton(
                            onClick = {
                                onSaveToSchedule(targetDay, isParallaxMode, scale, offsetX, offsetY, frameWidthPx, frameHeightPx, selectedTarget)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar p/ ${targetDay.shortName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (onSaveToSchedule != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showDaySelectionMenu = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Agendar Dia", fontSize = 12.sp)
                            }

                            DropdownMenu(
                                expanded = showDaySelectionMenu,
                                onDismissRequest = { showDaySelectionMenu = false }
                            ) {
                                DayOfWeekItem.entries.forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text(day.fullName) },
                                        onClick = {
                                            showDaySelectionMenu = false
                                            onSaveToSchedule(day, isParallaxMode, scale, offsetX, offsetY, frameWidthPx, frameHeightPx, selectedTarget)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Apply Button
                    Button(
                        onClick = {
                            onApplyNow(isParallaxMode, scale, offsetX, offsetY, frameWidthPx, frameHeightPx, selectedTarget)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val label = if (isParallaxMode) "Aplicar c/ Rolagem" else "Aplicar Fixo"
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
