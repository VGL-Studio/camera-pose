package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * The SDK message of a load error in the load error line, at staging only (plan T-093, KB-28): built once in
 * [AdsController] from [AdsDebugOptions.logLoadErrorMessages] and handed to the loaders, like [AdNetworkLog]. With
 * [withMessage] off ([CODE_ONLY], production and every loader built without one) the line keeps the code only.
 *
 * For the load error line only (KB-28, answer 58): never for the per-network log, the metrics or Crashlytics of
 * production. The message is made safe by [logSafeMessage] before it is logged.
 */
internal class LoadErrorLog(private val withMessage: Boolean) {
    /** The message of [sdkMessage] safe to log, or null when off or when the SDK gave none (null or blank). */
    fun messageOf(sdkMessage: String?): String? =
        if (!withMessage || sdkMessage.isNullOrBlank()) null else logSafeMessage(sdkMessage)

    /** The code of a load error, with its message when there is one: `3 "No fill."`, or `3`. */
    fun text(code: Int, sdkMessage: String?): String = loadErrorText(code, messageOf(sdkMessage))

    companion object {
        /** No message: the line of production. */
        val CODE_ONLY = LoadErrorLog(false)
    }
}

/** What stands in the message for a string in the shape of an ad unit. */
internal const val AD_UNIT_MASK = "<ad unit>"

/** The longest message kept, after masking; a longer one is cut and ends with `...`. */
internal const val MAX_MESSAGE_CHARS = 300

/** `<code> "<message>"`, or the code alone when [logMessage] is null. [logMessage] is already safe to log. */
internal fun loadErrorText(code: Int, logMessage: String?): String =
    if (logMessage == null) "$code" else "$code \"$logMessage\""

/**
 * [message] safe for one log line: ids of AdMob (`ca-app-pub-<digits>` with `/<digits>` or `~<digits>`, and
 * `pub-<digits>`) masked first, then ad units of MAX (16 hex characters or more, standing alone), each as
 * [AD_UNIT_MASK]; the other way round, the hex mask would eat the 16 digits of an AdMob id. Then every run of white
 * space becomes one space, the ends are trimmed, and a message longer than [MAX_MESSAGE_CHARS] is cut and ends with
 * `...`. Masked before it is cut, so a cut never leaves half of an id.
 */
internal fun logSafeMessage(message: String): String {
    val masked = message
        .replace(ADMOB_ID, AD_UNIT_MASK)
        .replace(HEX_UNIT, AD_UNIT_MASK)
    val oneLine = masked.replace(WHITE_SPACE, " ").trim()
    return if (oneLine.length > MAX_MESSAGE_CHARS) oneLine.take(MAX_MESSAGE_CHARS) + "..." else oneLine
}

private val ADMOB_ID = Regex("""(?<![0-9A-Za-z])(?:ca-app-)?pub-[0-9]{10,}(?:[~/][0-9]+)?""")
private val HEX_UNIT = Regex("""(?<![0-9A-Za-z])[0-9A-Fa-f]{16,}(?![0-9A-Za-z])""")
private val WHITE_SPACE = Regex("""\s+""")
