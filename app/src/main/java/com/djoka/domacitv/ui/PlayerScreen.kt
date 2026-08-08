package com.djoka.domacitv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.djoka.domacitv.data.PlaybackQueue
import com.djoka.domacitv.data.StreamCandidate

private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

@UnstableApi
private fun buildPlayer(
    context: android.content.Context,
    candidate: StreamCandidate,
    onError: () -> Unit
): ExoPlayer {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
        setUserAgent(candidate.headers?.get("User-Agent") ?: candidate.headers?.get("user-agent") ?: BROWSER_USER_AGENT)
        if (!candidate.headers.isNullOrEmpty()) {
            setDefaultRequestProperties(candidate.headers)
        }
        setAllowCrossProtocolRedirects(true)
    }
    val mediaSourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))

    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(30_000, 90_000, 3_000, 5_000)
        .build()

    // Kad video ima vise audio zapisa, uvek prvo probaj engleski (ako postoji)
    val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(
            buildUponParameters().setPreferredAudioLanguages("eng", "en")
        )
    }

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .setTrackSelector(trackSelector)
        .build().apply {
            setSeekParameters(SeekParameters.CLOSEST_SYNC)
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // Ne prikazuj gresku korisniku - tiho probaj sledeci link u pozadini
                    onError()
                }
            })
            setMediaItem(MediaItem.fromUri(candidate.url))
            prepare()
            playWhenReady = true
        }
}

@UnstableApi
@Composable
fun PlayerScreen() {
    val context = LocalContext.current

    // Pokupi sve kandidate jednom, "potrosi" red - ne ostaje za sledece pustanje
    val candidates = remember {
        val list = PlaybackQueue.candidates
        PlaybackQueue.candidates = emptyList()
        list
    }

    var currentIndex by remember { mutableStateOf(0) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var allFailed by remember { mutableStateOf(false) }

    LaunchedEffect(currentIndex) {
        if (candidates.isEmpty()) {
            allFailed = true
            return@LaunchedEffect
        }
        if (currentIndex >= candidates.size) {
            allFailed = true
            player?.release()
            player = null
            return@LaunchedEffect
        }
        isBuffering = true
        player?.release()
        player = buildPlayer(context, candidates[currentIndex]) {
            // Ovaj link ne radi - tiho predji na sledeci, u pozadini
            currentIndex += 1
        }
    }

    // Prati stanje aktivnog plejera (buffering indikator)
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
        }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (allFailed) {
            Text(
                text = "Nijedan link trenutno ne radi za ovaj naslov.\nPokušaj ponovo malo kasnije.",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
            return@Box
        }

        player?.let { p ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        this.player = p
                    }
                },
                update = { it.player = p },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isBuffering) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}
