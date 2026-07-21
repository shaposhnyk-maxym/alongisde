package com.alongside.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every destination from `docs/navigation-flow.mermaid`, one [NavKey] each.
 *
 * The auth gate (`Login → Onboarding → Pairing`) is a one-time sequential stack - each
 * transition replaces the whole back stack so Back never returns into a completed gate step.
 * The five main-app tabs ([Home], [Timeline], [Places], [Matcher], [MatchList]) swap in place
 * at the top of the stack; [Settings] and [Recap] push on top of whatever tab is current.
 */
@Serializable
public data object Login : NavKey

@Serializable
public data object Onboarding : NavKey

/** Create/Join choice + invite-code screens (`feature:pairing`, M8). */
@Serializable
public data object Pairing : NavKey

@Serializable
public data object Home : NavKey

@Serializable
public data object Timeline : NavKey

@Serializable
public data object Places : NavKey

/**
 * The share-link import confirmation card (`feature:places`, M13.2) - pushed on top of whatever
 * tab is current when the app is launched (or resumed) via `ACTION_SEND`. Carries the raw shared
 * text rather than an already-extracted URL: `PlaceImportContainer` does that extraction itself
 * (see `extractShareUrl`), the same reasoning as passing platform-sourced input to a Container
 * via `parametersOf` elsewhere in this graph.
 */
@Serializable
public data class PlaceImport(
    val shareText: String,
) : NavKey

@Serializable
public data object Matcher : NavKey

@Serializable
public data object MatchList : NavKey

@Serializable
public data object Settings : NavKey

@Serializable
public data object Recap : NavKey
