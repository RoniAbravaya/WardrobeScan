package com.wardrobescan.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wardrobescan.app.data.model.ClothingCategory
import com.wardrobescan.app.data.model.ClothingItem
import com.wardrobescan.app.ui.viewmodel.BulkScanViewModel
import com.wardrobescan.app.ui.viewmodel.WardrobeViewModel

/**
 * Main wardrobe screen, showing the user's clothing grid with category filters.
 *
 * When batch-captured images are being processed by [bulkScanViewModel], a [ProcessingList]
 * tray appears at the top of the screen showing a progress card per image. Cards disappear
 * automatically as each item is saved and surfaces in the Firestore-backed [WardrobeViewModel]
 * flow below.
 *
 * @param bulkScanViewModel Shared instance from NavGraph; exposes [processingItems][com.wardrobescan.app.ui.viewmodel.BulkScanViewModel.processingItems].
 * @param viewModel         Screen-scoped WardrobeViewModel for the clothing grid and filters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeScreen(
    onNavigateToItem: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOutfits: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToScan: () -> Unit,
    bulkScanViewModel: BulkScanViewModel,
    viewModel: WardrobeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val processingItems by bulkScanViewModel.processingItems.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wardrobe", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToScan) {
                        Icon(Icons.Default.Add, "Add item")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHome,
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Checkroom, null) },
                    label = { Text("Wardrobe") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToOutfits,
                    icon = { Icon(Icons.Default.Style, null) },
                    label = { Text("Outfits") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Processing progress tray — only visible when images are being analysed/saved.
            ProcessingList(items = processingItems)

            // Category filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedCategory == null,
                    onClick = { viewModel.onCategoryFilter(null) },
                    label = { Text("All") }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ClothingCategory.entries.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.onCategoryFilter(category) },
                        label = { Text(category.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Item count
            Text(
                text = "${state.filteredItems.size} items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.filteredItems.isEmpty() && processingItems.isEmpty()) {
                EmptyWardrobeMessage(onScanClick = onNavigateToScan)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stable key prevents recomposition of unrelated cards when new items arrive.
                    items(state.filteredItems, key = { it.id }) { item ->
                        WardrobeItemCard(
                            item = item,
                            onClick = { onNavigateToItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WardrobeItemCard(
    item: ClothingItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            val imageUrl = item.cutoutUrl.ifEmpty { item.imageUrl }
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.category,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Checkroom,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.colors.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item.colors.take(3).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(color.hex))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWardrobeMessage(onScanClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Checkroom,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Your wardrobe is empty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Start scanning your clothes to build your digital wardrobe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onScanClick) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan First Item")
            }
        }
    }
}
