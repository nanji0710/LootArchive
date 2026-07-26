package com.nanji.lootarchive.ui.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onPhotoTaken: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }
    val capturedUris = remember { mutableStateListOf<Uri>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            capturedUris.addAll(uris)
        }
    }

    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    DisposableEffect(Unit) {
        onDispose { }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera preview
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { pv ->
                        previewView = pv
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(pv.surfaceProvider)
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture
                            try {
                                provider.unbindAll()
                                camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xFF181818)), contentAlignment = Alignment.Center) {
                Text("需要相机权限", color = Color.White)
            }
        }

        // Top bar
        Box(Modifier.fillMaxWidth().height(100.dp).align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))))
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable(onClick = onBack).padding(12.dp)) {
                Icon(Icons.Default.Close, "关闭", tint = Color.White)
            }
            Text(if (capturedUris.isEmpty()) "拍照" else "已拍 ${capturedUris.size} 张", color = Color.White, fontSize = 16.sp)
            Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable { flashEnabled = !flashEnabled }.padding(12.dp)) {
                Icon(if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, "闪光灯", tint = Color.White)
            }
        }

        // Bottom controls
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
            .background(Color.Black.copy(alpha = 0.8f)).padding(horizontal = 20.dp, vertical = 16.dp)) {
            // Shutter button
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                    .border(2.dp, Color.White.copy(alpha = 0.78f), CircleShape)
                    .clickable {
                        takePhoto(context, imageCapture) { uri -> capturedUris.add(uri) }
                    }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(60.dp).clip(CircleShape).background(
                        Brush.linearGradient(listOf(Color(0xFFD4A574), Color(0xFFE6B886)))
                    ))
                }
            }
            Spacer(Modifier.height(16.dp))

            // Thumbnails + done button
            if (capturedUris.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${capturedUris.size} 张照片", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFD4A574))
                        .clickable { onPhotoTaken(capturedUris.toList()) }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("完成", color = Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Gallery button
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.14f))
                    .clickable { galleryLauncher.launch("image/*") }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("从相册选择", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun takePhoto(context: Context, imageCapture: ImageCapture?, onResult: (Uri) -> Unit) {
    val capture = imageCapture ?: return
    val imagesDir = File(context.filesDir, "photos")
    if (!imagesDir.exists()) imagesDir.mkdirs()
    val file = File(imagesDir, "capture_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    capture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onResult(Uri.fromFile(file))
            }
            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "拍照失败", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
