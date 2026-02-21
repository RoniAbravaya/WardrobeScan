package com.wardrobescan.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wardrobescan.app.ui.viewmodel.BulkScanViewModel
import java.io.File

/**
 * Camera screen for the multi-capture flow.
 *
 * The user can take as many photos as they want (via camera shutter or gallery picker).
 * Captured URIs accumulate in [bulkScanViewModel] without starting any heavy work.
 * [MultiCaptureControls] appears below the viewfinder once the first image is captured,
 * showing thumbnails and a "Done — Add to Wardrobe" button. Tapping Done calls [onDone],
 * which the NavGraph uses to invoke [BulkScanViewModel.startProcessing] then navigate to
 * WardrobeScreen where progress is displayed.
 *
 * Existing navigation behaviour ([onNavigateBack]) is preserved for the back button.
 *
 * @param onNavigateBack    Pops the back stack — same behaviour as before.
 * @param onDone            Triggered when the user taps "Done". The caller is responsible
 *                          for calling [BulkScanViewModel.startProcessing] and navigating.
 * @param bulkScanViewModel Shared instance provided by NavGraph; manages captured URIs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    bulkScanViewModel: BulkScanViewModel
) {
    val capturedUris by bulkScanViewModel.capturedUris.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Clear any URIs left over from a previous SCAN session (e.g., user backed out
    // without tapping Done). Runs once per composition — safe to call even when empty.
    LaunchedEffect(Unit) {
        bulkScanViewModel.clearPendingUris()
    }

    // Navigation trigger flag. Calling navController.navigate() directly inside
    // onClick can be silently dropped if the NavController is still processing a
    // lifecycle resume (e.g., returning from the camera-permission dialog or gallery
    // picker). Deferring to LaunchedEffect ensures the call happens after Compose has
    // finished its current frame and the NavController is in a navigable state.
    var triggerDone by remember { mutableStateOf(false) }
    LaunchedEffect(triggerDone) {
        if (triggerDone) {
            onDone()
            triggerDone = false
        }
    }

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        cameraGranted = grants[Manifest.permission.CAMERA] == true
        if (!cameraGranted) showPermissionDialog = true
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        // Wrap the existing onPhotoCaptured pattern — each URI is added individually.
        uris.forEach { bulkScanViewModel.addCapturedUri(it) }
    }

    LaunchedEffect(Unit) {
        val needed = buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.READ_MEDIA_IMAGES)
            else
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permLauncher.launch(needed.toTypedArray())
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Camera Required") },
            text = { Text("Please grant camera access in Settings to scan clothing items.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Items") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Camera viewfinder — expands to fill available space above the controls tray.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (cameraGranted) {
                    CameraView(
                        onPhotoCaptured = { uri ->
                            // Wrap onPhotoCaptured: add to the pending list instead of
                            // triggering immediate analysis (as in the old single-scan flow).
                            bulkScanViewModel.addCapturedUri(uri)
                        },
                        onGalleryClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                } else {
                    NoCameraPermissionPlaceholder(
                        onRequestPermission = {
                            permLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        }
                    )
                }
            }

            // Thumbnail tray + Done button — visible once at least one photo is captured.
            MultiCaptureControls(
                capturedUris = capturedUris,
                onRemoveUri = { bulkScanViewModel.removeCapturedUri(it) },
                onDone = onDone
            )
        }
    }
}

@Composable
private fun NoCameraPermissionPlaceholder(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Camera permission required",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun CameraView(
    modifier: Modifier = Modifier,
    onPhotoCaptured: (Uri) -> Unit,
    onGalleryClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
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
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bottom overlay: gallery + capture buttons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Point at a clothing item",
                style = MaterialTheme.typography.titleMedium,
                color = androidx.compose.ui.graphics.Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button
                FilledTonalIconButton(
                    onClick = onGalleryClick,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Import from Gallery",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Capture button
                FilledIconButton(
                    onClick = {
                        capturePhoto(context, imageCapture, onPhotoCaptured)
                    },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Spacer to balance layout
                Spacer(modifier = Modifier.size(52.dp))
            }
        }
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Uri) -> Unit
) {
    val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onCaptured(Uri.fromFile(file))
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}
