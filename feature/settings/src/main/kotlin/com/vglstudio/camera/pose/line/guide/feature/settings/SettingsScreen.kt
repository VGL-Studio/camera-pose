package com.vglstudio.camera.pose.line.guide.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.vglstudio.camera.pose.line.guide.core.ads.PrivacyOptionsButton
import com.vglstudio.camera.pose.line.guide.core.data.AppLanguages
import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ChoiceDialog
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SectionHeader
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SettingsRow
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SettingsRowTrailing

object SettingsTestTags {
    const val SCREEN = "settings_screen"
    const val LIST = "settings_list"
}

/** The theme choices in the order of the picker (design settings, section 2). */
private val THEME_ORDER = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)

/**
 * The settings screen wired to its view model. Opens the system pages it is asked to; a device without an app for
 * them only logs it (plan T-025, question 11). The consent row is drawn only where :core:ads requires it.
 */
@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    SettingsEvent.OpenSystemLanguageSettings -> context.openOrReport(
                        Intent(
                            Settings.ACTION_APP_LOCALE_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                        onUnavailable = { viewModel.onExternalPageUnavailable("language settings") },
                    )
                    is SettingsEvent.OpenUrl -> context.openOrReport(
                        Intent(Intent.ACTION_VIEW, event.url.toUri()),
                        onUnavailable = { viewModel.onExternalPageUnavailable("privacy policy") },
                    )
                    // The activity is rebuilt with the saved language; the back stack keeps this screen on top.
                    SettingsEvent.ApplyLanguage -> activity?.recreate()
                }
            }
        }
    }
    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onThemeClick = viewModel::onThemeClick,
        onThemeSelect = viewModel::onThemeSelect,
        onShutterSoundChange = viewModel::onShutterSoundChange,
        onLanguageClick = viewModel::onLanguageClick,
        onLanguageSelect = viewModel::onLanguageSelect,
        onPrivacyOptionsClick = viewModel::onPrivacyOptionsClick,
        onPrivacyPolicyClick = viewModel::onPrivacyPolicyClick,
        onDialogDismiss = viewModel::onDialogDismiss,
        onSaveErrorShown = viewModel::onSaveErrorShown,
        privacyOptionsEntry = { row -> PrivacyOptionsButton(content = row) },
    )
}

private fun Context.openOrReport(intent: Intent, onUnavailable: () -> Unit) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        onUnavailable()
    }
}

