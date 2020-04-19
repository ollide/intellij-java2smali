package org.ollide.java2smali

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

class GenerateAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        LOG.debug("Action performed.")

        val vFile = getVirtualFileFromEvent(e) ?: return
        val project = e.project!!
        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vFile)!!

        DexCompiler(vFile, project, module).run()
    }

    override fun update(e: AnActionEvent) {
        var enabled = false

        getVirtualFileFromEvent(e)?.let {
            e.project?.let { project ->
                val m = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(it)
                val extension = it.fileType.defaultExtension
                enabled = (JAVA == extension || KOTLIN == extension) && m != null
            }
        }
        e.presentation.isEnabled = enabled
    }

    private fun getVirtualFileFromEvent(e: AnActionEvent): VirtualFile? {
        val psiFile = e.getData(LangDataKeys.PSI_FILE) ?: return null
        return psiFile.virtualFile
    }

    companion object {
        private val LOG = Logger.getInstance(GenerateAction::class.java)

        private const val JAVA = "java"
        private const val KOTLIN = "kt"
    }

}
