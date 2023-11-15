package com.mlp.sdk.datatypes.datasets

data class TextAndLabelData(
    val text: String,
    val label: String
)

data class TextsAndLabels(
    val items: List<TextAndLabelData>
)