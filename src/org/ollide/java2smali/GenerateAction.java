package org.ollide.java2smali;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;

import java.io.File;
import java.io.IOException;

public class GenerateAction extends AnAction {

    private static final String CLASS_EXTENSION = ".class";
    private static final String DEX_EXTENSION = ".dex";
    private static final String JAVA_EXTENSION = ".java";
    private static final String SMALI_EXTENSION = ".smali";

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

    private static VirtualFile getSourceRootFile(PsiFile file) {
        return ProjectRootManager.getInstance(file.getProject()).getFileIndex().getSourceRootForFile(file.getVirtualFile());
    }

    /**
     * This is the Callback class which is called when the module has been compiled.
     */
    private static class CompilerCallback implements CompileStatusNotification {

        private final Module module;
        private final PsiJavaFile javaFile;

        public CompilerCallback(Module module, PsiJavaFile file) {
            this.module = module;
            this.javaFile = file;
        }

        public void finished(boolean b, int i, int i2, CompileContext compileContext) {
            VirtualFile outputDirectory = compileContext.getModuleOutputDirectory(module);
            String compileDirPath = outputDirectory.getPath();
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
