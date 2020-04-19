package org.ollide.java2smali

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskManager
import com.intellij.task.ProjectTaskNotification
import com.intellij.task.ProjectTaskResult
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise

/**
 * ProjectTaskManager wrapper that checks the IDE's build version
 * to execute the correct run method (Promise-based vs. Notification-based).
 */
class SupportProjectTaskManager(private val taskManager: ProjectTaskManager) {

    @Suppress("Deprecation", "UnstableApiUsage", "MissingRecentApi")
    fun run(projectTask: ProjectTask): Promise<TaskResult> {
        return if (newApiAvailable()) {
            taskManager.run(projectTask).then {
                TaskResult(it.hasErrors())
            }
        } else {
            val promise = AsyncPromise<TaskResult>()
            taskManager.run(projectTask, Notification(promise))
            promise
        }
    }

    @Suppress("Deprecation", "UnstableApiUsage")
    class Notification(private val promise: AsyncPromise<TaskResult>) : ProjectTaskNotification {

        override fun finished(executionResult: ProjectTaskResult) {
            promise.setResult(TaskResult(executionResult.errors > 0))
        }
    }

    class TaskResult(private val errors: Boolean) {
        fun hasErrors(): Boolean {
            return errors
        }
    }

    private fun newApiAvailable(): Boolean {
        return CURRENT_API >= NEW_RUN_API
    }

    companion object {

        /**
         * Minimum version of the promise-based ProjectTaskManager
         */
        private val NEW_RUN_API = BuildNumber("", 193, 4697, 15).withoutProductCode()
        private val CURRENT_API = ApplicationInfo.getInstance().build.withoutProductCode()
    }

}
