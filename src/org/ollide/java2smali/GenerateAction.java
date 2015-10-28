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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GenerateAction extends AnAction {

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

        public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
            if (errors > 0) return;
        
            String dexFile = javaFile.getVirtualFile().getNameWithoutExtension() + DEX_EXTENSION;
            // CLASS -> DEX
            try {
                String[] compiledFilePaths = getClassFiles(compileContext);
                Class2DexHelper.dexClassFile(compiledFilePaths, dexFile);
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

        // Mary wishes she had a little lambda
        private String[] getClassFiles(CompileContext context) {
            VirtualFile outDir = context.getModuleOutputDirectory(module);

            PsiClass[] classes = javaFile.getClasses();
            Set<String> classNames = new HashSet<String>(classes.length);
            for (PsiClass c : classes) {
                classNames.add(c.getName());
            }

            String pkg = javaFile.getPackageName().replace('.', '/');

            List<VirtualFile> compiledAsList = Arrays.asList(outDir.findFileByRelativePath(pkg).getChildren());
            List<VirtualFile> compiledChildren = new LinkedList<VirtualFile>(compiledAsList);

            List<VirtualFile> sourceAsList = Arrays.asList(javaFile.getVirtualFile().getParent().getChildren());
            List<VirtualFile> sourceChildren = new LinkedList<VirtualFile>(sourceAsList);

            for (Iterator<VirtualFile> iterCompiled = compiledChildren.iterator(); iterCompiled.hasNext(); ) {
                VirtualFile f = iterCompiled.next();
                String name = f.getNameWithoutExtension();
                if (f.isDirectory() || !f.getExtension().equals("class")) {
                    iterCompiled.remove();
                    continue;
                }

                boolean b = false;
                for (String className : classNames) {
                    if (name.startsWith(className)) {
                        b = true;
                        break;
                    }
                }

                if (!b) {
                    // this isn't part of the file we want
                    iterCompiled.remove();
                    continue;
                }

                // remove classes that start with the class name we want, but aren't actually related
                // example: we have A.java and AB.java, if we j2s A.java then we don't want AB.smali
                for (Iterator<VirtualFile> iterSource = sourceChildren.iterator(); iterSource.hasNext(); ) {
                    VirtualFile sVf = iterSource.next();
                    String sourceName = sVf.getNameWithoutExtension();
                    if (sVf.isDirectory() || classNames.contains(sourceName)) {
                        // don't waste my clock cycles in the future
                        iterSource.remove();
                        continue;
                    }

                    if (name.startsWith(sourceName)) {
                        iterCompiled.remove();
                        iterSource.remove();
                        break;
                    }
                }
            }

            int size = compiledChildren.size();
            String[] compiled = new String[size];
            VirtualFile[] compiledFiles = compiledChildren.toArray(new VirtualFile[size]);
            for (int i = 0; i < size; i++) {
                compiled[i] = compiledFiles[i].getPath();
            }
            return compiled;
        }
    }
}
