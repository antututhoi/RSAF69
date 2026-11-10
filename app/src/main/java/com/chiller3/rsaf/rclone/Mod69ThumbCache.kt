package com.chiller3.rsaf.rclone

import android.content.Context
import android.graphics.Point
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * 69 mod: keeps made thumbnails on the phone (in the app cache), so opening a folder again shows
 * them at once instead of downloading the videos again. A changed file (other size or date) gets
 * a new thumbnail. The cache is trimmed to [MAX_BYTES], oldest first.
 */
object Mod69ThumbCache {
    private const val TAG = "Mod69ThumbCache"
    private const val MAX_BYTES = 300L * 1024 * 1024

    private fun dir(context: Context) = File(context.cacheDir, "mod69_thumbs").apply { mkdirs() }

    fun file(context: Context, documentId: String, size: Long, modTime: Long, hint: Point): File {
        val key = "$documentId|$size|$modTime|${hint.x}x${hint.y}"
        val hash = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir(context), "$hash.png")
    }

    fun save(target: File, data: ByteArray) {
        try {
            val tmp = File(target.parentFile, target.name + ".tmp")
            tmp.writeBytes(data)
            if (!tmp.renameTo(target)) tmp.delete()
            trim(target.parentFile ?: return)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't keep thumbnail", e)
        }
    }

    private fun trim(dir: File) {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_BYTES) return
        for (f in files.sortedBy { it.lastModified() }) {
            if (total <= MAX_BYTES * 8 / 10) break
            total -= f.length()
            f.delete()
        }
    }
}
