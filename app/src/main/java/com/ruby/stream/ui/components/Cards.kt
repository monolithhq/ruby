package com.ruby.stream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ruby.stream.ui.theme.RubyDimens
import com.ruby.stream.ui.theme.RubyMotion
import com.ruby.stream.ui.theme.RubyRadius
import com.ruby.stream.ui.theme.RubySpacing
import com.ruby.stream.ui.theme.RubyType
import com.ruby.stream.ui.theme.StatusError
import com.ruby.stream.ui.theme.StatusSuccess
import com.ruby.stream.ui.theme.StatusWarning
import com.ruby.stream.ui.theme.Surface1
import com.ruby.stream.ui.theme.TextPrimary
import com.ruby.stream.ui.theme.TextSecondary

/**
 * Internal base for all media cards: rounded thumbnail, tap handling,
 * optional badge overlay. PosterCard/LandscapeCard/EpisodeCard/DownloadCard
 * are thin wrappers around this so sizing/behavior stays consistent and
 * changes only need to happen in one place.
 */
@Composable
private fun RubyMediaCard(
    imageUrl: String?,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    badge: (@Composable BoxScope.() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.96f else 1f

    Column(modifier = modifier.width(width)) {
        Box(
            modifier = Modifier
                .size(width, height)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(RoundedCornerShape(RubyRadius.sm))
                .background(Surface1)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            badge?.invoke(this)
        }
        footer?.invoke()
    }
}

@Composable
fun PosterCard(
    imageUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    qualityLabel: String? = null
) {
    RubyMediaCard(
        imageUrl = imageUrl,
        width = RubyDimens.PosterCardWidth,
        height = RubyDimens.PosterCardHeight,
        onClick = onClick,
        modifier = modifier,
        contentDescription = title,
        badge = qualityLabel?.let { label ->
            {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(RubySpacing.xxs)
                        .clip(RoundedCornerShape(RubyRadius.xs))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = RubySpacing.xxs, vertical = 2.dp)
                ) {
                    Text(text = label, style = RubyType.Label, color = TextPrimary)
                }
            }
        }
    )
}

@Composable
fun LandscapeCard(
    imageUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RubyMediaCard(
        imageUrl = imageUrl,
        width = RubyDimens.LandscapeCardWidth,
        height = RubyDimens.LandscapeCardHeight,
        onClick = onClick,
        modifier = modifier,
        contentDescription = title
    )
}

@Composable
fun ContinueWatchingCard(
    imageUrl: String?,
    title: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RubyMediaCard(
        imageUrl = imageUrl,
        width = RubyDimens.ContinueWatchingCardWidth,
        height = RubyDimens.ContinueWatchingCardHeight,
        onClick = onClick,
        modifier = modifier,
        contentDescription = title,
        badge = {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(RubyDimens.ContinueWatchingProgressHeight),
                color = com.ruby.stream.ui.theme.Crimson,
                trackColor = Color.Black.copy(alpha = 0.4f)
            )
        }
    )
}

@Composable
fun EpisodeCard(
    imageUrl: String?,
    episodeNumber: Int,
    title: String,
    durationLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RubyMediaCard(
        imageUrl = imageUrl,
        width = RubyDimens.EpisodeCardWidth,
        height = RubyDimens.EpisodeCardHeight,
        onClick = onClick,
        modifier = modifier,
        contentDescription = title,
        footer = {
            Column(modifier = Modifier.padding(top = RubySpacing.xxs)) {
                Text(
                    text = "Episode $episodeNumber · $title",
                    style = RubyType.Caption,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = durationLabel,
                    style = RubyType.Caption,
                    color = TextSecondary
                )
            }
        }
    )
}

enum class DownloadStatus { Downloading, Complete, Failed }

@Composable
fun DownloadCard(
    imageUrl: String?,
    title: String,
    status: DownloadStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadProgress: Float = 0f
) {
    RubyMediaCard(
        imageUrl = imageUrl,
        width = RubyDimens.DownloadCardWidth,
        height = RubyDimens.DownloadCardHeight,
        onClick = onClick,
        modifier = modifier,
        contentDescription = title,
        badge = {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(RubySpacing.xxs)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                when (status) {
                    DownloadStatus.Downloading -> CircularProgressIndicator(
                        progress = { downloadProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = TextPrimary
                    )
                    DownloadStatus.Complete -> Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = StatusSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    DownloadStatus.Failed -> Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Download failed",
                        tint = StatusError,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    )
}
