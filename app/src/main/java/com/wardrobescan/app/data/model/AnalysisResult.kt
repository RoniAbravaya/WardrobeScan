package com.wardrobescan.app.data.model

data class AnalysisResult(
    val category: ClothingCategory,
    val subcategory: String? = null,
    val labels: List<String> = emptyList(),
    val confidence: Float = 0f,
    val colors: List<DominantColor> = emptyList()
)
