package com.michael.sensorscope.feature.dead_pixel

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

data class PaintStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float = 25f
)

@Composable
fun DeadPixelScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Immersive Mode Management
    DisposableEffect(Unit) {
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            activity?.let { act ->
                val window = act.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Diagnostics Palette
    val colors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.White,
        Color.Black
    )
    val colorNames = listOf("Red", "Green", "Blue", "White", "Black")
    var selectedColorIndex by remember { mutableStateOf(0) }
    val activeColor = colors[selectedColorIndex]

    var isBrushMode by remember { mutableStateOf(false) }
    var hideControlsOverlay by remember { mutableStateOf(false) }

    // Brush paint trails
    val strokes = remember { mutableStateListOf<PaintStroke>() }
    var currentStrokePoints = remember { mutableStateListOf<Offset>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("dead_pixel_screen")
    ) {
        // Painting & Diagnostic Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(activeColor)
                .pointerInput(isBrushMode) {
                    if (isBrushMode) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                currentStrokePoints.add(startOffset)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentStrokePoints.add(change.position)
                            },
                            onDragEnd = {
                                if (currentStrokePoints.isNotEmpty()) {
                                    strokes.add(PaintStroke(currentStrokePoints.toList(), activeColor))
                                    currentStrokePoints.clear()
                                }
                            }
                        )
                    }
                }
                .clickable {
                    // Tap to toggle controls drawer easily in solid layout
                    hideControlsOverlay = !hideControlsOverlay
                }
                .testTag("pixel_canvas")
        ) {
            // Replay strokes on current frame using BlendMode.Src to bypass anti-aliasing artifacts
            strokes.forEach { stroke ->
                if (stroke.points.size > 1) {
                    for (i in 0 until stroke.points.size - 1) {
                        drawLine(
                            color = stroke.color,
                            start = stroke.points[i],
                            end = stroke.points[i + 1],
                            strokeWidth = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            blendMode = BlendMode.Src
                        )
                    }
                } else if (stroke.points.isNotEmpty()) {
                    drawCircle(
                        color = stroke.color,
                        radius = stroke.strokeWidth / 2,
                        center = stroke.points.first(),
                        blendMode = BlendMode.Src
                    )
                }
            }

            // Draw current active touch trail
            if (currentStrokePoints.isNotEmpty()) {
                if (currentStrokePoints.size > 1) {
                    for (i in 0 until currentStrokePoints.size - 1) {
                        drawLine(
                            color = activeColor,
                            start = currentStrokePoints[i],
                            end = currentStrokePoints[i + 1],
                            strokeWidth = 25f,
                            cap = StrokeCap.Round,
                            blendMode = BlendMode.Src
                        )
                    }
                } else {
                    drawCircle(
                        color = activeColor,
                        radius = 12.5f,
                        center = currentStrokePoints.first(),
                        blendMode = BlendMode.Src
                    )
                }
            }
        }

        // Invisible safe tapping zones for swipe cues
        if (!hideControlsOverlay) {
            // Collapsible Controls Overlay
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("palette_control_bar")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Return", tint = Color.White)
                        }

                        Text(
                            text = "Color Pattern: ${colorNames[selectedColorIndex]}",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            fontSize = 14.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Cycle paintbrush mode button
                            IconButton(
                                onClick = { isBrushMode = !isBrushMode },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isBrushMode) MaterialTheme.colorScheme.primary else Color.DarkGray
                                ),
                                modifier = Modifier.testTag("toggle_brush_btn")
                            ) {
                                Icon(
                                    imageVector = if (isBrushMode) Icons.Default.Brush else Icons.Default.FormatPaint,
                                    contentDescription = "Toggle painting paintbrush tool",
                                    tint = Color.White
                                )
                            }

                            // Reset canvas button
                            if (strokes.isNotEmpty()) {
                                IconButton(
                                    onClick = { strokes.clear() },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.DarkGray)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsBackupRestore,
                                        contentDescription = "Clear Brush Strokes",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic color selector palette
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        colors.forEachIndexed { idx, col ->
                            val isSelected = selectedColorIndex == idx
                            Button(
                                onClick = {
                                    selectedColorIndex = idx
                                    strokes.clear() // Clear strokes on color changes to inspect cleanly
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = col,
                                    contentColor = if (col == Color.White) Color.Black else Color.White
                                ),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .size(width = 56.dp, height = 44.dp)
                                    .testTag("color_pill_$idx"),
                                border = if (isSelected) {
                                    ButtonDefaults.outlinedButtonBorder.copy(
                                        width = 3.dp
                                    )
                                } else null
                            ) {}
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Swipe/Tap blank area to hide menu. Wipe screen to search for dead subpixels.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}
