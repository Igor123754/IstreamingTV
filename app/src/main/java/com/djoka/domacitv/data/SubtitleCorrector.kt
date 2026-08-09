package com.djoka.domacitv.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class SrtBlock(val index: Int, val timing: String, val text: String)

private const val CHUNK_SIZE = 120           // blokova titla po jednom Gemini pozivu
private const val MAX_BLOCKS_TO_CORRECT = 2500 // sigurnosna granica - preko ovoga preskacemo ispravku

object SubtitleCorrector {

    private val geminiApi = GeminiApi.create()
    private val httpClient = OkHttpClient()

    // Vraca lokalni file:// URI ispravljenog titla, ili null ako bilo sta ne uspe
    // (poziv nikad ne baca - caller uvek moze da padne nazad na originalni remote link)
    suspend fun correctSerbianSubtitle(context: Context, subtitleUrl: String): String? {
        val apiKey = GeminiApi.key()
        if (apiKey.isBlank()) return null

        val rawSrt = downloadText(subtitleUrl) ?: return null
        val blocks = parseSrt(rawSrt)
        if (blocks.isEmpty() || blocks.size > MAX_BLOCKS_TO_CORRECT) return null

        val corrected = HashMap<Int, String>()
        blocks.chunked(CHUNK_SIZE).forEach { chunk ->
            try {
                val fixed = correctChunk(chunk, apiKey)
                corrected.putAll(fixed)
            } catch (e: Exception) {
                // Ovaj deo ostaje neispravljen (koristi se originalni tekst za te blokove) - nastavljamo dalje
            }
        }

        if (corrected.isEmpty()) return null // nista se nije popravilo - nema smisla praviti novi fajl

        val finalBlocks = blocks.map { block ->
            block.copy(text = corrected[block.index] ?: block.text)
        }

        return try {
            val file = File(context.cacheDir, "titl_ispravljen_${System.currentTimeMillis()}.srt")
            file.writeText(buildSrt(finalBlocks))
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            null
        }
    }

    // Procenjuje ukupno trajanje titla (kraj poslednjeg bloka) - koristi se da automatski
    // izaberemo kandidata ciji titl najbolje odgovara stvarnom trajanju filma, bez ikakve akcije korisnika.
    suspend fun peekDurationMs(url: String): Long? {
        val raw = downloadText(url) ?: return null
        val blocks = parseSrt(raw)
        val lastBlock = blocks.maxByOrNull { it.index } ?: return null
        val endPart = lastBlock.timing.split("-->").getOrNull(1)?.trim() ?: return null
        return parseTimestampToMs(endPart)
    }

    private fun parseTimestampToMs(ts: String): Long? {
        val match = Regex("""(\d+):(\d+):(\d+)[,.](\d+)""").find(ts.trim()) ?: return null
        val (h, m, s, ms) = match.destructured
        return h.toLong() * 3600000 + m.toLong() * 60000 + s.toLong() * 1000 + ms.toLong()
    }

    private suspend fun correctChunk(chunk: List<SrtBlock>, apiKey: String): Map<Int, String> {
        val prompt = buildString {
            append(
                "Ispravi gramatiku, smisao i doslednost prevoda ovih linija filmskog/serijskog titla na srpskom jeziku. " +
                "NE dodaj i ne oduzimaj linije, NE menjaj brojeve, NE dodaj objasnjenja - vrati SAMO ispravljene linije " +
                "u istom formatu 'broj) tekst', jedna po jedna, tacno onoliko linija koliko je poslato:\n\n"
            )
            chunk.forEach { block ->
                append("${block.index}) ${block.text.replace("\n", " / ")}\n")
            }
        }

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
            generationConfig = GeminiGenerationConfig(maxOutputTokens = 4096, temperature = 0.2)
        )

        val response = geminiApi.generateContent(apiKey, request)
        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return emptyMap()

        val result = HashMap<Int, String>()
        val lineRegex = Regex("""^(\d+)\)\s?(.*)$""")
        text.lines().forEach { line ->
            val match = lineRegex.find(line.trim()) ?: return@forEach
            val idx = match.groupValues[1].toIntOrNull() ?: return@forEach
            val fixedText = match.groupValues[2].replace(" / ", "\n").trim()
            if (fixedText.isNotBlank()) result[idx] = fixedText
        }
        return result
    }

    private suspend fun downloadText(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSrt(raw: String): List<SrtBlock> {
        val blocks = mutableListOf<SrtBlock>()
        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
        val rawBlocks = normalized.split(Regex("\n\\s*\n"))

        for (rawBlock in rawBlocks) {
            val lines = rawBlock.trim().lines()
            if (lines.size < 3) continue
            val index = lines[0].trim().toIntOrNull() ?: continue
            val timing = lines[1].trim()
            if (!timing.contains("-->")) continue
            val text = lines.drop(2).joinToString("\n").trim()
            if (text.isBlank()) continue
            blocks.add(SrtBlock(index, timing, text))
        }
        return blocks
    }

    private fun buildSrt(blocks: List<SrtBlock>): String {
        return blocks.joinToString("\n\n") { block ->
            "${block.index}\n${block.timing}\n${block.text}"
        } + "\n"
    }
}
