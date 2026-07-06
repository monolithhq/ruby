package com.ruby.stream.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Kept as an enum, not a Boolean, deliberately -- leaves room for future
// profile types (e.g. TEEN) without another schema change, the same
// reasoning already applied to AddonHealth being an enum rather than a
// narrower Boolean. ADULT remains the default so every profile created
// before this field existed (and any new profile that doesn't specify
// otherwise) behaves exactly as it always has.
enum class ProfileType {
    ADULT,
    KIDS
}

// PIN protects casual/accidental access to THIS profile specifically --
// orthogonal to isOwner, which grants administrative permissions
// (Add-ons/Storage/Device settings). A profile can be owner AND have no
// PIN, non-owner AND have a PIN, etc. Do not conflate the two the way
// AddonHealth/enabled were once conflated on InstalledAddonEntity before
// being split (see SOT AD-00W) -- this field pair is deliberately kept
// separate from isOwner for the same reason.
//
// Threat model (locked): casual/accidental access by household members
// sharing one device, NOT offline database theft or a determined
// attacker. This justifies a plain salted SHA-256 hash rather than a
// slow KDF (BCrypt/Argon2/PBKDF2) -- see ProfileRepository for the
// hashing implementation. Both fields nullable together: pinHash == null
// means "no PIN configured" and is the only state UI needs to check to
// decide whether to prompt at all.
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val avatarUrl: String? = null,
    val isOwner: Boolean = false,

    // ADULT is the safe default -- a profile is only ever restricted by
    // explicit Owner action, never implicitly. profileType gates catalog
    // visibility (see InstalledAddonEntity.familyFriendly and
    // InstalledCatalogEntity.visibleToKids) independently of isOwner --
    // a KIDS profile may or may not be Owner, though in practice it
    // won't be. See AD-011.
    val profileType: ProfileType = ProfileType.ADULT,

    // Best-effort maturity ceiling, applied ONLY when an add-on's
    // metadata actually reports a rating -- this is explicitly NOT the
    // primary safety mechanism for KIDS profiles (see AD-011: Ruby's
    // add-ons are third-party and unverified, so this field cannot be
    // trusted alone the way Netflix's own curated ratings can be).
    // Null means no ceiling configured / not enforced.
    val contentRatingLevel: String? = null,

    // null = no PIN configured for this profile. Never store the raw PIN.
    val pinHash: String? = null,
    val pinSalt: String? = null,

    // Recovery phrase: Owner-only concept, used solely to let the Owner
    // recover from a forgotten PIN without a nuclear reset. NOT enforced
    // at the DB/entity level that only isOwner rows may populate this --
    // ProfileRepository is responsible for that policy, the same way it
    // already owns PIN policy. Left on every row (not a separate table)
    // for the same reason PIN fields are: exactly one repository, one
    // hash/salt/verify pattern, no parallel system to maintain. Never
    // store the raw phrase.
    val recoveryPhraseHash: String? = null,
    val recoveryPhraseSalt: String? = null
)
