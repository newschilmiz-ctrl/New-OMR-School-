package com.example.ui

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
import androidx.compose.ui.layout.onGloballyPositioned
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

import android.graphics.Matrix
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveScanner(
    numQuestions: Int,
    numOptions: Int,
    onScanSuccess: (OmrScanner.ScanResult) -> Unit,
    onCancel: () -> Unit,
    onImageCaptured: ((Bitmap, OmrScanner.ScanResult) -> Unit)? = null
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        LiveCameraPreview(numQuestions, numOptions, onScanSuccess, onCancel, onImageCaptured)
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
    onCancel: () -> Unit,
    onImageCaptured: ((Bitmap, OmrScanner.ScanResult) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var isProcessing by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf("Position sheet inside frame") }
    var isTorchOn by remember { mutableStateOf(false) }
    var currentCamera by remember { mutableStateOf<Camera?>(null) }
    
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build() 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OMR Scanner", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Auto De-Skew & Align Active", fontSize = 12.sp, color = Color(0xFF10B981))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isTorchOn = !isTorchOn
                        currentCamera?.cameraControl?.enableTorch(isTorchOn)
                    }) {
                        Icon(
                            if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = if (isTorchOn) Color(0xFFF59E0B) else LocalContentColor.current
                        )
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
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                            currentCamera = camera
                        } catch (e: Exception) {
                            Log.e("LiveScanner", "Use case binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Document Viewfinder Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val screenW = size.width
                val screenH = size.height

                // A4 aspect ratio guide box (1:1.414) centered on screen
                val guideW = screenW * 0.86f
                val guideH = (guideW * 1.414f).coerceAtMost(screenH * 0.72f)
                val left = (screenW - guideW) / 2f
                val top = (screenH - guideH) / 2.3f

                val bracketLen = 32.dp.toPx()
                val strokeW = 4.dp.toPx()
                val bracketColor = Color(0xFF10B981) // Emerald Green

                // Top-Left corner bracket
                drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), strokeW)
                drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLen), strokeW)

                // Top-Right corner bracket
                drawLine(bracketColor, Offset(left + guideW, top), Offset(left + guideW - bracketLen, top), strokeW)
                drawLine(bracketColor, Offset(left + guideW, top), Offset(left + guideW, top + bracketLen), strokeW)

                // Bottom-Left corner bracket
                drawLine(bracketColor, Offset(left, top + guideH), Offset(left + bracketLen, top + guideH), strokeW)
                drawLine(bracketColor, Offset(left, top + guideH), Offset(left, top + guideH - bracketLen), strokeW)

                // Bottom-Right corner bracket
                drawLine(bracketColor, Offset(left + guideW, top + guideH), Offset(left + guideW - bracketLen, top + guideH), strokeW)
                drawLine(bracketColor, Offset(left + guideW, top + guideH), Offset(left + guideW, top + guideH - bracketLen), strokeW)

                // Subtle inner guide border
                drawRect(
                    color = Color.White.copy(alpha = 0.25f),
                    topLeft = Offset(left, top),
                    size = Size(guideW, guideH),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Top Helper Hint
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "Sheet chahe thedha ya chota ho, auto-straighten kar lega",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Bottom Shutter Controls
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = scanStatus,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // Large Shutter Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(if (isProcessing) Color.Gray else Color.White)
                            .clickable(enabled = !isProcessing) {
                                isProcessing = true
                                scanStatus = "Capturing high-resolution photo..."

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

                                            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                                            scope.launch(Dispatchers.Default) {
                                                try {
                                                    withContext(Dispatchers.Main) {
                                                        scanStatus = "Auto-aligning & de-skewing OMR..."
                                                    }

                                                    val maxDim = Math.max(rotatedBitmap.width, rotatedBitmap.height)
                                                    val processingBitmap = if (maxDim > 2000) {
                                                        val scale = 2000f / maxDim
                                                        Bitmap.createScaledBitmap(
                                                            rotatedBitmap,
                                                            (rotatedBitmap.width * scale).toInt(),
                                                            (rotatedBitmap.height * scale).toInt(),
                                                            true
                                                        )
                                                    } else {
                                                        rotatedBitmap
                                                    }

                                                    // Pass directly to OmrScanner for auto-detection, de-skewing & perspective warp
                                                    val result = OmrScanner.scan(processingBitmap, numQuestions, numOptions, "Standard")

                                                    withContext(Dispatchers.Main) {
                                                        if (result.answers.isNotEmpty()) {
                                                            scanStatus = "Success! Sheet calibrated."
                                                            if (onImageCaptured != null) {
                                                                onImageCaptured(processingBitmap, result)
                                                            } else {
                                                                onScanSuccess(result)
                                                            }
                                                        } else {
                                                            scanStatus = "Could not detect sheet. Please center OMR and retry."
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
                                            scanStatus = "Capture failed. Try again."
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}
