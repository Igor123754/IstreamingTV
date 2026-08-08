package com.djoka.domacitv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.djoka.domacitv.data.PlaybackHeaders

private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

@UnstableApi
@Composable
fun PlayerScreen(videoUrl: String) {
    val context = LocalContext.current

    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember {
        // Headers koje je addon vratio za ovaj konkretan link (ok.ru, doodstream i sl. ih zahtevaju).
        // "Potrosimo" ih odmah da se ne bi slucajno iskoristile za sledeci, drugaciji video.
        val customHeaders = PlaybackHeaders.headers
        PlaybackHeaders.headers = null

        val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
            setUserAgent(customHeaders?.get("User-Agent") ?: customHeaders?.get("user-agent") ?: BROWSER_USER_AGENT)
            if (!customHeaders.isNullOrEmpty()) {
                setDefaultRequestProperties(customHeaders)
            }
            setAllowCrossProtocolRedirects(true)
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))

        // Buffer dovoljno velik da apsorbuje varijacije brzine izvora (mp4upload/vidhide/ok.ru znaju da usporavaju)
        // - prevelik minBuffer/premali bufferForPlayback = seckanje, sto je bio prethodni problem
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 90_000,
                /* bufferForPlaybackMs = */ 3_000,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                // Priblizan (brzi) seek umesto tacnog frame-a - premotavanje deluje skoro trenutno
                setSeekParameters(SeekParameters.CLOSEST_SYNC)

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = state == Player.STATE_BUFFERING
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        errorMessage = "${error.errorCodeName}: ${error.message}"
                    }
                })
                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Ne dozvoli da se ekran zakljuca/ugasi dok je plejer otvoren
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (videoUrl.isBlank()) {
            Text(text = "Nema video linka", color = Color.White, modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }

        errorMessage?.let { msg ->
            Text(
                text = "Greška pri puštanju:\n$msg",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        }
    }
}
