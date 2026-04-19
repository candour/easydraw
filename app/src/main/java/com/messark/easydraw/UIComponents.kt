package com.messark.easydraw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke as DrawScopeStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun FilePickerScreen(
    sensitivity: Float,
    onSensitivityChanged: (Float) -> Unit,
    onModeSelected: (DrawingMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Swipe sensitivity", style = MaterialTheme.typography.titleMedium)

        Slider(
            value = sensitivity,
            onValueChange = onSensitivityChanged,
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 8.dp)
                .fillMaxWidth(0.8f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("High Sensitivity", style = MaterialTheme.typography.labelSmall)
            Text("Fixed Thickness", style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onModeSelected(DrawingMode.OVER_LINES) },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.8f)
                .height(100.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Draw Over Lines", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onModeSelected(DrawingMode.UNDER_LINES) },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.8f)
                .height(100.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Draw Under Lines", fontSize = 20.sp)
        }
    }
}

@Composable
fun PageSelectionScreen(
    thumbnails: List<android.graphics.Bitmap>,
    onClose: () -> Unit,
    onPageSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pick a page to color:",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(36.dp))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(thumbnails) { index, bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onPageSelected(index) }
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Page $index",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun DrawingCanvasScreen(
    bitmap: android.graphics.Bitmap?,
    strokes: List<Stroke>,
    drawingMode: DrawingMode,
    sensitivity: Float,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onStrokeStarted: (Offset) -> Unit,
    onStrokeUpdated: (Offset, Offset, Float) -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit
) {
    val colors = listOf(
        Color.Red, Color(0xFFFFA500), Color.Yellow, Color.Green,
        Color.Blue, Color(0xFF800080), Color(0xFF8B4513), Color.Black, Color.White
    )

    val density = LocalDensity.current
    val minWidthPx = with(density) { MainViewModel.MIN_WIDTH_DP.dp.toPx() }
    val maxWidthPx = with(density) { MainViewModel.MAX_WIDTH_DP.dp.toPx() }

    // Constants for speed-based width
    // sensitivity = 0.75 was roughly 5 dp/ms
    // We want sensitivity = 0 to be very sensitive (maxSpeed is small)
    // and sensitivity = 1 to be fixed thickness (maxSpeed is effectively 0 but we handle it)
    val maxSpeedPxPerMs = with(density) {
        val baseSpeed = 5.dp.toPx()
        (baseSpeed * (sensitivity / 0.75f)).coerceAtLeast(0.1.dp.toPx())
    }
    val smoothingFactor = 0.2f // For EMA

    var lastOffset by remember { mutableStateOf<Offset?>(null) }
    var lastTimestamp by remember { mutableLongStateOf(0L) }
    var currentSmoothWidth by remember { mutableFloatStateOf(maxWidthPx) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(36.dp))
            }
            Button(onClick = onUndo) {
                Text("Undo")
            }
        }

        // Canvas Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            onStrokeStarted(offset)
                            onStrokeUpdated(offset, offset, minWidthPx)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            lastOffset = offset
                            lastTimestamp = System.currentTimeMillis()
                            currentSmoothWidth = maxWidthPx
                            onStrokeStarted(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val currentOffset = change.position
                            val currentTimestamp = System.currentTimeMillis()
                            val prevOffset = lastOffset

                            if (prevOffset != null) {
                                val distance = (currentOffset - prevOffset).getDistance()
                                val timeDelta = (currentTimestamp - lastTimestamp).coerceAtLeast(1L)
                                val speed = distance / timeDelta

                                val targetWidth = if (sensitivity >= 0.99f) {
                                    maxWidthPx
                                } else {
                                    // ratio: 0 (slow) to 1 (max speed)
                                    val ratio = (speed / maxSpeedPxPerMs).coerceIn(0f, 1f)
                                    // Use quadratic drop-off: (1 - ratio)^2
                                    // At ratio=0, width is maxWidth
                                    // At ratio=1, width is minWidth
                                    val scale = (1f - ratio) * (1f - ratio)
                                    minWidthPx + scale * (maxWidthPx - minWidthPx)
                                }

                                // Apply smoothing
                                currentSmoothWidth = currentSmoothWidth + smoothingFactor * (targetWidth - currentSmoothWidth)

                                onStrokeUpdated(prevOffset, currentOffset, currentSmoothWidth)
                            }

                            lastOffset = currentOffset
                            lastTimestamp = currentTimestamp
                        },
                        onDragEnd = {
                            lastOffset = null
                        },
                        onDragCancel = {
                            lastOffset = null
                        }
                    )
                }
        ) {
            if (bitmap != null && drawingMode == DrawingMode.OVER_LINES) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                strokes.forEach { stroke ->
                    stroke.segments.forEach { segment ->
                        if (segment.start == segment.end) {
                            drawCircle(
                                color = stroke.color,
                                center = segment.start,
                                radius = segment.width / 2f
                            )
                        } else {
                            drawLine(
                                color = stroke.color,
                                start = segment.start,
                                end = segment.end,
                                strokeWidth = segment.width,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            if (bitmap != null && drawingMode == DrawingMode.UNDER_LINES) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Palette
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(colors) { color ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = CircleShape
                        )
                        .border(
                            width = if (color == currentColor) 4.dp else 0.dp,
                            color = if (color == Color.White) Color.Black else Color.White,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}
