package com.ruby.stream.ui.theme

// ── Motion Timings (ms) ─────────────────────────────────────
// Reference for all screen transitions, dialogs, and micro-interactions.
// Use these constants rather than hardcoding durations in individual composables.
object RubyMotion {
    const val ScreenPush = 250
    const val Fade = 180
    const val BottomSheet = 320
    const val Dialog = 180
    const val CardExpansion = 250
    const val HeroTransition = 350
    const val PosterClick = 120
    const val Ripple = 150

    // Launch sequence ("Forge") — see /docs/motion/forge-sequence.md for full storyboard
    const val LaunchBlackout = 250
    const val LaunchCrimsonEnergy = 650
    const val LaunchLogoAssembly = 900
    const val LaunchInternalGlow = 400
    const val LaunchCinematicSweep = 250
    const val LaunchScreenTransition = 350
    // Total: ~2800ms
}

// ── Loading Pattern Rule ─────────────────────────────────────
// Page-level loading (Home rows, Search results, Title Details): ALWAYS skeleton/shimmer.
// Spinners are reserved for discrete actions only: PIN verification, add-on install,
// download start confirmation. Never use a spinner for page/content loading.
