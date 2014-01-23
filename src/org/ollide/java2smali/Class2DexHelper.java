package org.ollide.java2smali;

import com.android.dx.command.dexer.Main;

import java.io.IOException;

public class Class2DexHelper {

    /**
     * Uses the dx tool from the Android Build Tools (19.0.1) to create
     * a .dex version of a compiled java file (.class)
     *
     * @param inputClassFilePath full path to the compiled .class file
     * @param outputDexPath this will be the dex output file's path and name
     * @throws IOException
     */
    public static void dexClassFile(String inputClassFilePath, String outputDexPath) throws IOException {
        Main.Arguments arguments = new Main.Arguments();
        arguments.outName = outputDexPath;
        arguments.strictNameCheck = false;
        arguments.fileNames = new String[]{inputClassFilePath};

        Main.run(arguments);
    }
}
