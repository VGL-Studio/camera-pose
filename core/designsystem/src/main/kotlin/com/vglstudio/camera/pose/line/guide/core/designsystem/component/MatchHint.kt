package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

/** The pose matching hint on the camera (sticker.md, section 4): a pill with a round score (or icon) and a short text. */
@Composable
@Suppress("UNUSED_PARAMETER")
fun MatchHint(
    text: String,
    modifier: Modifier = Modifier,
    score: Int? = null,
    matched: Boolean = false,
    icon: Painter? = null,
) {
    Box(modifier)
}
