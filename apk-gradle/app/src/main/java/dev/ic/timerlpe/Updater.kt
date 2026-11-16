package dev.ic.timerlpe

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/* GitHub release plumbing for the in-app updater (v15).  The check and
 * the download run on Dispatchers.IO from MainActivity; everything here
 * is blocking and main-free. */
object Updater {
    const val LATEST_API =
        "https://api.github.com/repos/xxTurborocketxx/ic-timer-lpe/releases/latest"

    class UpdateInfo(val tag: String, val version: String, val url: String, val size: Long, val notes: String)

    /** Fetch the latest release; null means offline, rate-limited or the
     * release has no APK asset. */
    fun fetchLatest(): UpdateInfo? {
        val conn = URL(LATEST_API).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "TimerRoot-UpdateCheck")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(body)
            val tag = obj.optString("tag_name", "")
            if (tag.isEmpty()) return null
            val arr = obj.optJSONArray("assets") ?: return null
            var asset: JSONObject? = null
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                if (a.optString("name", "").endsWith(".apk")) {
                    asset = a
                    break
                }
            }
            asset ?: return null
            val url = asset.optString("browser_download_url", "")
            if (url.isEmpty()) return null
            return UpdateInfo(
                tag = tag,
                version = tag.removePrefix("v"),
                url = url,
                size = asset.optLong("size", 0L),
                notes = plainNotes(obj.optString("body", ""))
            )
        } finally {
            conn.disconnect()
        }
    }

    /** Trim the release body's markdown down to readable plain text. */
    private fun plainNotes(body: String): String {
        val cleaned = body.lines().joinToString("\n") { raw ->
            raw.trim()
                .trimStart { it == '#' }
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
        }.trim()
        return if (cleaned.length > 1200) cleaned.take(1200).trimEnd() + "..." else cleaned
    }

    /** Dotted-numeric comparison: 0.26.0 < 0.26.1 < 0.27.0. */
    fun isNewer(current: String, latest: String): Boolean {
        val a = current.split('.').map { it.toIntOrNull() ?: 0 }
        val b = latest.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return y > x
        }
        return false
    }

    /** Stream the APK into dest; reports progress as (bytes, total). */
    fun download(url: String, dest: File, onProgress: (Long, Long) -> Unit) {
        dest.parentFile?.mkdirs()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "TimerRoot-UpdateCheck")
        try {
            if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        onProgress(done, total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    /** True when the downloaded APK is signed by the same key as the
     * installed app - only then can the system installer update in place
     * without an uninstall and the data wipe that comes with it. */
    fun signaturesMatch(ctx: Context, apk: File): Boolean {
        val pm = ctx.packageManager
        val mine = runCatching {
            val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            (info.signingInfo?.apkContentsSigners ?: info.signingInfo?.signingCertificateHistory)
                ?.map { it.toCharsString() }?.toSet()
        }.getOrNull() ?: return false
        val theirs = runCatching {
            val info = pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                ?: return false
            (info.signingInfo?.apkContentsSigners ?: info.signingInfo?.signingCertificateHistory)
                ?.map { it.toCharsString() }?.toSet()
        }.getOrNull() ?: return false
        return mine == theirs
    }
}
