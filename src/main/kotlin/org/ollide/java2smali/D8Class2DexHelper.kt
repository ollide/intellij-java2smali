package org.ollide.java2smali

import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.stream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

object D8Class2DexHelper {

    /**
     * Uses the D8 tool from the Android Build Tools to create
     * a .dex version of a compiled java file (.class)
     *
     * @param inputClassFilePaths full paths to the compiled .class file
     * @param outputDexPath this will be the dex output file's path and name
     * @return `true` if dx ran successfully, otherwise `false`
     */
    fun dexClassFile(inputClassFilePaths: Array<String>, outputDexPath: Path): Boolean {
        val classFiles = inputClassFilePaths.stream().map { Paths.get(it) }.collect(Collectors.toList())

        val command = D8Command.builder()
                .setIntermediate(true)
                .setMinApiLevel(30)
                .addProgramFiles(classFiles)
                // Add --library android.jar ?
                // .addLibraryFiles(Paths.get(""))
                .setOutput(outputDexPath, OutputMode.DexIndexed)
                .build()

        return try {
            D8.run(command)
            true
        } catch (e: Exception) {
            LOG.error("Error running D8", e)
            false
        }
    }

    private val LOG = Logger.getInstance(D8Class2DexHelper::class.java)

}
