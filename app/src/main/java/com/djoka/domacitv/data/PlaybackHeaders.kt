package com.djoka.domacitv.data

// Neki hostovi (ok.ru, doodstream...) zahtevaju posebne HTTP headere (User-Agent, Referer) da bi link uopste radio.
// Navigation Compose ne prenosi lako mape kroz rutu, pa ih ostavljamo ovde tik pre nego sto navigiramo ka plejeru -
// plejer ih pokupi jednom i odmah ih "potrosi" (postavi na null).
object PlaybackHeaders {
    var headers: Map<String, String>? = null
}
