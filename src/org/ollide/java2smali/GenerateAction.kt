package org.ollide.java2smali

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager

class GenerateAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val vFile = getVirtualFileFromContext(e) ?: return

        val project = e.project!!
        val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vFile) ?: return
        val file = PsiManager.getInstance(project).findFile(vFile) as PsiClassOwner

        // Compile the vFile's module
        val compilerCallback = CompilerCallback(module, file)
        CompilerManager.getInstance(project).compile(module, compilerCallback)
    }

    override fun update(e: AnActionEvent) {
        var enabled = false

        val vFile = getVirtualFileFromContext(e)
        if (vFile != null) {
            val extension = vFile.fileType.defaultExtension
            val m = ProjectRootManager.getInstance(e.project!!).fileIndex.getModuleForFile(vFile)
            enabled = (JAVA == extension || KOTLIN == extension) && m != null
        }
        e.presentation.isEnabled = enabled
    }

    private fun getVirtualFileFromContext(e: AnActionEvent): VirtualFile? {
        val psiFile = e.getData(LangDataKeys.PSI_FILE) ?: return null
        return psiFile.virtualFile
    }

    companion object {
        private val JAVA = "java"
        private val KOTLIN = "kt"
    }

}
