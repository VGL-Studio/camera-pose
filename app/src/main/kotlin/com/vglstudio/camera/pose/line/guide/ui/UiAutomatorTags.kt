package com.vglstudio.camera.pose.line.guide.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Makes every `testTag` below this node show as `resource-id` in `uiautomator dump`, which is how the scenario files
 * of the plans (`scripts/device-scenario.py`, docs/ops/test-sessions.md) pick elements. The flag does not cross
 * windows: a dialog or a popup is its own window, so wherever the app opens one that a scenario must look into, put
 * this modifier on the root of its content (see `NetworkDialogHost`).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.exposeTestTagsForUiAutomator(): Modifier = semantics { testTagsAsResourceId = true }
