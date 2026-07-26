package com.nanji.lootarchive.ui.camera

import android.Manifest
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
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
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onPhotoTaken: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var isTakingPhoto by remember { mutableStateOf(false) }
    val capturedUris = remember { mutableStateListOf<Uri>() }

    // 用 ref 存 ImageCapture，避免 Compose state 更新时序问题
    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }

    // PreviewView 引用，由 AndroidView factory 赋值
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (uris.isNotEmpty()) capturedUris.addAll(uris) }

    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    // ─── 相机生命周期 ───
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // ─── 退出时清理 ───
    DisposableEffect(Unit) {
        onDispose {
            try {
                val provider = ProcessCameraProvider.getInstance(context).get()
                provider.unbindAll()
            } catch (_: Exception) {}
            imageCapture.value = null
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }.also { pv ->
                        previewViewRef = pv
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build()
                                preview.setSurfaceProvider(pv.surfaceProvider)
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                                imageCapture.value = capture
                                cameraReady = true
                            } catch (e: Exception) {
                                Log.e("CameraScreen", "相机绑定失败", e)
                                imageCapture.value = null
                                cameraReady = false
                            }
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

        // ─── 顶部栏 ───
        Box(Modifier.fillMaxWidth().height(100.dp).align(Alignment.TopCenter)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))))
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable { onBack() }.padding(12.dp)) {
                Icon(Icons.Default.Close, "关闭", tint = Color.White)
            }
            Text(if (capturedUris.isEmpty()) "拍照" else "已拍 ${capturedUris.size} 张", color = Color.White, fontSize = 16.sp)
            Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable { flashEnabled = !flashEnabled }.padding(12.dp)) {
                Icon(if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, "闪光灯", tint = Color.White)
            }
        }

        // ─── 底部控制栏 ───
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
            .background(Color.Black.copy(alpha = 0.8f)).padding(horizontal = 20.dp, vertical = 16.dp)) {

            val canShoot = cameraReady && !isTakingPhoto && imageCapture.value != null

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier.size(80.dp).clip(CircleShape)
                        .background(if (canShoot) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                        .border(2.dp, Color.White.copy(alpha = if (canShoot) 0.78f else 0.3f), CircleShape)
                        .clickable(enabled = canShoot) {
                            val cap = imageCapture.value ?: return@clickable
                            isTakingPhoto = true
                            val imagesDir = File(context.filesDir, "photos")
                            if (!imagesDir.exists()) imagesDir.mkdirs()
                            val file = File(imagesDir, "capture_${System.currentTimeMillis()}.jpg")
                            val outputOpts = ImageCapture.OutputFileOptions.Builder(file).build()
                            cap.takePicture(
                                outputOpts,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        capturedUris.add(Uri.fromFile(file))
                                        isTakingPhoto = false
                                    }
                                    override fun onError(exc: ImageCaptureException) {
                                        Log.e("CameraScreen", "拍照失败", exc)
                                        Toast.makeText(context, "拍照失败", Toast.LENGTH_SHORT).show()
                                        isTakingPhoto = false
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(60.dp).clip(CircleShape).background(
                        Brush.linearGradient(listOf(Color(0xFFFFA500), Color(0xFFFFB347)))
                    ))
                }
            }
            Spacer(Modifier.height(16.dp))

            if (capturedUris.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${capturedUris.size} 张照片", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFA500))
                        .clickable { onPhotoTaken(capturedUris.toList()) }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("完成", color = Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

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
