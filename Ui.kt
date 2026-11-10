package com.vamps.music

import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

internal enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    SEARCH("Search", Icons.Rounded.Search),
    LIBRARY("Library", Icons.Rounded.LibraryMusic),
    CONNECT("Connect", Icons.Rounded.Link),
    PROFILE("Profile", Icons.Rounded.Person),
}

/** Press feedback (gentle scale) + click, without the default ripple. */
@Composable
internal fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = Glass,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Line),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SectionHeading(label: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Dim, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        if (trailing != null) Text(trailing, color = Dim, fontSize = 11.sp)
    }
}

@Composable
internal fun EmptyCard(title: String, body: String, icon: ImageVector, action: (@Composable () -> Unit)? = null) {
    Surface(
        color = Glass,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Sub, modifier = Modifier.size(30.dp))
            Text(
                title,
                color = Txt,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                body,
                color = Sub,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (action != null) {
                Spacer(Modifier.height(14.dp))
                action()
            }
        }
    }
}

@Composable
internal fun PrimaryButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun GhostButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Txt),
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Elevated else Color.Transparent, label = "chipBg")
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (selected) Accent else Line),
        modifier = Modifier.pressable(onClick),
    ) {
        Text(
            label,
            color = if (selected) Txt else Sub,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

private val artCache = LruCache<String, ImageBitmap>(96)

/** Embedded/album artwork via MediaStore thumbnails (API 29+). Falls back to a neutral placeholder. */
@Composable
internal fun Artwork(uri: Uri?, px: Int, modifier: Modifier, iconSize: Dp = 22.dp) {
    val ctx = LocalContext.current
    val cacheKey = "$uri@$px"
    val bitmap by produceState<ImageBitmap?>(initialValue = artCache.get(cacheKey), cacheKey) {
        if (value == null && uri != null && Build.VERSION.SDK_INT >= 29) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.loadThumbnail(uri, Size(px, px), null).asImageBitmap() }.getOrNull()
            }
            if (loaded != null) {
                artCache.put(cacheKey, loaded)
                value = loaded
            }
        }
    }
    Box(
        modifier.background(Brush.linearGradient(listOf(Color(0xFF3A404B), Color(0xFF171A20)))),
        contentAlignment = Alignment.Center,
    ) {
        val b = bitmap
        if (b != null) {
            Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = Accent, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
internal fun TrackRow(
    track: Track,
    number: Int?,
    active: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pressable(onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Text(number.toString().padStart(2, '0'), color = Dim, fontSize = 12.sp, modifier = Modifier.width(28.dp))
        }
        Artwork(track.uri, 128, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                track.title,
                color = if (active) Accent else Txt,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${track.artist}  •  ${track.album}",
                color = Sub,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(formatTime(track.duration), color = Dim, fontSize = 11.sp)
        if (trailing != null) trailing()
    }
}

@Composable
internal fun FeatureCard(title: String, sub: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = Glass,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Line),
        modifier = modifier.pressable(onClick),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(icon, null, tint = Accent)
            Spacer(Modifier.height(14.dp))
            Text(title, color = Txt, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(sub, color = Dim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun SettingRow(title: String, sub: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        color = Glass,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).pressable(onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Accent)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, color = Txt, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, color = Sub, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Dim)
        }
    }
}

@Composable
internal fun TopBar(tab: Tab) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF424954), Color(0xFF171A20)))),
                contentAlignment = Alignment.Center,
            ) {
                Text("V", color = Txt, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Column {
                Text("VAMPS", color = Txt, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.5.sp)
                Text("M U S I C", color = Dim, fontSize = 9.sp, letterSpacing = 2.sp)
            }
        }
        Text(
            tab.label.uppercase(),
            color = Dim,
            fontSize = 11.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun FloatingNav(selected: Tab, onSelect: (Tab) -> Unit) {
    Surface(
        color = Glass.copy(alpha = 0.96f),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, Line),
        shadowElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Tab.entries.forEach { item ->
                NavItem(item, item == selected, Modifier.weight(1f)) { onSelect(item) }
            }
        }
    }
}

@Composable
private fun NavItem(tab: Tab, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (active) Color.White.copy(alpha = 0.10f) else Color.Transparent, label = "navBg")
    val tint by animateColorAsState(if (active) Txt else Dim, label = "navTint")
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .pressable(onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(tab.icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            tab.label,
            color = tint,
            fontSize = 11.sp,
            maxLines = 1,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
internal fun MiniPlayer(
    np: NowPlaying,
    track: Track?,
    pos: Long,
    dur: Long,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    val fraction = if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Surface(
        color = Elevated.copy(alpha = 0.97f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Line),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -40f) onOpen() },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
            .clickable(onClick = onOpen),
    ) {
        Column {
            Row(
                Modifier.padding(start = 10.dp, end = 2.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(track?.uri, 128, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        np.title.ifBlank { "Unknown track" },
                        color = Txt,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        np.artist.ifBlank { "Unknown artist" },
                        color = Sub,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (np.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (np.playing) "Pause" else "Play",
                        tint = Txt,
                    )
                }
                IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, "Next track", tint = Sub) }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Line)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxSize().background(Accent))
            }
        }
    }
}

// ---------------------------------------------------------------- dialogs

