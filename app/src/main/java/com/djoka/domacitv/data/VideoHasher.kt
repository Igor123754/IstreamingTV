package com.djoka.domacitv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class VideoHash(val hashHex: String, val fileSize: Long)

object VideoHasher {

    private const val CHUNK_SIZE = 65536L // 64KB
    private val httpClient = OkHttpClient()

    // Standardni OpenSubtitles hash algoritam: filesize + zbir 8-bajtnih (long) reci
    // iz prvih i poslednjih 64KB fajla. Radi preko dva mala HTTP Range zahteva.
    suspend fun compute(url: String, headers: Map<String, String>?): VideoHash? = withContext(Dispatchers.IO) {
        try {
            val head = rangeRequest(url, headers, 0, CHUNK_SIZE - 1) ?: return@withContext null
            val fileSize = head.second ?: return@withContext null
            if (fileSize < CHUNK_SIZE * 2) return@withContext null // premali fajl za ovaj algoritam (nece se desiti kod filmova)

            val tailStart = fileSize - CHUNK_SIZE
            val tail = rangeRequest(url, headers, tailStart, fileSize - 1)?.first ?: return@withContext null

            var hash = fileSize
            hash += sumAsLongWords(head.first)
            hash += sumAsLongWords(tail)

            VideoHash(hashHex = "%016x".format(hash), fileSize = fileSize)
        } catch (e: Exception) {
            null
        }
    }

    // Vraca (bajtovi, ukupna velicina fajla ako je poznata iz Content-Range headera)
    private fun rangeRequest(url: String, headers: Map<String, String>?, start: Long, end: Long): Pair<ByteArray, Long?>? {
        val builder = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$start-$end")
        headers?.forEach { (k, v) -> builder.addHeader(k, v) }

        httpClient.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null

            val totalSize = response.header("Content-Range")
                ?.substringAfter("/")
                ?.trim()
                ?.toLongOrNull()

            return bytes to totalSize
        }
    }

    private fun sumAsLongWords(bytes: ByteArray): Long {
        var sum = 0L
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        while (buffer.remaining() >= 8) {
            sum += buffer.long
        }
        return sum
    }
}
