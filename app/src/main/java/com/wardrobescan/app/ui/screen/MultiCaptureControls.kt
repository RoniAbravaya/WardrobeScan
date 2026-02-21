package com.wardrobescan.app.ui.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Bottom tray displayed in ScanScreen once at least one photo has been captured.
 *
 * Shows a horizontally scrollable row of [capturedUris] thumbnails. Each thumbnail
 * carries a small ✕ badge so the user can discard individual photos before committing.
 * The primary "Done" [Button] is enabled only when the list is non-empty; tapping it
 * invokes [onDone] which triggers [BulkScanViewModel.startProcessing][com.wardrobescan.app.ui.viewmodel.BulkScanViewModel.startProcessing]
 * and navigates to WardrobeScreen.
 *
 * This is a pure composable — all state lives in the caller's ViewModel.
 *
 * @param capturedUris  URIs currently pending processing.
 * @param onRemoveUri   Called when the user taps ✕ on a thumbnail.
 * @param onDone        Called when the user taps "Done — Add to Wardrobe".
 */
@Composable
fun MultiCaptureControls(
    capturedUris: List<Uri>,
    onRemoveUri: (Uri) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (capturedUris.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()

        Text(
            text = "${capturedUris.size} photo${if (capturedUris.size == 1) "" else "s"} selected",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Uri.toString() is stable and unique per captured file, so it's a safe key.
            items(capturedUris, key = { it.toString() }) { uri ->
                CapturedThumbnail(
                    uri = uri,
                    onRemove = { onRemoveUri(uri) }
                )
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            enabled = capturedUris.isNotEmpty()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Done — Add to Wardrobe")
        }
    }
}

/**
 * A fixed-size square thumbnail with a small circular ✕ badge overlay.
 * Uses [AsyncImage] so both `file://` and `content://` URIs render correctly.
 */
@Composable
private fun CapturedThumbnail(
    uri: Uri,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.size(72.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = "Captured photo",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // Remove badge — uses Box+clickable to respect the small 20dp visual size
        // without Material3's minimum touch-target enforcement expanding it.
        Box(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove photo",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