@Composable
internal fun NoticeDialog(notice: Notice, dismiss: () -> Unit) {
    val dismissButton: (@Composable () -> Unit)? = if (notice.action != null) {
        { TextButton(onClick = dismiss) { Text("Not now", color = Sub) } }
    } else {
        null
    }
    AlertDialog(
        onDismissRequest = dismiss,
        containerColor = Glass,
        title = { Text(notice.title, color = Txt) },
        text = { Text(notice.body, color = Sub, lineHeight = 20.sp) },
        confirmButton = {
            TextButton(onClick = {
                notice.action?.invoke()
                dismiss()
            }) { Text(notice.actionLabel ?: "Got it", color = Accent) }
        },
        dismissButton = dismissButton,
    )
}

@Composable
internal fun SleepDialog(activeLabel: String?, dismiss: () -> Unit, pick: (Int) -> Unit) {
    val options = listOf(
        0 to "Off",
        -1 to "End of current track",
        5 to "5 minutes",
        10 to "10 minutes",
        15 to "15 minutes",
        30 to "30 minutes",
        45 to "45 minutes",
        60 to "60 minutes",
        90 to "90 minutes",
    )
    AlertDialog(
        onDismissRequest = dismiss,
        containerColor = Glass,
        title = { Text("Sleep timer", color = Txt) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (activeLabel != null) {
                    Text(activeLabel, color = Good, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                options.forEach { (minutes, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { pick(minutes) }
                            .padding(vertical = 13.dp, horizontal = 6.dp),
                    ) { Text(label, color = Txt, fontSize = 15.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text("Close", color = Accent) } },
    )
}

@Composable
internal fun QueueDialog(controller: MediaController?, np: NowPlaying, dismiss: () -> Unit) {
    val items = remember(np.queueSize, np.mediaId, np.index) {
        val c = controller
        if (c == null) emptyList<MediaItem>() else (0 until c.mediaItemCount).map { c.getMediaItemAt(it) }
    }
    AlertDialog(
        onDismissRequest = dismiss,
        containerColor = Glass,
        title = { Text("Up next", color = Txt) },
        text = {
            if (items.isEmpty()) {
                Text("Your queue is empty.", color = Sub)
            } else {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    itemsIndexed(items) { i, item ->
                        val current = i == np.index
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { controller?.seekToDefaultPosition(i) }
                                .padding(start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${i + 1}", color = Dim, fontSize = 12.sp, modifier = Modifier.width(26.dp))
                            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                Text(
                                    item.mediaMetadata.title?.toString() ?: "Track ${i + 1}",
                                    color = if (current) Accent else Txt,
                                    fontSize = 14.sp,
                                    fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    item.mediaMetadata.artist?.toString().orEmpty(),
                                    color = Sub,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { controller?.removeMediaItem(i) }) {
                                Icon(Icons.Rounded.Close, "Remove from queue", tint = Dim)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text("Done", color = Accent) } },
        dismissButton = {
            if (items.isNotEmpty()) {
                TextButton(onClick = {
                    controller?.clearMediaItems()
                    dismiss()
                }) { Text("Clear", color = Bad) }
            }
        },
    )
}

private fun freqLabel(milliHz: Int): String {
    val hz = milliHz / 1000
    return if (hz >= 1000) String.format(Locale.US, "%.1f kHz", hz / 1000f) else "$hz Hz"
}

/** Controls the android.media.audiofx.Equalizer attached to VAMPS' own playback session only. */
@Composable
internal fun EqualizerDialog(prefs: Prefs, dismiss: () -> Unit) {
    val eq = PlaybackService.equalizer
    AlertDialog(
        onDismissRequest = dismiss,
        containerColor = Glass,
        title = { Text("Equalizer", color = Txt) },
        text = {
            if (eq == null) {
                Text(
                    "The device equalizer is not available right now. It attaches to VAMPS playback once the player service is running, and some devices do not provide it.",
                    color = Sub,
                    lineHeight = 20.sp,
                )
            } else {
                val range = eq.bandLevelRange
                val minLevel = range[0].toInt()
                val maxLevel = range[1].toInt()
                var enabled by remember { mutableStateOf(eq.enabled) }
                val levels = remember {
                    mutableStateListOf<Int>().apply {
                        for (b in 0 until eq.numberOfBands.toInt()) add(eq.getBandLevel(b.toShort()).toInt())
                    }
                }
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enabled", color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Switch(checked = enabled, onCheckedChange = {
                            enabled = it
                            runCatching { eq.setEnabled(it) }
                            prefs.eqEnabled = it
                        })
                    }
                    levels.forEachIndexed { band, level ->
                        Text(
                            "${freqLabel(eq.getCenterFreq(band.toShort()))}   ${String.format(Locale.US, "%+.1f dB", level / 100f)}",
                            color = Sub,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { v ->
                                val l = v.toInt()
                                levels[band] = l
                                runCatching { eq.setBandLevel(band.toShort(), l.toShort()) }
                                prefs.setEqLevel(band, l)
                            },
                            valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Accent,
                                activeTrackColor = Accent,
                                inactiveTrackColor = Line,
                            ),
                        )
                    }
                    TextButton(onClick = {
                        for (b in levels.indices) {
                            levels[b] = 0
                            runCatching { eq.setBandLevel(b.toShort(), 0.toShort()) }
                            prefs.setEqLevel(b, 0)
                        }
                    }) { Text("Reset to flat", color = Accent) }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text("Done", color = Accent) } },
    )
}
