package com.vamps.music

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player

// ------------------------------------------------------------------ Home

@Composable
internal fun HomePage(
    library: List<Track>,
    recent: List<Track>,
    favorites: Int,
    albums: Int,
    scanning: Boolean,
    granted: Boolean,
    activeId: Long?,
    onScan: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onShuffle: () -> Unit,
    onOpenLibrary: (LibMode) -> Unit,
) {
    val recentlyAdded = remember(library) { library.sortedByDescending { it.addedAt }.take(6) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Your sound,\nwithout limits.",
            color = Txt,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
        )
        Text("A quieter place for everything you listen to.", color = Sub, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(22.dp))
        GlassCard {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("YOUR LIBRARY", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (scanning) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.GraphicEq, null, tint = Sub)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    when {
                        scanning -> "Scanning your device…"
                        library.isEmpty() -> "Start with what you own."
                        else -> "${library.size} tracks, ready when you are."
                    },
                    color = Txt,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Local audio from this device. Works offline.",
                    color = Sub,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (library.isNotEmpty()) PrimaryButton("Shuffle", Icons.Rounded.Shuffle, Modifier.weight(1f), onShuffle)
                    GhostButton(if (granted) "Rescan" else "Allow access", Icons.Rounded.FolderOpen, Modifier.weight(1f), onScan)
                }
            }
        }

        if (recent.isNotEmpty()) {
            SectionHeading("RECENTLY PLAYED", "${recent.size} tracks")
            recent.take(5).forEachIndexed { i, t ->
                TrackRow(t, null, t.id == activeId) { onPlay(recent, i) }
            }
        }

        SectionHeading("RECENTLY ADDED", "${library.size} tracks")
        if (library.isEmpty()) {
            EmptyCard(
                "Your music will appear here",
                if (granted) "No audio files were found. Add music to this device, then rescan." else "Allow audio access to see the music on this device.",
                Icons.Rounded.MusicNote,
            )
        } else {
            recentlyAdded.forEachIndexed { i, t ->
                TrackRow(t, i + 1, t.id == activeId) { onPlay(recentlyAdded, i) }
            }
        }

        SectionHeading("YOUR SPACE")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard("Favorites", "$favorites saved", Icons.Rounded.Favorite, Modifier.weight(1f)) { onOpenLibrary(LibMode.LIKED) }
            FeatureCard("Albums", "$albums on device", Icons.Rounded.Album, Modifier.weight(1f)) { onOpenLibrary(LibMode.ALBUMS) }
        }
    }
}

// ---------------------------------------------------------------- Search

@Composable
internal fun SearchPage(
    query: String,
    onQuery: (String) -> Unit,
    mode: SearchMode,
    onMode: (SearchMode) -> Unit,
    results: List<Track>,
    total: Int,
    activeId: Long?,
    onPlay: (List<Track>, Int) -> Unit,
) {
    val clear: (@Composable () -> Unit)? = if (query.isNotEmpty()) {
        { IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, "Clear search", tint = Sub) } }
    } else {
        null
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Find your next repeat.", color = Txt, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search your local library", color = Dim) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Sub) },
            trailingIcon = clear,
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Line,
                focusedTextColor = Txt,
                unfocusedTextColor = Txt,
                cursorColor = Accent,
            ),
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchMode.entries.forEach { m -> Chip(m.label, mode == m) { onMode(m) } }
        }
        Spacer(Modifier.height(14.dp))
        when {
            query.isBlank() -> EmptyCard(
                "Search $total local tracks",
                "Find songs by title, artist or album. Results come from this device only.",
                Icons.Rounded.Search,
            )
            results.isEmpty() -> EmptyCard("No matches", "Try another title, artist or album.", Icons.Rounded.Search)
            else -> {
                Text("LOCAL RESULTS  •  ${results.size}", color = Dim, fontSize = 11.sp, letterSpacing = 1.7.sp)
                LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp)) {
                    itemsIndexed(results, key = { _, t -> t.id }) { i, t ->
                        TrackRow(t, null, t.id == activeId) { onPlay(results, i) }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------- Library

@Composable
internal fun LibraryPage(
    tracks: List<Track>,
    liked: Set<Long>,
    mode: LibMode,
    onMode: (LibMode) -> Unit,
    scanning: Boolean,
    granted: Boolean,
    activeId: Long?,
    onScan: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onLike: (Track) -> Unit,
) {
    val sorted = remember(tracks) { tracks.sortedBy { it.title.lowercase() } }
    val shown = remember(sorted, liked, mode) { if (mode == LibMode.LIKED) sorted.filter { it.id in liked } else sorted }
    val groups = remember(tracks, mode) {
        val map: Map<String, List<Track>> = when (mode) {
            LibMode.ALBUMS -> tracks.groupBy { it.album }
            LibMode.ARTISTS -> tracks.groupBy { it.artist }
            else -> emptyMap()
        }
        map.toList().sortedBy { it.first.lowercase() }
    }
    val grouped = mode == LibMode.ALBUMS || mode == LibMode.ARTISTS

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your library", color = Txt, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (scanning) "Scanning…" else "${tracks.size} tracks on this device",
                    color = Sub,
                    fontSize = 13.sp,
                )
            }
            GhostButton("Rescan", Icons.Rounded.Refresh, onClick = onScan)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LibMode.entries.forEach { m -> Chip(m.label, mode == m) { onMode(m) } }
        }
        Spacer(Modifier.height(8.dp))
        when {
            tracks.isEmpty() -> EmptyCard(
                "Your library is waiting",
                if (granted) "No audio files were found on this device." else "Allow audio access, then scan your device.",
                Icons.Rounded.LibraryMusic,
            ) { GhostButton(if (granted) "Scan again" else "Allow access", Icons.Rounded.FolderOpen, onClick = onScan) }
            grouped -> LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                items(groups, key = { it.first }) { (name, list) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .pressable { onPlay(list, 0) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(list.first().uri, 128, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(name, color = Txt, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${list.size} tracks", color = Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Rounded.PlayArrow, "Play", tint = Sub)
                    }
                }
            }
            shown.isEmpty() -> EmptyCard("No favorites yet", "Tap the heart on any song to keep it here.", Icons.Rounded.FavoriteBorder)
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                itemsIndexed(shown, key = { _, t -> t.id }) { i, t ->
                    TrackRow(
                        t,
                        null,
                        t.id == activeId,
                        trailing = {
                            IconButton(onClick = { onLike(t) }) {
                                Icon(
                                    if (t.id in liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    if (t.id in liked) "Remove from favorites" else "Add to favorites",
                                    tint = if (t.id in liked) Accent else Dim,
                                )
                            }
                        },
                    ) { onPlay(shown, i) }
                }
            }
        }
    }
}

