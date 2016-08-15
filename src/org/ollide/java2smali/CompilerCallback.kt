package org.ollide.java2smali

import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiFile
import java.io.File
import java.io.IOException

class CompilerCallback(val module: Module, val file: PsiClassOwner) : CompileStatusNotification {

    private val DEX_EXTENSION = ".dex"
    private val SMALI_EXTENSION = ".smali"
    private val CLASS = "class"

    override fun finished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext) {
        if (errors > 0) return

        val fileName = file.virtualFile.nameWithoutExtension

        var outputDirectory = compileContext.getModuleOutputDirectory(module)
        val pkg = file.packageName.replace('.', '/')
        outputDirectory = outputDirectory?.findFileByRelativePath(pkg)

        val children = outputDirectory?.children ?: return
        val compiledPaths = children.filter {
            (it.nameWithoutExtension == fileName || it.nameWithoutExtension.startsWith(fileName + '$'))
                    && it.extension == CLASS
        }.map {
            it.path
        }.toTypedArray()

        var dexFile = outputDirectory!!.path
        if (!dexFile.endsWith('/')) dexFile += '/'
        dexFile += fileName + DEX_EXTENSION

        // CLASS -> DEX
        try {
            Class2DexHelper.dexClassFile(compiledPaths, dexFile)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        // DEX -> SMALI
        val outputDir = getSourceRootFile(file).path
        try {
            Dex2SmaliHelper.disassembleDexFile(dexFile, outputDir)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        // we've created the smali file in our source file's directory
        // refresh directory synchronously to let IDEA detect the file
        file.virtualFile.parent.refresh(false, false)

        // get a VirtualFile by the IO path
        val smaliPath = file.virtualFile.path.substringBeforeLast('.') + SMALI_EXTENSION
        val virtualDexFile = LocalFileSystem.getInstance().findFileByIoFile(File(smaliPath)) ?: return

        // use the VirtualFile to show the smali file in IDEA editor
        val openFileDescriptor = OpenFileDescriptor(file.project, virtualDexFile)
        openFileDescriptor.navigate(true)
    }

    private fun getSourceRootFile(file: PsiFile): VirtualFile {
        return ProjectRootManager.getInstance(file.project).fileIndex.getSourceRootForFile(file.virtualFile) as VirtualFile
    }

}
