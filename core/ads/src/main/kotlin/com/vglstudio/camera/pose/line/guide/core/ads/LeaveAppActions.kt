package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable

// Skeleton for the tests written first (plan T-074, pass 2): no hold, no mark yet.

@Composable
fun <I, O> rememberAdHoldingLauncher(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): ActivityResultLauncher<I> = rememberLauncherForActivityResult(contract, onResult)

@Composable
fun rememberStartOutsideApp(): (Intent) -> Boolean = { false }

@Composable
fun rememberStartAdHold(): () -> (() -> Unit) = { {} }
