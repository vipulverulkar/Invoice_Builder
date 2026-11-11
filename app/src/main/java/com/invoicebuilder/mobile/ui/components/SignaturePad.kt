package com.invoicebuilder.mobile.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 6f,
    onSignatureCaptured: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Draw Signature",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.medium)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val path = Path().apply { moveTo(offset.x, offset.y) }
                                currentPath = path
                                paths.add(path)
                            },
                            onDrag = { change, _ ->
                                currentPath?.lineTo(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                currentPath = null
                            }
                        )
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    canvasWidth = size.width.toInt()
                    canvasHeight = size.height.toInt()

                    for (path in paths) {
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { paths.clear() }) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (paths.isNotEmpty() && canvasWidth > 0 && canvasHeight > 0) {
                                val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                                val androidCanvas = android.graphics.Canvas(bitmap)
                                androidCanvas.drawColor(android.graphics.Color.TRANSPARENT)

                                val paint = Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    style = Paint.Style.STROKE
                                    this.strokeWidth = strokeWidth
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    isAntiAlias = true
                                }

                                for (path in paths) {
                                    androidCanvas.drawPath(path.asAndroidPath(), paint)
                                }

                                val stream = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                onSignatureCaptured(stream.toByteArray())
                            }
                        },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Signature")
                    }
                }
            }
        }
    }
}
