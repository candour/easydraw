package com.messark.easydraw

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.messark.easydraw.ui.theme.EasyDrawTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyDrawTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val drawingMode by viewModel.drawingMode.collectAsState()
                val sensitivity by viewModel.sensitivity.collectAsState()
                val selectedUri by viewModel.selectedUri.collectAsState()
                val pdfThumbnails by viewModel.pdfThumbnails.collectAsState()
                val selectedBitmap by viewModel.selectedBitmap.collectAsState()
                val strokes by viewModel.strokes.collectAsState()
                val currentColor by viewModel.currentColor.collectAsState()
                val scope = rememberCoroutineScope()

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        viewModel.selectUri(uri)
                        val type = contentResolver.getType(uri)
                        scope.launch {
                            if (type == "application/pdf") {
                                val thumbnails = FileUtils.renderPdfPages(this@MainActivity, uri)
                                viewModel.setPdfThumbnails(thumbnails)
                                viewModel.navigateTo(AppScreen.PageSelection)
                            } else {
                                var bitmap = FileUtils.decodeBitmapFromUri(this@MainActivity, uri)
                                if (bitmap != null && drawingMode == DrawingMode.UNDER_LINES) {
                                    val processed = FileUtils.processBitmapForUnderLines(bitmap)
                                    bitmap.recycle()
                                    bitmap = processed
                                }
                                viewModel.selectBitmap(bitmap)
                                viewModel.navigateTo(AppScreen.DrawingCanvas)
                            }
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            AppScreen.FilePicker -> FilePickerScreen(
                                sensitivity = sensitivity,
                                onSensitivityChanged = { viewModel.setSensitivity(it) },
                                onModeSelected = { mode ->
                                    viewModel.setDrawingMode(mode)
                                    launcher.launch(arrayOf("image/*", "application/pdf"))
                                }
                            )
                            AppScreen.PageSelection -> PageSelectionScreen(pdfThumbnails) { pageIndex ->
                                scope.launch {
                                    selectedUri?.let { uri ->
                                        var bitmap = FileUtils.renderPdfPage(this@MainActivity, uri, pageIndex)
                                        if (bitmap != null && drawingMode == DrawingMode.UNDER_LINES) {
                                            val processed = FileUtils.processBitmapForUnderLines(bitmap)
                                            bitmap.recycle()
                                            bitmap = processed
                                        }
                                        viewModel.selectBitmap(bitmap)
                                        viewModel.navigateTo(AppScreen.DrawingCanvas)
                                    }
                                }
                            }
                            AppScreen.DrawingCanvas -> DrawingCanvasScreen(
                                bitmap = selectedBitmap,
                                strokes = strokes,
                                drawingMode = drawingMode,
                                sensitivity = sensitivity,
                                currentColor = currentColor,
                                onColorSelected = { viewModel.setCurrentColor(it) },
                                onStrokeStarted = { _ ->
                                    viewModel.addStroke(Stroke(emptyList(), currentColor))
                                },
                                onStrokeUpdated = { start: Offset, end: Offset, width: Float ->
                                    viewModel.addSegmentToLastStroke(LineSegment(start, end, width))
                                },
                                onUndo = { viewModel.undo() },
                                onClose = { viewModel.reset() }
                            )
                        }
                    }
                }
            }
        }
    }
}
