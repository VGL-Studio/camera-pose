package com.vglstudio.camera.pose.line.guide.feature.camera.model

/** Flash cycles off, on, auto (spec camera, KB-4). */
enum class FlashMode {
    OFF,
    ON,
    AUTO,
    ;

    fun next(): FlashMode = entries[(ordinal + 1) % entries.size]
}

/** Timer cycles off, 3 seconds, 10 seconds (spec camera, KB-4). */
enum class TimerSetting(val seconds: Int) {
    OFF(0),
    THREE(3),
    TEN(10),
    ;

    fun next(): TimerSetting = entries[(ordinal + 1) % entries.size]
}

enum class Lens { BACK, FRONT }

/** Capture tools kept across app launches (spec camera, KB-4). Zoom is not kept: it goes back to 1x. */
data class CameraSettings(
    val flash: FlashMode = FlashMode.OFF,
    val timer: TimerSetting = TimerSetting.OFF,
    val gridVisible: Boolean = false,
    val lens: Lens = Lens.BACK,
)
