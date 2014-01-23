package org.ollide.java2smali;

import org.jf.baksmali.baksmali;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import java.io.File;
import java.io.IOException;

public class Dex2SmaliHelper {

    /**
     * Uses baksmali, an disassembler for Android's dex format.
     * Source code and more information: https://bitbucket.org/JesusFreke/smali/overview
     *
     * @param dexFilePath
     * @param outputDir
     * @throws IOException
     */
    public static void disassembleDexFile(String dexFilePath, String outputDir) throws IOException {
        DexBackedDexFile dexBackedDexFile = DexFileFactory.loadDexFile(new File(dexFilePath), 19);
        baksmaliOptions options = new baksmaliOptions();
        options.outputDirectory = outputDir;

        // default value -1 will lead to an exception
        // this setup is copied from Baksmali project
        options.jobs = Runtime.getRuntime().availableProcessors();
        if (options.jobs > 6) {
            options.jobs = 6;
        }

        baksmali.disassembleDexFile(dexBackedDexFile, options);
    }
}
