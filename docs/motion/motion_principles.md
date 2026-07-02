# Ruby — Motion Principles

## Core Rule
Motion should feel **heavy, precise, deliberate** — never bouncy or elastic.
No overshoot except the single intentional "settle" moment in the Forge
launch sequence. Everything else uses standard ease-in-out.

## Timing Reference
See `RubyMotion` object in `ui/theme/Motion.kt` for exact constants.

| Interaction         | Duration | Notes                                  |
|----------------------|----------|------------------------------------------|
| Screen push           | 250ms    | Standard forward navigation               |
| Fade                  | 180ms    | Cross-fade transitions, dialogs closing   |
| Bottom sheet           | 320ms    | Quality/audio/subtitle selectors          |
| Dialog                | 180ms    | PIN prompt, confirm-delete                |
| Card expansion         | 250ms    | Poster → Title Details shared-element     |
| Hero transition        | 350ms    | Home hero banner → Title Details          |
| Poster click (press)  | 120ms    | Scale-down feedback on tap                |
| Ripple                | 150ms    | Material ripple on all touch targets      |

## Loading Pattern (Hard Rule)
**Never use spinners for page/content loading.** Use skeleton screens with
shimmer instead — this applies to Home rows, Search results, Title Details,
Fetching Streams' per-add-on progress list.

Spinners are reserved ONLY for discrete, short actions:
- PIN verification
- Add-on install confirmation
- Download start confirmation

This choice isn't just aesthetic — it ties directly into the Startup Cache
design: since Home renders from a cached snapshot instantly, a page-level
spinner would actively contradict that "instant" feeling. Skeleton loading
is reserved for the rare case where nothing is cached yet (first-ever visit
to a screen).

## Launch Sequence ("Forge")
Full storyboard lives in this doc. Timing constants in `RubyMotion`
(Launch* prefixed). Total runtime ~2.8s. Not yet built as a real asset —
documented here so it can be implemented once UI is far enough along that
the final Phase 6 transition lands on the real Home screen (per the
"postpone the build, not the spec" decision).

## Reduced Motion
When the system Reduced Motion accessibility setting is on:
- Launch sequence collapses to a simple 400ms fade (no beam/assembly/sweep)
- Screen transitions drop to simple fades at 120ms
- Card expansion / hero transitions become instant cuts
- Skeleton shimmer becomes a static (non-animated) placeholder
