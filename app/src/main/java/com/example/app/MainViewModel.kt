package com.example.app

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AppScreen {
    object FilePicker : AppScreen()
    object PageSelection : AppScreen()
    object DrawingCanvas : AppScreen()
}

data class Stroke(
    val path: Path,
    val color: Color,
    val width: Float,
    val version: Int = 0 // Used to trigger recomposition when path is mutated
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.FilePicker)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

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

    fun updateLastStroke(path: Path) {
        val currentStrokes = _strokes.value.toMutableList()
        if (currentStrokes.isNotEmpty()) {
            val lastStroke = currentStrokes.last()
            // Increment version to ensure StateFlow sees a change even if Path object is same
            currentStrokes[currentStrokes.size - 1] = lastStroke.copy(path = path, version = lastStroke.version + 1)
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
