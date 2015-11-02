package org.ollide.java2smali;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;

public class GenerateAction extends AnAction {

    public void actionPerformed(AnActionEvent e) {
        VirtualFile vFile = getVirtualFileFromContext(e);

        Project p = e.getProject();
        Module module = ProjectRootManager.getInstance(p).getFileIndex().getModuleForFile(vFile);
        PsiJavaFile javaFile = (PsiJavaFile) PsiManager.getInstance(p).findFile(vFile);

        // Compile the vFile's module
        CompilerCallback compilerCallback = new CompilerCallback(module, javaFile);
        CompilerManager.getInstance(p).compile(module, compilerCallback);
    }

    @Override
    public void update(AnActionEvent e) {
        boolean enabled = false;

        VirtualFile vFile = getVirtualFileFromContext(e);
        if (vFile != null) {
            String extension = vFile.getFileType().getDefaultExtension();
            Module m = ProjectRootManager.getInstance(e.getProject()).getFileIndex().getModuleForFile(vFile);
            enabled = "java".equals(extension) && m != null;
        }
        e.getPresentation().setEnabled(enabled);
    }

    private VirtualFile getVirtualFileFromContext(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        if (psiFile == null) {
            return null;
        }
        return psiFile.getVirtualFile();
    }

}
