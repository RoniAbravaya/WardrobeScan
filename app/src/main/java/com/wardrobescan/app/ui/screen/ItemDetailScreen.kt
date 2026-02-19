package com.wardrobescan.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wardrobescan.app.data.model.ClothingCategory
import com.wardrobescan.app.ui.viewmodel.ItemDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onNavigateBack()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this item? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteItem()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!state.isEditing) {
                        IconButton(onClick = { viewModel.toggleEdit() }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        TextButton(
                            onClick = { viewModel.saveChanges() },
                            enabled = !state.isSaving
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Save")
                            }
                        }
                        TextButton(onClick = { viewModel.toggleEdit() }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.item != null) {
            val item = state.item!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Image
                val imageUrl = item.cutoutUrl.ifEmpty { item.imageUrl }
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = item.category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Category
                    Text(
                        "Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.isEditing) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ClothingCategory.entries.forEach { category ->
                                FilterChip(
                                    selected = item.category.equals(category.name, ignoreCase = true),
                                    onClick = { viewModel.updateCategory(category) },
                                    label = { Text(category.displayName, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    } else {
                        AssistChip(
                            onClick = {},
                            label = { Text(item.category.replaceFirstChar { it.uppercase() }) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Colors
                    if (item.colors.isNotEmpty()) {
                        Text("Colors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            item.colors.forEach { color ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                try {
                                                    Color(android.graphics.Color.parseColor(color.hex))
                                                } catch (e: Exception) {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(color.name, style = MaterialTheme.typography.labelSmall)
                                    Text("${color.percentage.toInt()}%", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Labels
                    if (item.labels.isNotEmpty()) {
                        Text("Labels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.labels.take(5).forEach { label ->
                                SuggestionChip(onClick = {}, label = { Text(label) })
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Properties
                    Text("Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.isEditing) {
                        Text("Warmth: ${item.warmthScore}/5")
                        Slider(
                            value = item.warmthScore.toFloat(),
                            onValueChange = { viewModel.updateWarmthScore(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = item.waterproof,
                                onCheckedChange = { viewModel.updateWaterproof(it) }
                            )
                            Text("Waterproof")
                        }
                    } else {
                        ListItem(
                            headlineContent = { Text("Warmth Score") },
                            trailingContent = { Text("${item.warmthScore}/5") },
                            leadingContent = { Icon(Icons.Default.Thermostat, null) }
                        )
                        ListItem(
                            headlineContent = { Text("Waterproof") },
                            trailingContent = {
                                Icon(
                                    if (item.waterproof) Icons.Default.Check else Icons.Default.Close,
                                    null,
                                    tint = if (item.waterproof) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = { Icon(Icons.Default.WaterDrop, null) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notes
                    Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.isEditing) {
                        OutlinedTextField(
                            value = item.userNotes,
                            onValueChange = { viewModel.updateNotes(it) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            maxLines = 3
                        )
                    } else {
                        Text(
                            text = item.userNotes.ifEmpty { "No notes" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.userNotes.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
