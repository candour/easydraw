# Implementation Plan - Coloring App

This document outlines the plan to implement an Android application that allows users to pick a PDF or image and color it in.

## 1. Project Setup & Navigation
*   Define the main navigation states:
    *   `FilePicker`: Initial screen to select a file.
    *   `PageSelection`: Screen to pick a page if the file is a PDF.
    *   `DrawingCanvas`: The main drawing interface.
*   Use a simple `ViewModel` to manage the app state (selected file, current page, drawing paths).

## 2. File Loading & Processing
*   **Image Loading**: Use `BitmapFactory` to load images picked by the user.
*   **PDF Loading**: Use Android's `PdfRenderer` to open PDF files.
*   **Thumbnail Generation**: For PDFs, render each page to a small `Bitmap` for the selection screen.

## 3. UI Implementation
### File Picker Screen
*   Large, friendly button to "Open a Picture or PDF".
*   Use `RememberLauncherForActivityResult` with `GetContent()` to handle file selection.

### Page Selection Screen (PDF only)
*   Display a grid of thumbnails rendered from the PDF.
*   Large tap targets for easy selection by a child.

### Drawing Canvas Screen
*   **Background**: Display the selected image or PDF page.
*   **Canvas**: A `Canvas` overlay that captures touch events and draws paths.
*   **Palette**: A row or grid of 8 large colored circles (e.g., Red, Orange, Yellow, Green, Blue, Purple, Brown, Black).
*   **Controls**:
    *   Undo button (Top left/right).
    *   Close ("X") button to return to the picker (Top left/right).

## 4. Drawing Logic
*   Maintain a list of `Stroke` objects (Path + Color).
*   Implement `pointerInput` to detect dragging and update the current stroke.
*   **Undo**: Remove the last `Stroke` from the list.
*   **Brush**: Fixed size (approx. 40-50dp to represent a fingertip).

## 5. Implementation Steps
1.  Add necessary permissions and dependencies (if any).
2.  Implement `FilePicker` and PDF rendering logic.
3.  Implement the `DrawingCanvas` with basic path drawing.
4.  Add the 8-color palette and undo functionality.
5.  Refine UI for a 5-year-old (large buttons, bright colors).
6.  Verification and Testing.
