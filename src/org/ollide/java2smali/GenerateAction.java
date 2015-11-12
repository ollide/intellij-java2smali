package org.ollide.java2smali;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class GenerateAction extends AnAction {

    private static final String JAVA = "java";
    private static final String KOTLIN = "kt";

    public void actionPerformed(AnActionEvent e) {
        VirtualFile vFile = getVirtualFileFromContext(e);

        Project p = e.getProject();
        Module module = ProjectRootManager.getInstance(p).getFileIndex().getModuleForFile(vFile);
        PsiClassOwner file = (PsiClassOwner) PsiManager.getInstance(p).findFile(vFile);

        // Compile the vFile's module
        CompilerCallback compilerCallback = new CompilerCallback(module, file);
        CompilerManager.getInstance(p).compile(module, compilerCallback);
    }

    @Override
    public void update(AnActionEvent e) {
        boolean enabled = false;

        VirtualFile vFile = getVirtualFileFromContext(e);
        if (vFile != null) {
            String extension = vFile.getFileType().getDefaultExtension();
            Module m = ProjectRootManager.getInstance(e.getProject()).getFileIndex().getModuleForFile(vFile);
            enabled = (JAVA.equals(extension) || KOTLIN.equals(extension)) && m != null;
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
