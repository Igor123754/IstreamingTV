package com.djoka.domacitv.data

// Jedan kandidat za puštanje - link + eventualni custom HTTP headeri (ok.ru/doodstream i sl. ih zahtevaju)
data class StreamCandidate(
    val url: String,
    val headers: Map<String, String>? = null
)

// Jedan ponudjeni titl - moze ih biti vise za isti naslov (razliciti upload-i), korisnik bira u plejeru
data class SubtitleTrackInfo(
    val url: String,
    val label: String
)

// Cela lista kandidata (svi mirror-i sa oba addon-a) i titlova - Navigation Compose ne prenosi lako
// liste kroz rutu, pa ih ostavljamo ovde tik pre navigacije ka plejeru. Plejer ih pokupi jednom i
// "potrosi" red - on sam prelazi na sledeci kandidat ako trenutni ne uspe da se pusti.
object PlaybackQueue {
    var candidates: List<StreamCandidate> = emptyList()
    var subtitles: List<SubtitleTrackInfo> = emptyList()
}
