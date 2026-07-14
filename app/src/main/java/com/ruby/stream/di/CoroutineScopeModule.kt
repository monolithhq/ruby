package com.ruby.stream.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * PASS 6 (AD-027) — provides the application-lifetime CoroutineScope
 * used by AddonRepository's fire-and-forget add-on health write-back
 * (see AddonRepository's own doc comment, "application-lifetime
 * repositoryScope"). Qualifier declared separately in Qualifiers.kt.
 *
 * SupervisorJob so one failed child coroutine does not cancel the
 * whole scope and silently kill future write-backs for every other
 * add-on. Dispatchers.IO matches AddonRepository's own existing
 * repositoryScope.launch(Dispatchers.IO) call site.
 *
 * This scope is genuinely application-lifetime (never explicitly
 * cancelled) -- unlike @PlayerControllerScope (see PlayerModule.kt/
 * PlayerController.release()), there is no narrower owning lifecycle
 * to tie cancellation to; it is intentionally left running for the
 * process's lifetime, same as AddonRepository itself (@Singleton).
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
