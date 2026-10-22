package com.vglstudio.camera.pose.line.guide.feature.loading.data

/**
 * Runs one preparation task. Returns true when the task is done, false when it could not be done.
 * Only [PreparationTask.STARTER_SET] calls [onProgress], when it starts and after each file, on any thread.
 */
interface PreparationRepository {
    suspend fun run(task: PreparationTask, onProgress: (done: Int, total: Int) -> Unit): Boolean
}
