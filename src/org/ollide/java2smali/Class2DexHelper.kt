package org.ollide.java2smali

import com.android.dx.command.dexer.Main

import java.io.IOException

object Class2DexHelper {

    /**
     * Uses the dx tool from the Android Build Tools (19.0.1) to create
     * a .dex version of a compiled java file (.class)
     *
     * @param inputClassFilePaths full paths to the compiled .class file
     * @param outputDexPath this will be the dex output file's path and name
     * @throws IOException
     */
    @Throws(IOException::class)
    fun dexClassFile(inputClassFilePaths: Array<String>, outputDexPath: String) {
        val arguments = Main.Arguments()
        arguments.outName = outputDexPath
        arguments.strictNameCheck = false
        arguments.fileNames = inputClassFilePaths

        Main.run(arguments)
    }
}
