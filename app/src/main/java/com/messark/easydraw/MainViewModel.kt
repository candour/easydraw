package com.messark.easydraw

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AppScreen {
    object FilePicker : AppScreen()
    object PageSelection : AppScreen()
    object DrawingCanvas : AppScreen()
}

enum class DrawingMode {
    OVER_LINES,
    UNDER_LINES
}

data class LineSegment(
    val start: Offset,
    val end: Offset,
    val width: Float
)

data class Stroke(
    val segments: List<LineSegment>,
    val color: Color,
    val version: Int = 0 // Used to trigger recomposition when segments are added
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val MIN_WIDTH_DP = 5f
        const val MAX_WIDTH_DP = 40f
        private const val PREFS_NAME = "easydraw_prefs"
        private const val KEY_SENSITIVITY = "sensitivity"
    }

    private val sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.FilePicker)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _drawingMode = MutableStateFlow(DrawingMode.OVER_LINES)
    val drawingMode: StateFlow<DrawingMode> = _drawingMode.asStateFlow()

    private val _sensitivity = MutableStateFlow(sharedPreferences.getFloat(KEY_SENSITIVITY, 0.5f))
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()

    private val _pdfThumbnails = MutableStateFlow<List<Bitmap>>(emptyList())
    val pdfThumbnails: StateFlow<List<Bitmap>> = _pdfThumbnails.asStateFlow()

    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

    private val _currentColor = MutableStateFlow(Color.Red)
    val currentColor: StateFlow<Color> = _currentColor.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setDrawingMode(mode: DrawingMode) {
        _drawingMode.value = mode
    }

    fun setSensitivity(value: Float) {
        _sensitivity.value = value
    }

    fun saveSensitivity() {
        sharedPreferences.edit().putFloat(KEY_SENSITIVITY, _sensitivity.value).apply()
    }

    fun selectUri(uri: Uri?) {
        _selectedUri.value = uri
        _strokes.value = emptyList()
    }

    fun setPdfThumbnails(thumbnails: List<Bitmap>) {
        // Clear previous bitmaps if any to avoid OOM
        _pdfThumbnails.value.forEach { it.recycle() }
        _pdfThumbnails.value = thumbnails
    }

    fun selectBitmap(bitmap: Bitmap?) {
        // Clear previous bitmap if any
        _selectedBitmap.value?.recycle()
        _selectedBitmap.value = bitmap
        _strokes.value = emptyList()
    }

    fun addStroke(stroke: Stroke) {
        _strokes.value = _strokes.value + stroke
    }

    fun addSegmentToLastStroke(segment: LineSegment) {
        val currentStrokes = _strokes.value.toMutableList()
        if (currentStrokes.isNotEmpty()) {
            val lastStrokeIdx = currentStrokes.size - 1
            val lastStroke = currentStrokes[lastStrokeIdx]
            val updatedSegments = lastStroke.segments + segment
            currentStrokes[lastStrokeIdx] = lastStroke.copy(
                segments = updatedSegments,
                version = lastStroke.version + 1
            )
            _strokes.value = currentStrokes
        }
    }

    fun undo() {
        if (_strokes.value.isNotEmpty()) {
            _strokes.value = _strokes.value.dropLast(1)
        }
    }

    fun setCurrentColor(color: Color) {
        _currentColor.value = color
    }

    fun closeDrawing() {
        if (_pdfThumbnails.value.isNotEmpty()) {
            _selectedBitmap.value?.recycle()
            _selectedBitmap.value = null
            _strokes.value = emptyList()
            _currentScreen.value = AppScreen.PageSelection
        } else {
            reset()
        }
    }

    fun reset() {
        _selectedUri.value = null
        _selectedBitmap.value?.recycle()
        _selectedBitmap.value = null
        _pdfThumbnails.value.forEach { it.recycle() }
        _pdfThumbnails.value = emptyList()
        _strokes.value = emptyList()
        _currentScreen.value = AppScreen.FilePicker
    }

    override fun onCleared() {
        super.onCleared()
        _selectedBitmap.value?.recycle()
        _pdfThumbnails.value.forEach { it.recycle() }
    }
}
