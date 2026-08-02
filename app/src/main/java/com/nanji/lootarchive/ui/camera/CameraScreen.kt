package com.nanji.lootarchive.ui.camera

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nanji.lootarchive.util.Feedback
import com.nanji.lootarchive.util.PhotoUtil
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onPhotoTaken: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var isTakingPhoto by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var flashEnabled by remember { mutableStateOf(false) }

    // 对焦标记
    var focusMarker by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var focusMarkerStamp by remember { mutableLongStateOf(0L) }

    val capturedPaths = remember { mutableStateListOf<String>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            PhotoUtil.savePhotoFromUri(context, uri)?.let { path -> capturedPaths.add(path) }
        }
    }

    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    // 闪光灯更新
    LaunchedEffect(flashEnabled, imageCapture) {
        imageCapture?.flashMode = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    // 对焦标记自动消失
    LaunchedEffect(focusMarkerStamp) {
        if (focusMarkerStamp == 0L) return@LaunchedEffect
        kotlinx.coroutines.delay(Feedback.CAMERA_FOCUS_DISMISS)
        focusMarker = null
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }.also { pv ->
                        previewView = pv
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build()
                                preview.setSurfaceProvider(pv.surfaceProvider)
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture
                                provider.unbindAll()
                                camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (exc: Exception) {
                                android.util.Log.e("CameraScreen", "相机初始化失败", exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camera, previewView) {
                        detectTapGestures { offset ->
                            val view = previewView ?: return@detectTapGestures
                            val cameraInstance = camera ?: return@detectTapGestures
                            val meteringPoint = view.meteringPointFactory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(
                                meteringPoint,
                                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                            ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                            cameraInstance.cameraControl.startFocusAndMetering(action)
                            focusMarker = offset.x to offset.y
                            focusMarkerStamp = System.currentTimeMillis()
                        }
                    }
            )

            // 对焦框
            focusMarker?.let { (x, y) ->
                Box(
                    modifier = Modifier
                        .padding(
                            start = with(density) { x.toDp() - 36.dp },
                            top = with(density) { y.toDp() - 36.dp }
                        )
                        .size(72.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                )
            }
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
            Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable { onBack() }.padding(12.dp)) {
                Icon(Icons.Default.Close, "关闭", tint = Color.White)
            }
            Text(if (capturedPaths.isEmpty()) "拍照" else "已拍 ${capturedPaths.size} 张", color = Color.White, fontSize = 16.sp)
            Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable { flashEnabled = !flashEnabled }.padding(12.dp)) {
                Icon(
                    if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    "闪光灯", tint = if (flashEnabled) Color(0xFFFF8C42) else Color.White
                )
            }
        }

        // Bottom controls
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
            .background(Color.Black.copy(alpha = 0.8f)).padding(horizontal = 20.dp, vertical = 16.dp)) {

            val canCapture = !isTakingPhoto && imageCapture != null

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(80.dp).clip(CircleShape)
                    .background(if (canCapture) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                    .border(2.dp, Color.White.copy(alpha = if (canCapture) 0.78f else 0.3f), CircleShape)
                    .clickable(enabled = canCapture) {
                        val cap = imageCapture ?: return@clickable
                        isTakingPhoto = true
                        val dir = PhotoUtil.getPhotoDir(context)
                        val file = File(dir, PhotoUtil.generatePhotoFileName())
                        cap.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    capturedPaths.add(file.absolutePath)
                                    isTakingPhoto = false
                                }
                                override fun onError(exc: ImageCaptureException) {
                                    Toast.makeText(context, "拍照失败", Toast.LENGTH_SHORT).show()
                                    isTakingPhoto = false
                                }
                            }
                        )
                    }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(60.dp).clip(CircleShape).background(
                        Brush.linearGradient(listOf(Color(0xFFFF8C42), Color(0xFFFFB347)))
                    ))
                }
            }
            Spacer(Modifier.height(16.dp))

            if (capturedPaths.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${capturedPaths.size} 张照片", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFF8C42))
                        .clickable { onPhotoTaken(capturedPaths.toList()) }.padding(horizontal = 20.dp, vertical = 10.dp)) {
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
