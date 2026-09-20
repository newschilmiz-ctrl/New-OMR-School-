import os

# 1. Update LiveScanner.kt
live_scanner_content = """package com.example.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.OmrScanner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveScanner(
    numQuestions: Int,
    numOptions: Int,
    onScanSuccess: (OmrScanner.ScanResult) -> Unit,
    onCancel: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        LiveCameraPreview(numQuestions, numOptions, onScanSuccess, onCancel)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to scan OMR sheets.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCameraPreview(
    numQuestions: Int,
    numOptions: Int,
    onScanSuccess: (OmrScanner.ScanResult) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var isProcessing by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("Align sheet and press shutter") }
    
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HINDI EXAMINAT...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("12th", fontSize = 14.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("LiveScanner", "Use case binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlays
            Text(
                text = "शीर्ष",
                color = Color(0xFF1E88E5), // Blue matching Ekodroid
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxSize = 80.dp.toPx()
                val marginX = 24.dp.toPx()
                val marginY = 80.dp.toPx()
                val bottomMargin = 160.dp.toPx()
                
                val strokeWidth = 3.dp.toPx()
                val blueColor = Color(0xFF0000CC) // Deep Blue
                
                // Top Left
                drawRect(color = blueColor, topLeft = Offset(marginX, marginY), size = Size(boxSize, boxSize), style = Stroke(width = strokeWidth))
                // Top Right
                drawRect(color = blueColor, topLeft = Offset(size.width - marginX - boxSize, marginY), size = Size(boxSize, boxSize), style = Stroke(width = strokeWidth))
                // Bottom Left
                val bottomY = size.height - bottomMargin - boxSize
                drawRect(color = blueColor, topLeft = Offset(marginX, bottomY), size = Size(boxSize, boxSize), style = Stroke(width = strokeWidth))
                // Bottom Right
                drawRect(color = blueColor, topLeft = Offset(size.width - marginX - boxSize, bottomY), size = Size(boxSize, boxSize), style = Stroke(width = strokeWidth))
            }

            // Shutter Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(72.dp)
                    .background(if (isProcessing) Color.Gray else Color.White, shape = CircleShape)
                    .clickable(enabled = !isProcessing) {
                        isProcessing = true
                        scanStatus = "Capturing..."
                        
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val buffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    val rotation = image.imageInfo.rotationDegrees
                                    image.close()

                                    val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                                    scope.launch(Dispatchers.Default) {
                                        try {
                                            scanStatus = "Processing Image..."
                                            // Process Image with new Perspective Transform
                                            val result = OmrScanner.scanAdvanced(rotatedBitmap, numQuestions, numOptions)
                                            
                                            withContext(Dispatchers.Main) {
                                                if (result.studentId.isNotEmpty() && result.studentId != "?") {
                                                    scanStatus = "Success!"
                                                    onScanSuccess(result)
                                                } else {
                                                    scanStatus = "Could not read. Try again."
                                                    isProcessing = false
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("LiveScanner", "Error analyzing", e)
                                            withContext(Dispatchers.Main) {
                                                scanStatus = "Error: ${e.message}"
                                                isProcessing = false
                                            }
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("LiveScanner", "Photo capture failed", exception)
                                    isProcessing = false
                                    scanStatus = "Capture failed!"
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
               // Inner UI if needed
            }

            if (isProcessing) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = scanStatus,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
"""
with open("app/src/main/java/com/example/ui/LiveScanner.kt", "w") as f:
    f.write(live_scanner_content)

# 2. Update OmrGenerator.kt to add corner markers
with open("app/src/main/java/com/example/util/OmrGenerator.kt", "r") as f:
    gen = f.read()

marker_code = """
            // DRAW 4 CORNER MARKERS FOR SCANNER ALIGNMENT
            val markerSize = 40f
            val markerPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, markerSize, markerSize, markerPaint) // TL
            canvas.drawRect(canvasWidth - markerSize, 0f, canvasWidth, markerSize, markerPaint) // TR
            canvas.drawRect(0f, canvasHeight - markerSize, markerSize, canvasHeight, markerPaint) // BL
            canvas.drawRect(canvasWidth - markerSize, canvasHeight - markerSize, canvasWidth, canvasHeight, markerPaint) // BR
"""

if "DRAW 4 CORNER MARKERS" not in gen:
    gen = gen.replace("canvas.drawColor(android.graphics.Color.WHITE)", "canvas.drawColor(android.graphics.Color.WHITE)\n" + marker_code)
    with open("app/src/main/java/com/example/util/OmrGenerator.kt", "w") as f:
        f.write(gen)

# 3. Update OmrScanner.kt with scanAdvanced function
with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    scan_code = f.read()

advanced_scan_code = """
    companion object {
        fun scanAdvanced(bitmap: android.graphics.Bitmap, numQuestions: Int, numOptions: Int): ScanResult {
            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()
            
            // Search regions based on where the blue boxes are in the UI
            val searchW = (w * 0.25f).toInt()
            val searchH = (h * 0.20f).toInt()
            
            val tl = findMarkerCenter(bitmap, 0, 0, searchW, searchH)
            val tr = findMarkerCenter(bitmap, (w - searchW).toInt(), 0, searchW, searchH)
            val bl = findMarkerCenter(bitmap, 0, (h - searchH).toInt(), searchW, searchH)
            val br = findMarkerCenter(bitmap, (w - searchW).toInt(), (h - searchH).toInt(), searchW, searchH)
            
            val a4W = 800f
            val a4H = 1131f
            
            val src = floatArrayOf(
                tl.x, tl.y,
                tr.x, tr.y,
                br.x, br.y,
                bl.x, bl.y
            )
            val dst = floatArrayOf(
                0f, 0f,
                a4W, 0f,
                a4W, a4H,
                0f, a4H
            )
            
            val matrix = android.graphics.Matrix()
            matrix.setPolyToPoly(src, 0, dst, 0, 4)
            
            val warpedBitmap = android.graphics.Bitmap.createBitmap(a4W.toInt(), a4H.toInt(), android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(warpedBitmap)
            canvas.concat(matrix)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            
            return scan(warpedBitmap, numQuestions, numOptions)
        }
        
        private fun findMarkerCenter(bitmap: android.graphics.Bitmap, startX: Int, startY: Int, width: Int, height: Int): android.graphics.PointF {
            var sumX = 0L
            var sumY = 0L
            var count = 0
            
            val endX = Math.min(startX + width, bitmap.width)
            val endY = Math.min(startY + height, bitmap.height)
            val sX = Math.max(startX, 0)
            val sY = Math.max(startY, 0)
            
            for (y in sY until endY step 2) {
                for (x in sX until endX step 2) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                    
                    if (luminance < 100) {
                        sumX += x
                        sumY += y
                        count++
                    }
                }
            }
            
            if (count > 10) {
                return android.graphics.PointF(sumX.toFloat() / count, sumY.toFloat() / count)
            }
            
            // Fallback if no black marker found in region
            return android.graphics.PointF(sX + width / 2f, sY + height / 2f)
        }
"""

if "fun scanAdvanced" not in scan_code:
    scan_code = scan_code.replace("companion object {", advanced_scan_code)
    with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
        f.write(scan_code)

