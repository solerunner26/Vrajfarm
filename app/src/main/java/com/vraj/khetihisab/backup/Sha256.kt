package com.vraj.khetihisab.backup

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Lowercase hex SHA-256 — the format the server compares with `hash_file('sha256')`. */
object Sha256 {
    fun of(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun of(file: File): String = file.inputStream().use(::of)

    fun of(input: InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            md.update(buf, 0, n)
        }
        return hex(md.digest())
    }

    fun hex(digest: ByteArray): String {
        val out = CharArray(digest.size * 2)
        digest.forEachIndexed { i, b ->
            val v = b.toInt() and 0xFF
            out[i * 2] = HEX[v ushr 4]
            out[i * 2 + 1] = HEX[v and 0x0F]
        }
        return String(out)
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
