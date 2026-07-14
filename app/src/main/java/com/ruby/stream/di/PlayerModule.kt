package com.ruby.stream.di

import com.ruby.stream.core.player.DefaultPlayerController
import com.ruby.stream.core.player.PlayerController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * PASS 6 (AD-027) — player-domain Hilt bindings, grouped by feature
 * rather than by Hilt component so any future player-specific
 * bindings have an obvious home. Both bindings @ViewModelScoped /
 * installed in ViewModelComponent (NOT SingletonComponent) per AD-00U
 * (locked since Session 5): PlayerController must be @ViewModelScoped,
 * never @Singleton, so a fresh instance (and underlying ExoPlayer) is
 * created per ViewModel rather than shared/leaked across screens.
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class PlayerModule {

    @Binds
    @ViewModelScoped
    abstract fun bindPlayerController(
        impl: DefaultPlayerController,
    ): PlayerController

    companion object {
        /**
         * DefaultPlayerController's controllerScope, driving its
         * internal ~500ms polling loop. Dispatchers.Main.immediate,
         * confirmed against Media3's own threading model docs: an
         * ExoPlayer instance must be accessed from the single
         * application thread it was created on -- the main thread
         * here, since ExoPlayer.Builder(context).build() supplies no
         * explicit Looper. exoPlayer.currentPosition/.duration reads
         * inside the polling loop are direct Player-interface property
         * reads, still bound by that same single-thread contract.
         *
         * NOT relied upon to be cancelled by Hilt/ViewModelComponent
         * teardown -- a CoroutineScope is just another injected object
         * to Hilt, not a Closeable it knows how to clean up. Explicitly
         * cancelled instead in DefaultPlayerController.release(). A
         * fresh SupervisorJob per call keeps this genuinely
         * per-ViewModel rather than an accidental singleton.
         */
        @Provides
        @ViewModelScoped
        @PlayerControllerScope
        fun provideControllerScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
