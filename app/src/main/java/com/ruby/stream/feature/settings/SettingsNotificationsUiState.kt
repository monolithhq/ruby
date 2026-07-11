package com.ruby.stream.feature.settings

/**
 * PASS 5 — SettingsNotifications's state (Session 15, no AD entry
 * spent -- no owned subsystem exists to architecturally lock).
 *
 * Confirmed no notification-producing subsystem exists anywhere in the
 * repo -- no WorkManager/CoroutineWorker infrastructure at all, no
 * download-execution engine (despite DownloadEntity/DownloadDao
 * existing), no background health-check scheduler, no library-sync
 * worker. There is currently NO notification source, so there is
 * nothing for a preference toggle to govern. This is the FIFTH
 * validation of "a setting should only exist if Ruby owns the subsystem
 * it configures" (after Playback, Network, Storage, Device), but the
 * FIRST to conclude that ZERO settings survive the test, not just some.
 *
 * Purely informational -- no Loading (nothing to await), no fields, no
 * toggles. Exists only so the SettingsNotifications route resolves to
 * something in the meantime.
 *
 * The one notification Ruby is likely to eventually own: Download
 * Completed -- a direct consequence of a foreground user action, once a
 * download-execution engine exists. Natural ownership boundary when
 * that day comes: DownloadManager -> download completed ->
 * NotificationManager. Deferred, not designed here -- revisit whenever
 * a download-execution engine (or any other background subsystem) is
 * actually designed.
 */
sealed interface SettingsNotificationsUiState {
    data object Content : SettingsNotificationsUiState
}
