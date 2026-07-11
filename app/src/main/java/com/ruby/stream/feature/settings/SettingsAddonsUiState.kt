package com.ruby.stream.feature.settings

import com.ruby.stream.data.database.entity.AddonHealth

/**
 * PASS 5 — SettingsAddons's state (Session 14, AD-018).
 *
 * Confirmed against the actual repo before designing: InstalledAddonDao.
 * observeAll() already exists as a reactive Flow<List<InstalledAddonEntity>>;
 * InstalledAddonEntity already has enabled, health, and familyFriendly
 * (AD-011). Nothing here invents new infrastructure -- same "observe a
 * Flow, derive UI from it" pattern already established for
 * SettingsAppearance/SettingsNetwork.
 *
 * SCOPE: this is the reactive add-on MANAGEMENT LIST only. The install/
 * update flow (AddonInstall) is EXPLICITLY DEFERRED as its own future
 * destination, not designed this pass -- it crosses several
 * architectural layers the list screen never touches (manifest URL entry
 * -> HTTP fetch -> manifest parsing -> validation -> repository
 * persistence -> InstalledAddonEntity creation/update ->
 * InstalledCatalogEntity population via replaceForAddon()). Modeling
 * both screens together would produce exactly the oversized,
 * mixed-responsibility UiState Sessions 10-11 deliberately moved away
 * from. Install and Update will be the SAME future workflow,
 * parameterized by mode (AddonInstallMode { INSTALL, UPDATE }), not two
 * separate designs -- same pattern as ChangePin's PinAuthority.
 *
 * Delete cascades to the add-on's InstalledCatalogEntity rows
 * automatically via the existing ForeignKey.CASCADE -- no manual cleanup
 * needed in the repository. The delete CONFIRMATION dialog is triggered
 * by the ViewModel via a platform dialog, NOT modeled as UiState here.
 *
 * Loading earned per AD-015's rule -- live Flow, remains reactive after
 * first emission, same consumption pattern as Appearance/Network.
 */
sealed interface SettingsAddonsUiState {
    data object Loading : SettingsAddonsUiState

    data class Content(
        val addons: List<InstalledAddonItem>,
    ) : SettingsAddonsUiState
}

data class InstalledAddonItem(
    val id: Long,
    val name: String,
    val enabled: Boolean,
    val health: AddonHealth,
    val familyFriendly: Boolean,
)