// --------------------------------------------------------------- Connect

@Composable
internal fun ConnectPage() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        Text("Your services.", color = Txt, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "Provider-owned sign-in and permissions. VAMPS never asks for your passwords.",
            color = Sub,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 5.dp, bottom = 16.dp),
        )
        ProviderCard("Spotify", "Official authorization and Spotify's approved remote playback.", "Needs a registered developer app", "com.spotify.music")
        ProviderCard("YouTube", "Official player and supported content discovery.", "Needs Google developer setup", "com.google.android.youtube")
        ProviderCard("YouTube Music", "App handoff only. There is no public unrestricted music API.", "Handoff only", "com.google.android.apps.youtube.music")
        ProviderCard("SoundCloud", "OAuth 2.1 with PKCE and API-permitted playback.", "Needs a developer application", "com.soundcloud.android")
        GlassCard {
            Text(
                "Provider sign-in is not active in this build. Each integration needs its own registered developer application and approved scopes. These cards open the official apps and never claim an account is connected.",
                color = Sub,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ProviderCard(name: String, description: String, requirement: String, pkg: String) {
    val ctx = LocalContext.current
    val installed = remember(pkg) { ctx.packageManager.getLaunchIntentForPackage(pkg) != null }
    Surface(
        color = Glass,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Elevated),
                    contentAlignment = Alignment.Center,
                ) { Text(name.take(1), color = Txt, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(name, color = Txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("NOT CONNECTED", color = Dim, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(description, color = Sub, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 10.dp))
            Text(requirement, color = Dim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            TextButton(onClick = { openProvider(ctx, pkg) }, modifier = Modifier.padding(top = 4.dp)) {
                Text(if (installed) "Open app" else "Get app", color = Accent)
            }
        }
    }
}

// --------------------------------------------------------------- Profile

@Composable
internal fun ProfilePage(
    sleepLabel: String?,
    historyCount: Int,
    favoriteCount: Int,
    onSleep: () -> Unit,
    onEq: () -> Unit,
    onInfo: (String, String) -> Unit,
    onClearHistory: () -> Unit,
    onClearFavorites: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        Text("Your experience", color = Txt, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Playback tools and app preferences.", color = Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 18.dp))

        SectionHeading("PLAYBACK")
        SettingRow("Sleep timer", sleepLabel ?: "Stop playback automatically", Icons.Rounded.Timer, onSleep)
        SettingRow("Equalizer", "Device equalizer on VAMPS playback", Icons.Rounded.Tune, onEq)
        SettingRow("Gapless playback", "Automatic where files and decoder support it", Icons.Rounded.SkipNext) {
            onInfo(
                "Gapless playback",
                "VAMPS plays your queue with Media3 ExoPlayer, which transitions between tracks without added silence when the files and decoder allow it. There is no separate toggle.",
            )
        }
        SettingRow("Crossfade", "Not available yet", Icons.Rounded.GraphicEq) {
            onInfo(
                "Crossfade",
                "Crossfade needs a two-player transition engine and is not implemented in this build. It will only apply to VAMPS-controlled local playback.",
            )
        }
        SettingRow("Lyrics", "Needs a licensed lyrics source", Icons.Rounded.Subtitles) {
            onInfo(
                "Lyrics",
                "No licensed lyrics provider is connected, so VAMPS shows no lyrics rather than unlicensed text.",
            )
        }

        SectionHeading("LIBRARY & PRIVACY")
        SettingRow("Clear listening history", "$historyCount tracks remembered on this device", Icons.Rounded.History, onClearHistory)
        SettingRow("Clear favorites", "$favoriteCount saved", Icons.Rounded.DeleteSweep, onClearFavorites)
        SettingRow("Audio permission", "Open Android app settings", Icons.Rounded.Settings, onSettings)
        SettingRow("Privacy", "What VAMPS stores", Icons.Rounded.LibraryMusic) {
            onInfo(
                "Privacy",
                "Favorites, listening history and equalizer settings are stored only on this device. This build has no accounts, no analytics and makes no network requests.",
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("VAMPS MUSIC", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
        Text("Version 1.1 • Obsidian", color = Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

// ---------------------------------------------------------------- Player

@Composable
internal fun PlayerPage(
    np: NowPlaying,
    track: Track?,
    pos: Long,
    dur: Long,
    liked: Boolean,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onLike: () -> Unit,
    onQueue: () -> Unit,
    onSleep: () -> Unit,
    onLyrics: () -> Unit,
    onEq: () -> Unit,
) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val fraction = drag ?: if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val shownPos = drag?.let { (it * dur).toLong() } ?: pos

    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = { if (total > 80f) onClose() },
                            onVerticalDrag = { _, dy -> total += dy },
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Close player", tint = Txt) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text("From this device", color = Sub, fontSize = 11.sp)
                }
                IconButton(onClick = onQueue) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Open queue", tint = Txt) }
            }

            Spacer(Modifier.height(12.dp))
            Artwork(
                track?.uri,
                768,
                Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)),
                iconSize = 96.dp,
            )
            Spacer(Modifier.height(26.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        np.title.ifBlank { "Unknown track" },
                        color = Txt,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        np.artist.ifBlank { "Unknown artist" },
                        color = Sub,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                IconButton(onClick = onLike) {
                    Icon(
                        if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (liked) "Remove from favorites" else "Add to favorites",
                        tint = if (liked) Accent else Sub,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Slider(
                value = fraction,
                onValueChange = { drag = it },
                onValueChangeFinished = {
                    drag?.let { onSeek((it * dur).toLong()) }
                    drag = null
                },
                enabled = dur > 0L,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Line),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(shownPos), color = Dim, fontSize = 12.sp)
                Text(formatTime(dur), color = Dim, fontSize = 12.sp)
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShuffle) {
                    Icon(Icons.Rounded.Shuffle, if (np.shuffle) "Shuffle on" else "Shuffle off", tint = if (np.shuffle) Accent else Dim)
                }
                IconButton(onClick = onPrev, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous track", tint = Txt, modifier = Modifier.size(34.dp))
                }
                Surface(
                    color = Accent,
                    shape = CircleShape,
                    modifier = Modifier.size(70.dp).pressable(onToggle),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (np.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (np.playing) "Pause" else "Play",
                            tint = Bg,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Next track", tint = Txt, modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = onRepeat) {
                    val label = when (np.repeat) {
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        else -> "Repeat off"
                    }
                    Icon(
                        if (np.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        label,
                        tint = if (np.repeat == Player.REPEAT_MODE_OFF) Dim else Accent,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PlayerAction("Queue", Icons.AutoMirrored.Rounded.QueueMusic, Modifier.weight(1f), onQueue)
                PlayerAction("Sleep", Icons.Rounded.Timer, Modifier.weight(1f), onSleep)
                PlayerAction("Lyrics", Icons.Rounded.Subtitles, Modifier.weight(1f), onLyrics)
                PlayerAction("EQ", Icons.Rounded.Equalizer, Modifier.weight(1f), onEq)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PlayerAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).pressable(onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = Sub, modifier = Modifier.size(22.dp))
        Text(label, color = Sub, fontSize = 12.sp, maxLines = 1)
    }
}
