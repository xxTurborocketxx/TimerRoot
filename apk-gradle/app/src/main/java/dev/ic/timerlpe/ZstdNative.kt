package dev.ic.timerlpe

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * REPLACE_ZSTD (payload op 14) decoder backed by the bundled native
 * `libextract.so --zstd-decode`, ported from YuKongA/ghostlock-app's
 * ZstdDecoder.kt (Apache-2.0).
 *
 * The Java-side decoder (aircompressor) is built on sun.misc.Unsafe.  Android
 * 13+ no longer exposes the object-based copyMemory overload, so decompressing
 * a full OTA payload died with:
 *
 *   NoSuchMethodError: No virtual method copyMemory(Object, long, Object,
 *   long, long) in class Lsun/misc/Unsafe
 *
 * The native binary links the Rust zstd crate and never touches Unsafe.  It is
 * staged in jniLibs so the installer extracts it to nativeLibraryDir, which is
 * executable (payload assets in filesDir are not).
 */
object ZstdNative {
    @Volatile private var binary: File? = null
    @Volatile private var workDir: File? = null

    fun init(nativeLibraryDir: File, cacheDir: File) {
        val f = File(nativeLibraryDir, "libextract.so")
        if (f.isFile) {
            binary = f
            workDir = cacheDir
        }
    }

    fun available(): Boolean = binary?.isFile == true && workDir?.isDirectory == true

    /**
     * Decodes one zstd op blob.  [expected] is the destination-extent size;
     * the native side fails closed when the decoded size differs, and so do we.
     */
    fun decode(input: ByteArray, expected: Int): ByteArray {
        val bin = binary ?: throw IOException("Built-in zstd decoder missing (libextract.so)")
        val dir = workDir ?: throw IOException("Cache directory unavailable")
        if (input.isEmpty()) throw IOException("zstd data is empty")
        if (expected <= 0) throw IOException("zstd output size unknown")

        val inFile = File.createTempFile("ic_zst_in", ".bin", dir)
        val outFile = File.createTempFile("ic_zst_out", ".bin", dir)
        try {
            inFile.writeBytes(input)
            val process = ProcessBuilder(
                bin.absolutePath, "--zstd-decode",
                inFile.absolutePath, outFile.absolutePath, expected.toString(),
            ).redirectErrorStream(true).start()
            /* Wait first: reading the stream to EOF before waitFor would hang
             * the timeout forever if the native side wedges. */
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                throw IOException("zstd decode timed out")
            }
            val diagnostics = try {
                process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: Exception) {
                ""
            }
            if (process.exitValue() != 0) {
                throw IOException(
                    "zstd decode failed (exit=${process.exitValue()}): ${diagnostics.take(200)}"
                )
            }
            val out = outFile.readBytes()
            if (out.size != expected) {
                throw IOException("zstd output size mismatch: expected $expected, got ${out.size}")
            }
            return out
        } finally {
            inFile.delete()
            outFile.delete()
        }
    }
}
