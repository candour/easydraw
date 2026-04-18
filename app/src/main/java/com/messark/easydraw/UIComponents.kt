package com.messark.easydraw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun FilePickerScreen(onFileSelected: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = onFileSelected,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.8f)
                .height(100.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Tap to Open a Picture or PDF", fontSize = 20.sp)
        }
    }
}

@Composable
fun PageSelectionScreen(thumbnails: List<android.graphics.Bitmap>, onPageSelected: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pick a page to color:", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
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
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onStrokeStarted: (Offset) -> Unit,
    onStrokeUpdated: (Offset) -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit
) {
    val colors = listOf(
        Color.Red, Color(0xFFFFA500), Color.Yellow, Color.Green,
        Color.Blue, Color(0xFF800080), Color(0xFF8B4513), Color.Black
    )

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
                .background(Color.LightGray)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> onStrokeStarted(offset) },
                            onDrag = { change, _ ->
                                change.consume()
                                onStrokeUpdated(change.position)
                            }
                        )
                    }
            ) {
                strokes.forEach { stroke ->
                    drawPath(
                        path = stroke.path,
                        color = stroke.color,
                        style = DrawScopeStroke(
                            width = stroke.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
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
                            width = if (color == currentColor) 4.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}
