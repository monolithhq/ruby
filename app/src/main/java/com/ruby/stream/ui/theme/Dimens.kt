package com.ruby.stream.ui.theme

import androidx.compose.ui.unit.dp

// ── Spacing Scale ─────────────────────────────────────────
object RubySpacing {
    val xxs = 4.dp
    val xs  = 8.dp
    val sm  = 12.dp
    val md  = 16.dp
    val lg  = 24.dp
    val xl  = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp
}

// ── Corner Radius ─────────────────────────────────────────
object RubyRadius {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val xl  = 20.dp
}

// ── Touch Targets (accessibility minimum) ──────────────────
object RubyTouchTarget {
    val minimum = 48.dp
}

// ── Component-specific dimens (populated as components are built) ──
object RubyDimens {
    val PosterCardWidth = 120.dp
    val PosterCardHeight = 180.dp
    val LandscapeCardWidth = 220.dp
    val LandscapeCardHeight = 124.dp
    val ContinueWatchingProgressHeight = 3.dp
    // Continue-watching cards are visually identical in size to landscape
    // cards (thumbnail + progress bar overlay) — defined in terms of
    // LandscapeCardWidth/Height rather than as separate literals, so the
    // two can never silently drift apart if one is resized later.
    val ContinueWatchingCardWidth = LandscapeCardWidth
    val ContinueWatchingCardHeight = LandscapeCardHeight
    val EpisodeCardWidth = 280.dp
    val EpisodeCardHeight = 158.dp
    val DownloadCardWidth = 120.dp
    val DownloadCardHeight = 180.dp
    val BottomNavHeight = 64.dp
    val HeroBannerHeight = 480.dp
    val ChipHeight = RubyTouchTarget.minimum
}
