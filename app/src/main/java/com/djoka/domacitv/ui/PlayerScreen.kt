package com.djoka.domacitv.ui

import android.net.Uri
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
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
import com.djoka.domacitv.data.SubtitleTrackInfo

private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

@UnstableApi
private fun buildSubtitleConfig(track: SubtitleTrackInfo, isDefault: Boolean): MediaItem.SubtitleConfiguration {
    val mimeType = if (track.url.endsWith(".vtt", ignoreCase = true)) {
        MimeTypes.TEXT_VTT
    } else {
        MimeTypes.APPLICATION_SUBRIP
    }
    return MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
        .setMimeType(mimeType)
        .setLanguage(if (track.label.startsWith("Srpski")) "sr" else "en")
        .setLabel(track.label)
        .setSelectionFlags(if (isDefault) C.SELECTION_FLAG_DEFAULT else 0)
        .build()
}

@UnstableApi
private fun buildPlayer(
    context: android.content.Context,
    candidate: StreamCandidate,
    subtitleTracks: List<SubtitleTrackInfo>,
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

    // Kad video ima vise audio zapisa, uvek prvo probaj engleski (ako postoji).
    // Za titl: uvek prednost nasem spoljasnjem srpskom nad bilo kojim ugradjenim (npr. engleskim) trackom.
    val baseTrackParams = androidx.media3.exoplayer.trackselection.DefaultTrackSelector.Parameters.Builder(context)
        .setPreferredAudioLanguages("eng", "en")
        .setPreferredTextLanguage("sr")
        .setSelectUndeterminedTextLanguage(false)
        .build()
    val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(baseTrackParams)
    }

    val mediaItemBuilder = MediaItem.Builder().setUri(candidate.url)
    if (subtitleTracks.isNotEmpty()) {
        val configs = subtitleTracks.mapIndexed { i, track -> buildSubtitleConfig(track, isDefault = i == 0) }
        mediaItemBuilder.setSubtitleConfigurations(configs)
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

                // Cim se saznaju svi track-ovi (ukljucujuci ugradjene titlove iz samog fajla),
                // ugasi sve tekstualne track-ove OSIM naseg srpskog - u meniju plejera ostaje
                // samo "Srpski" i "Nista", bez engleskog ili bilo kog ugradjenog jezika.
                override fun onTracksChanged(tracks: Tracks) {
                    var params = baseTrackParams.buildUpon()
                    var changed = false
                    for (group in tracks.groups) {
                        if (group.type != C.TRACK_TYPE_TEXT) continue
                        var isOurSerbian = false
                        for (i in 0 until group.length) {
                            if (group.getTrackFormat(i).label == "Srpski") {
                                isOurSerbian = true
                                break
                            }
                        }
                        if (!isOurSerbian) {
                            params = params.addOverride(
                                TrackSelectionOverride(group.mediaTrackGroup, emptyList())
                            )
                            changed = true
                        }
                    }
                    if (changed) {
                        trackSelectionParameters = params.build()
                    }
                }
            })
            setMediaItem(mediaItemBuilder.build())
            prepare()
            playWhenReady = true
        }
}

@UnstableApi
@Composable
fun PlayerScreen() {
    val context = LocalContext.current

    // Pokupi sve kandidate i titlove jednom, "potrosi" red - ne ostaje za sledece pustanje
    val candidates = remember {
        val list = PlaybackQueue.candidates
        PlaybackQueue.candidates = emptyList()
        list
    }
    val subtitleTracks = remember {
        val list = PlaybackQueue.subtitles
        PlaybackQueue.subtitles = emptyList()
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
        player = buildPlayer(context, candidates[currentIndex], subtitleTracks) {
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
                        setShowSubtitleButton(true)
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
