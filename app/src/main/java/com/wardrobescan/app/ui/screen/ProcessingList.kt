package com.wardrobescan.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.wardrobescan.app.ui.viewmodel.ProcessingItemState
import com.wardrobescan.app.ui.viewmodel.ProcessingStatus

/**
 * Horizontal row of progress cards shown at the top of WardrobeScreen while
 * [BulkScanViewModel][com.wardrobescan.app.ui.viewmodel.BulkScanViewModel] is
 * analysing and saving batch-captured images.
 *
 * Each card displays a thumbnail (once the bitmap is decoded) and a status label.
 * Cards disappear automatically when [ProcessingStatus.ERROR] is reached or when the
 * item is saved — at that point the ViewModel removes it from [items] and the
 * WardrobeRepository Flow surfaces the new item in the grid below.
 *
 * @param items Live list from [BulkScanViewModel.processingItems][com.wardrobescan.app.ui.viewmodel.BulkScanViewModel.processingItems].
 */
@Composable
fun ProcessingList(
    items: List<ProcessingItemState>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Adding to wardrobe…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stable key prevents item recomposition when unrelated items complete.
            items(items, key = { it.id }) { item ->
                ProcessingCard(item)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

/**
 * Individual progress card for one image being processed.
 *
 * Shows a thumbnail placeholder (spinner) until the bitmap is loaded, then the
 * actual image. Below the thumbnail, the current [ProcessingStatus] is rendered
 * as a short text + icon pair.
 */
@Composable
private fun ProcessingCard(item: ProcessingItemState) {
    Card(
        modifier = Modifier.width(104.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thumbnail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bmp = item.thumbnailBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Status label
            when (item.status) {
                ProcessingStatus.PENDING -> StatusRow(label = "Pending", showSpinner = true)
                ProcessingStatus.ANALYZING -> StatusRow(label = "Analysing", showSpinner = true)
                ProcessingStatus.SAVING -> StatusRow(label = "Saving", showSpinner = true)
                ProcessingStatus.ERROR -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, showSpinner: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (showSpinner) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
