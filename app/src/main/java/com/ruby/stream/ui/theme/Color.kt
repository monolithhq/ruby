package com.ruby.stream.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary (Brand) ─────────────────────────────────────
val Crimson         = Color(0xFFB0060F)  // Brand — logo, active states, key actions
val CrimsonHover    = Color(0xFFC70A15)  // Buttons — hover/focus state
val CrimsonPressed  = Color(0xFF8F040C)  // Pressed/active touch state

// ── Neutral (Surfaces) ───────────────────────────────────
val Black       = Color(0xFF000000)  // App background
val Surface1    = Color(0xFF111111)  // Cards, elevated content rows
val Surface2    = Color(0xFF1B1B1B)  // Modals, bottom sheets
val Surface3    = Color(0xFF242424)  // Highest elevation (dialogs, popovers)
val Divider     = Color(0xFF303030)  // Hairlines, separators

// ── Text ──────────────────────────────────────────────────
val TextPrimary   = Color(0xFFFFFFFF)  // Titles, primary content
val TextSecondary = Color(0xFFB5B5B5)  // Descriptions, metadata
val TextDisabled  = Color(0xFF6A6A6A)  // Disabled labels, placeholder text

// ── Status ────────────────────────────────────────────────
// Used for: add-on health, download states, error/offline banners
val StatusSuccess = Color(0xFF22C55E)  // Healthy add-on, completed download
val StatusWarning = Color(0xFFF59E0B)  // Degraded/slow add-on, low storage
val StatusError   = Color(0xFFEF4444)  // Failed stream, add-on error, offline
val StatusInfo    = Color(0xFF3B82F6)  // Neutral notices, background refresh indicator
