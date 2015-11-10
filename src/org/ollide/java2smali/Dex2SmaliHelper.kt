package org.ollide.java2smali

import org.jf.baksmali.baksmali
import org.jf.baksmali.baksmaliOptions
import org.jf.dexlib2.DexFileFactory

import java.io.File
import java.io.IOException

object Dex2SmaliHelper {

    /**
     * Uses baksmali, an disassembler for Android's dex format.
     * Source code and more information: https://bitbucket.org/JesusFreke/smali/overview
     *
     * @param dexFilePath
     * @param outputDir
     * @throws IOException
     */
    @Throws(IOException::class)
    fun disassembleDexFile(dexFilePath: String, outputDir: String) {
        val dexBackedDexFile = DexFileFactory.loadDexFile(File(dexFilePath), 19, false)
        val options = baksmaliOptions()
        options.outputDirectory = outputDir

        // default value -1 will lead to an exception
        // this setup is copied from Baksmali project
        options.jobs = Runtime.getRuntime().availableProcessors()
        if (options.jobs > 6) {
            options.jobs = 6
        }

        baksmali.disassembleDexFile(dexBackedDexFile, options)
    }
}
