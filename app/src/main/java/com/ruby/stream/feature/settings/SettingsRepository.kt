package com.ruby.stream.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 6 infrastructure (Session 21, AD-024) — the single DataStore-
 * backed façade for every device-wide (not profile-scoped) setting.
 *
 * LOCKED PRINCIPLE (AD-024, not just this pass's scoping choice):
 * exposes only settings that correspond to already-designed,
 * already-locked product capabilities. A new setting is added when its
 * owning screen's behavior has actually been designed (a real AD entry
 * exists for it), never speculatively ahead of that design -- the same
 * "a setting should only exist if Ruby owns the subsystem it
 * configures" bar independently validated five times during PASS 5
 * (Playback, Network, Storage, Device, Notifications).
 *
 * Covers exactly AD-015 (theme) and AD-016 (cellularStreamingPolicy) as
 * of this revision -- the only two Settings screens whose design
 * requires a live, persisted, device-wide value today. Deliberately
 * does NOT cover Storage (read-only aggregate I/O, not a stored
 * preference), Device (one boolean, not yet wired to this repository),
 * Notifications (zero settings survive AD-024's own test), or
 * UpdatePolicy (AD-016 already deferred this explicitly to Add-ons' own
 * future design session). Future Settings screens EXTEND this same
 * interface rather than getting their own DataStore-backed repository,
 * per AD-016's original consolidation call ("ONE SettingsRepository
 * serves every device-wide settings screen").
 *
 * Does NOT store or enforce CellularStreamingPolicy's actual
 * consequences -- connectivity detection and enforcement are
 * deliberately separate (see AD-022, PlaybackPolicy). This repository
 * answers exactly one question per setting ("what is the current
 * persisted value / persist this new value"), the same narrow
 * single-responsibility contract already established for
 * ProfileRepository.verifyPin() (AD-010).
 */
interface SettingsRepository {
    /** Current app theme. Live -- reflects every persisted change. */
    val theme: Flow<AppTheme>

    /** Current cellular-streaming policy. Live, same contract as theme. */
    val cellularStreamingPolicy: Flow<CellularStreamingPolicy>

    /** Persists the new theme. Immediate-commit, no confirmation step. */
    suspend fun setTheme(theme: AppTheme)

    /**
     * Persists the new cellular-streaming policy. Immediate-commit, no
     * confirmation step -- this only changes the STORED policy; it has
     * no effect on any in-progress playback session (see AD-022).
     */
    suspend fun setCellularStreamingPolicy(policy: CellularStreamingPolicy)
}

private val Context.settingsDataStore by preferencesDataStore(name = "ruby_settings")

@Singleton
class DefaultSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val CELLULAR_STREAMING_POLICY = stringPreferencesKey("cellular_streaming_policy")
    }

    // ADULT-equivalent safe default for theme: SYSTEM, so a fresh
    // install (or a value predating this preference's existence)
    // matches device-wide platform behavior rather than forcing light
    // or dark -- nothing has been explicitly chosen yet.
    override val theme: Flow<AppTheme> =
        context.settingsDataStore.data.map { prefs ->
            prefs[Keys.THEME]?.let { stored ->
                runCatching { AppTheme.valueOf(stored) }.getOrNull()
            } ?: AppTheme.SYSTEM
        }

    // ALLOW is the safe default: matches Ruby's behavior before this
    // preference existed (no cellular restriction at all) -- a
    // preference must never retroactively become more restrictive for
    // an existing install than it already was.
    override val cellularStreamingPolicy: Flow<CellularStreamingPolicy> =
        context.settingsDataStore.data.map { prefs ->
            prefs[Keys.CELLULAR_STREAMING_POLICY]?.let { stored ->
                runCatching { CellularStreamingPolicy.valueOf(stored) }.getOrNull()
            } ?: CellularStreamingPolicy.ALLOW
        }

    override suspend fun setTheme(theme: AppTheme) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME] = theme.name
        }
    }

    override suspend fun setCellularStreamingPolicy(policy: CellularStreamingPolicy) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.CELLULAR_STREAMING_POLICY] = policy.name
        }
    }
}
