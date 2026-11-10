package com.vamps.music

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private var future: ListenableFuture<MediaController>? = null
    private var controller by mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val f = MediaController.Builder(this, token).buildAsync()
        future = f
        f.addListener({ controller = runCatching { f.get() }.getOrNull() }, ContextCompat.getMainExecutor(this))

        setContent { VampsTheme { VampsApp(controller) } }
    }

    override fun onDestroy() {
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
        super.onDestroy()
    }
}

@Composable
internal fun VampsApp(controller: MediaController?) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { Prefs(ctx) }
    val audioPerm =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val permsToAsk = remember {
        buildList {
            add(audioPerm)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }

    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var libMode by rememberSaveable { mutableStateOf(LibMode.ALL) }
    var searchMode by rememberSaveable { mutableStateOf(SearchMode.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var asked by rememberSaveable { mutableStateOf(false) }

    var library by remember { mutableStateOf(emptyList<Track>()) }
    var scanning by remember { mutableStateOf(false) }
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, audioPerm) == PackageManager.PERMISSION_GRANTED)
    }
    var liked by remember { mutableStateOf(prefs.likedIds()) }
    var recentIds by remember { mutableStateOf(prefs.recent()) }

    var np by remember { mutableStateOf(NowPlaying()) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }

    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<Notice?>(null) }
    var sleepLabel by remember { mutableStateOf<String?>(null) }

    fun scan() {
        if (scanning) return
        scope.launch {
            scanning = true
            val result = withContext(Dispatchers.IO) { loadAudio(ctx) }
            scanning = false
            if (result == null) {
                notice = Notice("Scan failed", "Android could not read the audio library. Try again in a moment.")
            } else {
                library = result
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = ContextCompat.checkSelfPermission(ctx, audioPerm) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            scan()
        } else {
            notice = Notice(
                "Audio access needed",
                "VAMPS needs permission to read audio files so it can show and play the music on this device.",
                "Open settings",
            ) { openAppSettings(ctx) }
        }
    }

    fun requestScan() {
        granted = ContextCompat.checkSelfPermission(ctx, audioPerm) == PackageManager.PERMISSION_GRANTED
        if (granted) scan() else permLauncher.launch(permsToAsk)
    }

    fun playQueue(list: List<Track>, index: Int, shuffle: Boolean = false) {
        val c = controller
        if (c == null) {
            notice = Notice("Player not ready", "The playback service is still starting. Try again in a moment.")
            return
        }
        if (list.isEmpty()) return
        c.shuffleModeEnabled = shuffle
        c.setMediaItems(list.map { it.toMediaItem() }, index.coerceIn(0, list.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying || c.playWhenReady) c.pause() else c.play()
    }

    fun toggleLike(t: Track) {
        liked = if (t.id in liked) liked - t.id else liked + t.id
        prefs.saveLiked(liked)
    }

    // First launch: ask for audio (and notification) permission once, otherwise scan right away.
    LaunchedEffect(Unit) {
        if (granted) {
            scan()
        } else if (!asked) {
            asked = true
            permLauncher.launch(permsToAsk)
        }
    }

    // Real player state, straight from the MediaController.
    DisposableEffect(controller) {
        val c = controller
        if (c == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    np = NowPlaying.from(player)
                }

                override fun onPlayerError(error: PlaybackException) {
                    notice = Notice(
                        "Couldn't play this track",
                        "The file may be missing, protected or in a format this device can't decode (${error.errorCodeName}).",
                    )
                }
            }
            c.addListener(listener)
            np = NowPlaying.from(c)
            onDispose { c.removeListener(listener) }
        }
    }

    val isPlaying = np.playing
    LaunchedEffect(controller, isPlaying, np.mediaId) {
        val c = controller ?: return@LaunchedEffect
        do {
            pos = c.currentPosition.coerceAtLeast(0L)
            dur = if (c.duration != C.TIME_UNSET && c.duration > 0L) c.duration else 0L
            if (isPlaying) delay(250)
        } while (isPlaying && isActive)
    }

    LaunchedEffect(np.mediaId) {
        np.mediaId.toLongOrNull()?.let {
            prefs.pushRecent(it)
            recentIds = prefs.recent()
        }
    }

    LaunchedEffect(np.hasItem) { if (!np.hasItem) playerOpen = false }

    LaunchedEffect(Unit) {
        while (isActive) {
            val end = PlaybackService.sleepEndsAtMs
            sleepLabel = when {
                PlaybackService.stopAfterTrack -> "Stops after this track"
                end > 0L -> "Stops in ${formatTime(end - System.currentTimeMillis())}"
                else -> null
            }
            delay(1000)
        }
    }

    BackHandler(enabled = !playerOpen && tab != Tab.HOME) { tab = Tab.HOME }
    BackHandler(enabled = playerOpen) { playerOpen = false }

    val byId = remember(library) { library.associateBy { it.id } }
    val nowTrack = np.mediaId.toLongOrNull()?.let { byId[it] }
    val activeId = np.mediaId.toLongOrNull()
    val recentTracks = remember(recentIds, byId) { recentIds.mapNotNull { byId[it] } }
    val albumCount = remember(library) { library.map { it.album }.toSet().size }
    val results = remember(library, query, searchMode) { filterTracks(library, query, searchMode) }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(tab)
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "page",
            ) { page ->
                when (page) {
                    Tab.HOME -> HomePage(
                        library = library,
                        recent = recentTracks,
                        favorites = liked.size,
                        albums = albumCount,
                        scanning = scanning,
                        granted = granted,
                        activeId = activeId,
                        onScan = { requestScan() },
                        onPlay = { list, i -> playQueue(list, i) },
                        onShuffle = { if (library.isNotEmpty()) playQueue(library, Random.nextInt(library.size), shuffle = true) },
                        onOpenLibrary = { mode ->
                            libMode = mode
                            tab = Tab.LIBRARY
                        },
                    )
                    Tab.SEARCH -> SearchPage(
                        query = query,
                        onQuery = { query = it },
                        mode = searchMode,
                        onMode = { searchMode = it },
                        results = results,
                        total = library.size,
                        activeId = activeId,
                        onPlay = { list, i -> playQueue(list, i) },
                    )
                    Tab.LIBRARY -> LibraryPage(
                        tracks = library,
                        liked = liked,
                        mode = libMode,
                        onMode = { libMode = it },
                        scanning = scanning,
                        granted = granted,
                        activeId = activeId,
                        onScan = { requestScan() },
                        onPlay = { list, i -> playQueue(list, i) },
                        onLike = { toggleLike(it) },
                    )
                    Tab.CONNECT -> ConnectPage()
                    Tab.PROFILE -> ProfilePage(
                        sleepLabel = sleepLabel,
                        historyCount = recentIds.size,
                        favoriteCount = liked.size,
                        onSleep = { showSleep = true },
                        onEq = { showEq = true },
                        onInfo = { title, body -> notice = Notice(title, body) },
                        onClearHistory = {
                            prefs.clearRecent()
                            recentIds = emptyList()
                            notice = Notice("History cleared", "Your listening history on this device has been removed.")
                        },
                        onClearFavorites = {
                            liked = emptySet()
                            prefs.saveLiked(liked)
                            notice = Notice("Favorites cleared", "Your saved favorites on this device have been removed.")
                        },
                        onSettings = { openAppSettings(ctx) },
                    )
                }
            }
            if (np.hasItem) {
                MiniPlayer(
                    np = np,
                    track = nowTrack,
                    pos = pos,
                    dur = dur,
                    onOpen = { playerOpen = true },
                    onToggle = { togglePlay() },
                    onNext = { controller?.seekToNext() },
                )
            }
            FloatingNav(tab) { tab = it }
        }

        AnimatedVisibility(
            visible = playerOpen,
            enter = slideInVertically(tween(280)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        ) {
            PlayerPage(
                np = np,
                track = nowTrack,
                pos = pos,
                dur = dur,
                liked = nowTrack?.let { it.id in liked } ?: false,
                onClose = { playerOpen = false },
                onToggle = { togglePlay() },
                onPrev = { controller?.seekToPrevious() },
                onNext = { controller?.seekToNext() },
                onSeek = { ms ->
                    controller?.seekTo(ms)
                    pos = ms
                },
                onShuffle = { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } },
                onRepeat = {
                    controller?.let {
                        it.repeatMode = when (it.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                    }
                },
                onLike = { nowTrack?.let { toggleLike(it) } },
                onQueue = { showQueue = true },
                onSleep = { showSleep = true },
                onLyrics = {
                    notice = Notice(
                        "Lyrics unavailable",
                        "No licensed lyrics provider is connected, so VAMPS shows no lyrics rather than unlicensed text.",
                    )
                },
                onEq = { showEq = true },
            )
        }

        if (showSleep) {
            SleepDialog(sleepLabel, { showSleep = false }) { minutes ->
                showSleep = false
                if (!PlaybackService.setSleep(minutes)) {
                    notice = Notice("Timer unavailable", "The playback service isn't running yet. Start a track and try again.")
                }
            }
        }
        if (showQueue) QueueDialog(controller, np) { showQueue = false }
        if (showEq) EqualizerDialog(prefs) { showEq = false }
        notice?.let { n -> NoticeDialog(n) { notice = null } }
    }
}
