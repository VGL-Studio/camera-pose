package com.vglstudio.camera.pose.line.guide.core.ads

import com.google.android.gms.ads.initialization.AdapterStatus

/**
 * One log line per ads SDK adapter once `MobileAds.initialize` is done, sorted by adapter class name, so the team can
 * check on a staging build that mediation adapters such as AppLovin started (KB-9). The description comes from the
 * SDK and holds no user data.
 */
internal fun adapterStatusLines(statuses: Map<String, AdapterStatus>): List<String> =
    statuses.toSortedMap().map { (className, status) ->
        "Adapter $className: ${status.initializationState.name}, ${status.latency} ms, ${status.description}"
    }
