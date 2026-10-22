package com.vglstudio.camera.pose.line.guide.feature.loading.data

/**
 * The three things the app prepares before its main screen, in the order they run (spec loading, section 2).
 * [share] is the part of the progress bar, in thousandths: 10%, 10%, 80%.
 */
enum class PreparationTask(val share: Int) {
    REMOTE_CONFIG(100),
    CATALOG(100),
    STARTER_SET(800),
    ;

    companion object {
        const val TOTAL_SHARE = 1000
    }
}
