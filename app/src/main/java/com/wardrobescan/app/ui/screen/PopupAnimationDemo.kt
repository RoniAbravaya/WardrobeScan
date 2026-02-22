package com.wardrobescan.app.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PopupAnimationDemo(onClose: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fotoğraf Çekim İpucu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Animation Container
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color(0xFFE3F2FD), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        ScanAnimation()
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Lütfen her seferinde SADECE 1 ADET ürün resmini çekerek ekleyin. " +
                               "Kıyafetinizi düz, tek renkli bir zemine yerleştirip net bir şekilde çekin.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var checked by remember { mutableStateOf(false) }
                        Checkbox(checked = checked, onCheckedChange = { checked = it })
                        Text("Anladım, bir daha gösterme", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kapat ve Başla")
                    }
                }
            }
        }
    }
}

@Composable
fun ScanAnimation() {
    val infiniteTransition = rememberInfiniteTransition()

    // Pulsing background circle
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Floating effect for the shirt
    val floatY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Scan line vertical position
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Checkmark scale (pops up after scan)
    val checkmarkScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0
                0f at 1400 // wait for scan to almost finish
                1.2f at 1600 // pop up
                1f at 1700 // rest
                1f at 1900 // hold
                0f at 2000 // disappear
            }
        )
    )

    // Flash effect
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0
                0f at 1300
                0.6f at 1400
                0f at 1600
            }
        )
    )

    Canvas(modifier = Modifier.fillMaxSize(0.6f)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2, h / 2)

        // Draw pulsing background circle
        drawCircle(
            color = Color(0xFF64B5F6).copy(alpha = 0.2f),
            radius = (w / 2) * pulseScale,
            center = center
        )

        // Apply floating modifier to the shirt drawing
        withTransform({
            translate(top = floatY)
        }) {
            // Draw T-Shirt shape
            val shirtPath = Path().apply {
                moveTo(w * 0.3f, h * 0.2f)
                lineTo(w * 0.4f, h * 0.1f)
                quadraticBezierTo(w * 0.5f, h * 0.15f, w * 0.6f, h * 0.1f)
                lineTo(w * 0.7f, h * 0.2f)
                lineTo(w * 0.9f, h * 0.3f)
                lineTo(w * 0.8f, h * 0.4f)
                lineTo(w * 0.7f, h * 0.35f)
                lineTo(w * 0.7f, h * 0.9f)
                lineTo(w * 0.3f, h * 0.9f)
                lineTo(w * 0.3f, h * 0.35f)
                lineTo(w * 0.2f, h * 0.4f)
                lineTo(w * 0.1f, h * 0.3f)
                close()
            }

            // Draw shirt interior
            drawPath(
                path = shirtPath,
                color = Color(0xFF1976D2)
            )

            // Draw shirt outline
            drawPath(
                path = shirtPath,
                color = Color(0xFF0D47A1),
                style = Stroke(width = 4.dp.toPx())
            )

            // Draw Scan Line
            val currentY = h * scanLineY
            if (scanLineY < 0.9f) {
                drawLine(
                    color = Color.Green,
                    start = Offset(0f, currentY),
                    end = Offset(w, currentY),
                    strokeWidth = 4.dp.toPx()
                )
                // Scan line glow
                drawRect(
                    color = Color.Green.copy(alpha = 0.3f),
                    topLeft = Offset(0f, currentY - 20f),
                    size = Size(w, 20f)
                )
            }
        }

        // Draw Checkmark
        if (checkmarkScale > 0f) {
            withTransform({
                scale(scaleX = checkmarkScale, scaleY = checkmarkScale, pivot = center)
            }) {
                // Background circle for checkmark
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = w * 0.3f,
                    center = center
                )
                // Checkmark path
                val checkPath = Path().apply {
                    moveTo(w * 0.35f, h * 0.5f)
                    lineTo(w * 0.45f, h * 0.6f)
                    lineTo(w * 0.65f, h * 0.35f)
                }
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(
                        width = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }
        }

        // Flash overlay
        if (flashAlpha > 0f) {
            drawRoundRect(
                color = Color.White.copy(alpha = flashAlpha),
                topLeft = Offset(-w * 0.2f, -h * 0.2f),
                size = Size(w * 1.4f, h * 1.4f),
                cornerRadius = CornerRadius(16f, 16f)
            )
        }
    }
}
