package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog

/** Catalog schema this app version understands. */
internal const val APP_SCHEMA = 1

internal sealed interface CatalogParseResult {
    data class Valid(val catalog: PoseCatalog, val droppedPoses: Int) : CatalogParseResult

    data object Invalid : CatalogParseResult

    data class UnsupportedSchema(val minAppSchema: Int) : CatalogParseResult
}

internal class CatalogParser(private val logger: AppLogger) {
    fun parse(text: String): CatalogParseResult = CatalogParseResult.Invalid
}
