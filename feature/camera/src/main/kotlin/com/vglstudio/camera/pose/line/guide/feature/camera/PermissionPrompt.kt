package com.vglstudio.camera.pose.line.guide.feature.camera

/**
 * Whether the system permission dialog was really shown (T-026). Android does not say so directly; it is inferred from
 * "should show request permission rationale" read just before and just after the request. A permanently denied
 * permission returns "denied" at once with both values false.
 * On Android 11+, tapping outside the dialog on the first request also reads as "not shown" (accepted, plan T-026).
 */
internal fun permissionDialogShown(granted: Boolean, rationaleBefore: Boolean, rationaleAfter: Boolean): Boolean =
    granted || rationaleBefore || rationaleAfter
