package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Suppress("UNUSED_PARAMETER")
@Composable
fun StepCard(
    text: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Box(modifier)
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun StepCardArrows(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    previousDescription: String,
    nextDescription: String,
) {
}
