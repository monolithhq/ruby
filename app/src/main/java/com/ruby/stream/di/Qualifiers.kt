package com.ruby.stream.di

import javax.inject.Qualifier

/**
 * PASS 6 (AD-027) — CoroutineScope qualifiers, kept in their own file
 * rather than declared inside RepositoryModule.kt/PlayerModule.kt.
 * Qualifiers are a type-level concept independent of any one module's
 * implementation -- keeping them here means either module could be
 * reorganized, split, or renamed later without moving the qualifier
 * declarations themselves, and any future consumer only needs to
 * import from one place.
 */

/** Application-lifetime CoroutineScope (Dispatchers.IO + SupervisorJob). See CoroutineScopeModule.kt. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** Per-PlayerController CoroutineScope (Dispatchers.Main.immediate + SupervisorJob), cancelled explicitly in DefaultPlayerController.release() -- NOT relied upon to be cancelled by Hilt/ViewModelComponent teardown, since a CoroutineScope is not Closeable and Hilt has no generic hook for cancelling one. See PlayerModule.kt and PlayerController.kt. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerControllerScope
