package com.wardrobescan.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onConsentDecided: (optIn: Boolean) -> Unit = {}
) {
    var showConsentDialog by remember { mutableStateOf(false) }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = {
                // Dismissing without choosing = decline (GDPR-safe)
                showConsentDialog = false
                onConsentDecided(false)
                onComplete()
            },
            title = { Text("Style Analysis", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Allow WardrobeScan to analyse your wardrobe to build a style profile " +
                    "(e.g. your colour preferences, style persona, and wardrobe gaps).\n\n" +
                    "This data is used only to improve your experience and may be used for " +
                    "personalised features. You can change this at any time in Settings."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showConsentDialog = false
                    onConsentDecided(true)
                    onComplete()
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConsentDialog = false
                    onConsentDecided(false)
                    onComplete()
                }) { Text("No thanks") }
            }
        )
    }

    val pages = listOf(
        OnboardingPage(
            title = "Scan Your Wardrobe",
            description = "Take photos of your clothing items and we'll automatically categorize them using AI-powered recognition.",
            icon = { Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary) }
        ),
        OnboardingPage(
            title = "Smart Outfit Suggestions",
            description = "Get daily outfit recommendations based on your local weather, occasion, and personal style.",
            icon = { Icon(Icons.Default.Checkroom, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary) }
        ),
        OnboardingPage(
            title = "Your Privacy Matters",
            description = "All image processing happens on your device. Uploading is only for backup — you're always in control of your data.",
            icon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary) }
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                pages[page].icon()

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = pages[page].title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = pages[page].description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Page indicators
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation buttons
        AnimatedVisibility(
            visible = pagerState.currentPage == pages.size - 1,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut()
        ) {
            Button(
                onClick = { showConsentDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Get Started", style = MaterialTheme.typography.titleMedium)
            }
        }

        AnimatedVisibility(
            visible = pagerState.currentPage < pages.size - 1,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    // Skip = no consent, go straight through
                    onConsentDecided(false)
                    onComplete()
                }) {
                    Text("Skip")
                }
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
