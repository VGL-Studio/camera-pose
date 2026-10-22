package com.vglstudio.camera.pose.line.guide.feature.onboarding

/**
 * Whether the system permission dialog was really shown (T-026). Android does not say so directly; it is inferred from
 * "should show request permission rationale" read just before and just after the request. A permanently denied
 * permission returns "denied" at once with both values false.
 * On Android 11+, tapping outside the dialog on the first request also reads as "not shown" (accepted, plan T-026).
 *
 * Copied from :feature:camera (plan T-012, version 2): a feature cannot use another feature (ADR-0001). Change both
 * copies together; PermissionPromptTest is the same in both modules.
 *
 * | Case                         | granted | before | after | Shown |
 * | ---------------------------- | ------- | ------ | ----- | ----- |
 * | Allowed                      | true    | any    | any   | yes   |
 * | First denial                 | false   | false  | true  | yes   |
 * | Second denial ("don't ask")  | false   | true   | false | yes   |
 * | Already permanently denied   | false   | false  | false | no    |
 * | Denied, rationale both times | false   | true   | true  | yes   |
 */
internal fun permissionDialogShown(granted: Boolean, rationaleBefore: Boolean, rationaleAfter: Boolean): Boolean =
    granted || rationaleBefore || rationaleAfter
