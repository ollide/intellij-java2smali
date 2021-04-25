package org.ollide.java2smali.log

import com.intellij.openapi.diagnostic.Logger
import org.apache.log4j.Level
import java.io.OutputStream

/**
 * Wraps IntelliJ's [com.intellij.openapi.diagnostic.Logger] as an [java.io.OutputStream].
 */
class LogOutputStream(private val logger: Logger, private val level: Level) : OutputStream() {

    private var msg = ""

    override fun write(b: Int) {
        val bytes = ByteArray(1)
        bytes[0] = (b and 0xff).toByte()
        msg += String(bytes)
        if (msg.endsWith("\n")) {
            msg = msg.substring(0, msg.length - 1)
            flush()
        }
    }

    override fun flush() {
        if (level === Level.ERROR || level === Level.FATAL) {
            logger.error(msg)
        } else {
            logger.info(msg)
        }
        msg = ""
    }
}
