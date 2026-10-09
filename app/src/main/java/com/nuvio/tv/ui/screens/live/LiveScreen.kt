package com.nuvio.tv.ui.screens.live

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.ui.theme.NuvioTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveScreen(
    showBuiltInHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf("cricket") }
    var matches by remember { mutableStateOf<List<LiveMatch>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedMatch by remember { mutableStateOf<LiveMatch?>(null) }
    var sources by remember { mutableStateOf<List<LiveStreamSource>>(emptyList()) }
    var sourcesLoading by remember { mutableStateOf(false) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedCategory) {
        loading = true
        selectedMatch = null
        playingUrl = null
        matches = LiveApi.getMatches(selectedCategory)
        loading = false
    }

    // Request focus on first item when matches load (D-pad support)
    LaunchedEffect(matches) {
        if (matches.isNotEmpty()) {
            firstItemFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background)
    ) {
        when {
            playingUrl != null -> {
                // WebView player
                LiveWebViewPlayer(
                    url = playingUrl!!,
                    title = selectedMatch?.title ?: "Live",
                    onBack = { playingUrl = null },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            selectedMatch != null -> {
                // Source picker
                val match = selectedMatch!!
                LaunchedEffect(match) {
                    sourcesLoading = true
                    sources = LiveApi.getSources(match.category, match.id)
                    sourcesLoading = false
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { selectedMatch = null },
                            contentPadding = PaddingValues(12.dp),
                            colors = ButtonDefaults.colors(
                                containerColor = NuvioTheme.colors.SurfaceVariant
                            )
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = match.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = NuvioTheme.colors.TextPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Choose a stream:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NuvioTheme.colors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (sourcesLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading streams...",
                                color = NuvioTheme.colors.TextSecondary,
                            )
                        }
                    } else if (sources.isEmpty()) {
                        Text(
                            text = "No streams available right now.",
                            color = NuvioTheme.colors.TextSecondary,
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(sources, key = { it.streamNo }) { source ->
                                Card(
                                    onClick = { playingUrl = source.embedUrl },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.colors(
                                        containerColor = NuvioTheme.colors.BackgroundCard
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = NuvioTheme.colors.TextPrimary,
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Stream ${source.streamNo}",
                                                fontWeight = FontWeight.SemiBold,
                                                color = NuvioTheme.colors.TextPrimary,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            if (source.language.isNotBlank()) {
                                                Text(
                                                    text = source.language,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = NuvioTheme.colors.TextSecondary,
                                                )
                                            }
                                            if (source.viewers > 0) {
                                                Text(
                                                    text = "${source.viewers} watching",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NuvioTheme.colors.TextSecondary,
                                                )
                                            }
                                        }
                                        if (source.hd) {
                                            Text(
                                                text = "HD",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Red,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // Match list
                Column(modifier = Modifier.fillMaxSize()) {
                    if (showBuiltInHeader) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp, vertical = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.LiveTv,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = NuvioTheme.colors.TextPrimary,
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(LiveApi.categories) { category ->
                            val selected = selectedCategory == category
                            Button(
                                onClick = { selectedCategory = category },
                                colors = ButtonDefaults.colors(
                                    containerColor = if (selected)
                                        NuvioTheme.colors.TextPrimary
                                    else
                                        NuvioTheme.colors.SurfaceVariant,
                                    contentColor = if (selected)
                                        NuvioTheme.colors.Background
                                    else
                                        NuvioTheme.colors.TextPrimary,
                                ),
                            ) {
                                Text(
                                    text = category.replaceFirstChar { it.uppercase() },
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (loading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading live matches...",
                                color = NuvioTheme.colors.TextSecondary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    } else if (matches.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No live matches right now.",
                                color = NuvioTheme.colors.TextSecondary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(matches, key = { it.id }) { match ->
                                val itemModifier = if (matches.indexOf(match) == 0) {
                                    Modifier.focusRequester(firstItemFocusRequester)
                                } else {
                                    Modifier
                                }
                                Card(
                                    onClick = { selectedMatch = match },
                                    modifier = itemModifier.fillMaxWidth(),
                                    colors = CardDefaults.colors(
                                        containerColor = NuvioTheme.colors.BackgroundCard
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (match.poster.isNotBlank()) {
                                            AsyncImage(
                                                model = match.poster,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(96.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(96.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(NuvioTheme.colors.SurfaceVariant),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    Icons.Filled.LiveTv,
                                                    contentDescription = null,
                                                    tint = NuvioTheme.colors.TextSecondary,
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(20.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = match.title,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = NuvioTheme.colors.TextPrimary,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color.Red, RoundedCornerShape(4.dp)),
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "LIVE",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.Red,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = "Play",
                                            tint = NuvioTheme.colors.TextSecondary,
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LiveWebViewPlayer(
    url: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                destroy()
            }
            webView = null
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onBack,
                contentPadding = PaddingValues(12.dp),
                colors = ButtonDefaults.colors(
                    containerColor = NuvioTheme.colors.SurfaceVariant
                )
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NuvioTheme.colors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
            )
        }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    loadUrl(url)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
