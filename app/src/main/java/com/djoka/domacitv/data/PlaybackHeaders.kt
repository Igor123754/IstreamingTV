package com.djoka.domacitv.data

// Jedan kandidat za puštanje - link + eventualni custom HTTP headeri (ok.ru/doodstream i sl. ih zahtevaju)
data class StreamCandidate(
    val url: String,
    val headers: Map<String, String>? = null
)

// Cela lista kandidata (svi mirror-i sa oba addon-a) - Navigation Compose ne prenosi lako liste kroz
// rutu, pa je ostavljamo ovde tik pre navigacije ka plejeru. Plejer je pokupi jednom i "potrosi" je -
// on sam prelazi na sledeci kandidat ako trenutni ne uspe da se pusti, bez ikakvog pitanja korisniku.
object PlaybackQueue {
    var candidates: List<StreamCandidate> = emptyList()
}
