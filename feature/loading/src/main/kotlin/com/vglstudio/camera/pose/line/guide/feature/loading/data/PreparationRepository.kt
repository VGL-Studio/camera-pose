package com.vglstudio.camera.pose.line.guide.feature.loading.data

/** Runs one preparation task. Returns true when the task is done, false when it could not be done. */
interface PreparationRepository {
    suspend fun run(task: PreparationTask): Boolean
}
