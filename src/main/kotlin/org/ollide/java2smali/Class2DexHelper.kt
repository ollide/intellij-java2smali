package org.ollide.java2smali

import com.android.dx.command.dexer.DxContext
import com.android.dx.command.dexer.Main
import com.intellij.openapi.diagnostic.Logger
import org.apache.log4j.Level
import org.ollide.java2smali.log.LogOutputStream

object Class2DexHelper {

    /**
     * Uses the dx tool from the Android Build Tools to create
     * a .dex version of a compiled java file (.class)
     *
     * @param inputClassFilePaths full paths to the compiled .class file
     * @param outputDexPath this will be the dex output file's path and name
     * @return `true` if dx ran successfully, otherwise `false`
     */
    fun dexClassFile(inputClassFilePaths: Array<String>, outputDexPath: String): Boolean {
        val dxContext = DxContext(LogOutputStream(LOG, Level.INFO), LogOutputStream(LOG, Level.ERROR))

        val arguments = Main.Arguments()
        arguments.outName = outputDexPath
        arguments.strictNameCheck = false
        arguments.fileNames = inputClassFilePaths

        return try {
            val returnCode = Main(dxContext).runDx(arguments)
            returnCode == 0
        } catch (e: Exception) {
            LOG.error("Failed to run dx", e)
            false
        }
    }

    private val LOG = Logger.getInstance(Class2DexHelper::class.java)

}
