package com.messark.easydraw

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {
    private fun createViewModel(): MainViewModel {
        val application = mockk<Application>(relaxed = true)
        val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
        every { application.getSharedPreferences(any(), any()) } returns sharedPrefs
        return MainViewModel(application)
    }

    @Test
    fun `initial screen is FilePicker`() = runBlocking {
        val viewModel = createViewModel()
        assertEquals(AppScreen.FilePicker, viewModel.currentScreen.first())
    }

    @Test
    fun `navigateTo updates currentScreen`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.navigateTo(AppScreen.DrawingCanvas)
        assertEquals(AppScreen.DrawingCanvas, viewModel.currentScreen.first())
    }

    @Test
    fun `undo removes last stroke`() = runBlocking {
        val viewModel = createViewModel()
        val stroke = Stroke(emptyList(), Color.Red)
        viewModel.addStroke(stroke)
        assertEquals(1, viewModel.strokes.value.size)
        viewModel.undo()
        assertTrue(viewModel.strokes.value.isEmpty())
    }

    @Test
    fun `reset clears state and returns to FilePicker`() = runBlocking {
        val viewModel = createViewModel()
        viewModel.navigateTo(AppScreen.DrawingCanvas)
        viewModel.addStroke(Stroke(emptyList(), Color.Red))

        viewModel.reset()

        assertEquals(AppScreen.FilePicker, viewModel.currentScreen.first())
        assertTrue(viewModel.strokes.value.isEmpty())
    }

    @Test
    fun `addSegmentToLastStroke handles dot segment`() = runBlocking {
        val viewModel = createViewModel()
        val stroke = Stroke(emptyList(), Color.Red)
        viewModel.addStroke(stroke)

        val dotOffset = Offset(100f, 100f)
        val dotSegment = LineSegment(dotOffset, dotOffset, 5f)
        viewModel.addSegmentToLastStroke(dotSegment)

        val lastStroke = viewModel.strokes.value.last()
        assertEquals(1, lastStroke.segments.size)
        assertEquals(dotOffset, lastStroke.segments.first().start)
        assertEquals(dotOffset, lastStroke.segments.first().end)
    }
}