/**
 * The settings screen (spec settings; design settings): three groups of rows under a top bar, and the theme or
 * language picker over them. [privacyOptionsEntry] draws the privacy options row through :core:ads, which hides it
 * where consent options are not required; without it the row is not drawn. [dialogInWindow] false draws the picker
 * inside the screen, for screenshots; true puts it in its own window, so the scrim covers the system bars and Back
 * closes it.
 */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onThemeClick: () -> Unit,
    onThemeSelect: (ThemeMode) -> Unit,
    onShutterSoundChange: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onLanguageSelect: (String) -> Unit,
    onPrivacyOptionsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onDialogDismiss: () -> Unit,
    onSaveErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    privacyOptionsEntry: @Composable (row: @Composable (onClick: () -> Unit) -> Unit) -> Unit = {},
    dialogInWindow: Boolean = true,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveError = stringResource(R.string.settings_save_error)
    val currentOnSaveErrorShown by rememberUpdatedState(onSaveErrorShown)
    val saveFailed = (uiState as? SettingsUiState.Ready)?.saveFailed == true
    LaunchedEffect(saveFailed) {
        if (saveFailed) {
            snackbarHostState.showSnackbar(saveError)
            currentOnSaveErrorShown()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(SettingsTestTags.SCREEN),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar(onBack = onBack)
            if (uiState is SettingsUiState.Ready) {
                SettingsList(
                    state = uiState,
                    onThemeClick = onThemeClick,
                    onShutterSoundChange = onShutterSoundChange,
                    onLanguageClick = onLanguageClick,
                    onPrivacyOptionsClick = onPrivacyOptionsClick,
                    onPrivacyPolicyClick = onPrivacyPolicyClick,
                    privacyOptionsEntry = privacyOptionsEntry,
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        if (uiState is SettingsUiState.Ready && uiState.dialog != null) {
            PickerHost(inWindow = dialogInWindow, onDismiss = onDialogDismiss) {
                when (uiState.dialog) {
                    SettingsDialog.THEME -> ChoiceDialog(
                        title = stringResource(R.string.settings_theme),
                        options = THEME_ORDER.map { stringResource(themeLabel(it)) },
                        selectedIndex = THEME_ORDER.indexOf(uiState.themeMode),
                        onSelect = { onThemeSelect(THEME_ORDER[it]) },
                        dismissLabel = stringResource(R.string.settings_cancel),
                        onDismiss = onDialogDismiss,
                    )
                    SettingsDialog.LANGUAGE -> {
                        val tags = listOf(AppLanguages.SYSTEM) + AppLanguages.RELEASE
                        ChoiceDialog(
                            title = stringResource(R.string.settings_language),
                            options = tags.map { stringResource(languageLabel(it)) },
                            selectedIndex = tags.indexOf(uiState.languageTag).coerceAtLeast(0),
                            onSelect = { onLanguageSelect(tags[it]) },
                            dismissLabel = stringResource(R.string.settings_cancel),
                            onDismiss = onDialogDismiss,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_arrow_back),
                    contentDescription = stringResource(R.string.settings_back),
                )
            }
        },
        // The app shell already keeps the screen out of the system bars.
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.background,
            titleContentColor = colors.onSurface,
            navigationIconContentColor = colors.onSurface,
        ),
    )
}

@Composable
private fun SettingsList(
    state: SettingsUiState.Ready,
    onThemeClick: () -> Unit,
    onShutterSoundChange: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onPrivacyOptionsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    privacyOptionsEntry: @Composable (row: @Composable (onClick: () -> Unit) -> Unit) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(SettingsTestTags.LIST),
    ) {
        SectionHeader(title = stringResource(R.string.settings_section_general))
        SettingsRow(
            icon = painterResource(R.drawable.ic_settings_palette),
            title = stringResource(R.string.settings_theme),
            trailing = SettingsRowTrailing.Chevron(onThemeClick),
            value = stringResource(themeLabel(state.themeMode)),
        )
        SettingsRow(
            icon = painterResource(R.drawable.ic_settings_volume_up),
            title = stringResource(R.string.settings_shutter_sound),
            trailing = SettingsRowTrailing.Switch(state.shutterSoundEnabled, onShutterSoundChange),
        )
        SettingsRow(
            icon = painterResource(R.drawable.ic_settings_language),
            title = stringResource(R.string.settings_language),
            trailing = SettingsRowTrailing.Chevron(onLanguageClick),
            // The language the text is shown in, written in that language (KB-4).
            value = stringResource(R.string.settings_language_current),
        )
        SectionHeader(title = stringResource(R.string.settings_section_privacy))
        privacyOptionsEntry { adsClick ->
            SettingsRow(
                icon = painterResource(R.drawable.ic_settings_shield_person),
                title = stringResource(R.string.settings_privacy_options),
                trailing = SettingsRowTrailing.Link {
                    onPrivacyOptionsClick()
                    adsClick()
                },
            )
        }
        SettingsRow(
            icon = painterResource(R.drawable.ic_settings_policy),
            title = stringResource(R.string.settings_privacy_policy),
            trailing = SettingsRowTrailing.Link(onPrivacyPolicyClick),
        )
        SectionHeader(title = stringResource(R.string.settings_section_about))
        SettingsRow(
            icon = painterResource(R.drawable.ic_settings_info),
            title = stringResource(R.string.settings_version),
            trailing = SettingsRowTrailing.None,
            value = state.version,
        )
    }
}

/**
 * Draws a picker in its own full-screen window, as the network dialog (NetworkDialogHost): the screen draws its own
 * scrim, so the system dim is off; Back closes the picker. With [inWindow] false it is drawn in place.
 */
@Composable
private fun PickerHost(inWindow: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    if (!inWindow) {
        content()
        return
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            // Edge to edge, so the scrim also covers the status and navigation bars.
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        content()
    }
}

@StringRes
private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@StringRes
private fun languageLabel(tag: String): Int = when (tag) {
    "en" -> R.string.settings_language_name_en
    "hi" -> R.string.settings_language_name_hi
    "pt-BR" -> R.string.settings_language_name_pt_br
    "es-419" -> R.string.settings_language_name_es_419
    "id" -> R.string.settings_language_name_id
    "th" -> R.string.settings_language_name_th
    "vi" -> R.string.settings_language_name_vi
    else -> R.string.settings_language_system
}
