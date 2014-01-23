package org.ollide.java2smali;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;

import java.io.File;
import java.io.IOException;

public class GenerateAction extends AnAction {

    private static final String CLASS_EXTENSION = ".class";
    private static final String DEX_EXTENSION = ".dex";
    private static final String JAVA_EXTENSION = ".java";
    private static final String SMALI_EXTENSION = ".smali";

    public void actionPerformed(AnActionEvent e) {
        PsiJavaFile javaFile = (PsiJavaFile) getPsiClassFromContext(e).getContainingFile();

        Project p = e.getProject();
        Module module = ProjectRootManager.getInstance(p).getFileIndex().getModuleForFile(javaFile.getVirtualFile());

        // Compile the javaFile's module
        CompilerCallback compilerCallback = new CompilerCallback(javaFile);
        CompilerManager.getInstance(p).compile(module, compilerCallback);
    }

    @Override
    public void update(AnActionEvent e) {
        PsiClass psiClass = getPsiClassFromContext(e);
        e.getPresentation().setEnabled(psiClass != null && psiClass.getContainingFile() instanceof PsiJavaFile);
    }

    private PsiClass getPsiClassFromContext(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        if (psiFile == null || editor == null) {
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement elementAt = psiFile.findElementAt(offset);
        return PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);
    }

    private static VirtualFile getSourceRootFile(PsiFile file) {
        return ProjectRootManager.getInstance(file.getProject()).getFileIndex().getSourceRootForFile(file.getVirtualFile());
    }

    /**
     * This is the Callback class which is called when the module has been compiled.
     */
    private static class CompilerCallback implements CompileStatusNotification {

        private final PsiJavaFile javaFile;

        public CompilerCallback(PsiJavaFile file) {
            this.javaFile = file;
        }

        public void finished(boolean b, int i, int i2, CompileContext compileContext) {
            VirtualFile[] outputDirectories = compileContext.getAllOutputDirectories();
            if (outputDirectories != null && outputDirectories.length > 0) {
                String compileDirPath = outputDirectories[0].getPath();
                String compiledFilePath = getCompiledClassFilePath(compileDirPath);

                String dexFile = compiledFilePath + DEX_EXTENSION;
                // CLASS -> DEX
                try {
                    Class2DexHelper.dexClassFile(compiledFilePath, dexFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                // DEX -> SMALI
                String outputDir = getSourceRootFile(javaFile).getPath();
                try {
                    Dex2SmaliHelper.disassembleDexFile(dexFile, outputDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                // we've created the smali file in our source file's directory
                // refresh directory synchronously to let IDEA detect the file
                javaFile.getVirtualFile().getParent().refresh(false, false);

                // get a VirtualFile by the IO path
                String smaliPath = javaFile.getVirtualFile().getPath().replace(JAVA_EXTENSION, SMALI_EXTENSION);
                VirtualFile virtualDexFile = LocalFileSystem.getInstance().findFileByIoFile(new File(smaliPath));
                if (virtualDexFile == null) {
                    // create smali file failed
                    return;
                }

                // use the VirtualFile to show the smali file in IDEA editor
                OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(javaFile.getProject(), virtualDexFile);
                openFileDescriptor.navigate(true);
            }
        }

        private String getCompiledClassFilePath(String dirPath) {
            String packageName = javaFile.getPackageName();
            String[] packages = packageName.split("\\.");

            StringBuilder sb = new StringBuilder(dirPath);
            for (String p : packages) {
                sb.append(File.separator);
                sb.append(p);
            }
            sb.append(File.separator);
            sb.append(javaFile.getContainingFile().getVirtualFile().getNameWithoutExtension());
            sb.append(CLASS_EXTENSION);
            return sb.toString();
        }
    }
}
