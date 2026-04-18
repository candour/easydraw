package com.messark.easydraw

import android.app.Application
import androidx.compose.ui.graphics.Color
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {
    @Test
    fun `initial screen is FilePicker`() = runBlocking {
        val application = mockk<Application>()
        val viewModel = MainViewModel(application)
        assertEquals(AppScreen.FilePicker, viewModel.currentScreen.first())
    }

    @Test
    fun `navigateTo updates currentScreen`() = runBlocking {
        val application = mockk<Application>()
        val viewModel = MainViewModel(application)
        viewModel.navigateTo(AppScreen.DrawingCanvas)
        assertEquals(AppScreen.DrawingCanvas, viewModel.currentScreen.first())
    }

    @Test
    fun `undo removes last stroke`() = runBlocking {
        val application = mockk<Application>()
        val viewModel = MainViewModel(application)
        val stroke = Stroke(mockk(), Color.Red, 40f)
        viewModel.addStroke(stroke)
        assertEquals(1, viewModel.strokes.value.size)
        viewModel.undo()
        assertTrue(viewModel.strokes.value.isEmpty())
    }

    @Test
    fun `reset clears state and returns to FilePicker`() = runBlocking {
        val application = mockk<Application>()
        val viewModel = MainViewModel(application)
        viewModel.navigateTo(AppScreen.DrawingCanvas)
        viewModel.addStroke(Stroke(mockk(), Color.Red, 40f))

        viewModel.reset()

        assertEquals(AppScreen.FilePicker, viewModel.currentScreen.first())
        assertTrue(viewModel.strokes.value.isEmpty())
    }
}
