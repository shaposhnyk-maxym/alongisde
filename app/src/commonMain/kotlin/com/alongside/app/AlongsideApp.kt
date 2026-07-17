package com.alongside.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.alongside.app.navigation.AlongsideNavDisplay
import com.alongside.app.navigation.Home
import com.alongside.app.navigation.Login
import com.alongside.app.navigation.MainShell
import com.alongside.app.navigation.MainTab
import com.alongside.app.navigation.MatchList
import com.alongside.app.navigation.Matcher
import com.alongside.app.navigation.Onboarding
import com.alongside.app.navigation.Pairing
import com.alongside.app.navigation.PlaceholderScreen
import com.alongside.app.navigation.Places
import com.alongside.app.navigation.Recap
import com.alongside.app.navigation.Settings
import com.alongside.app.navigation.Timeline
import com.alongside.core.ui.component.AlongsideTextButton
import com.alongside.feature.auth.GoogleAuthProvider
import com.alongside.feature.auth.presentation.AuthContainer
import com.alongside.feature.auth.presentation.AuthScreen
import com.alongside.feature.auth.presentation.AuthSideEffect
import com.alongside.feature.onboarding.PermissionController
import com.alongside.feature.onboarding.presentation.OnboardingContainer
import com.alongside.feature.onboarding.presentation.OnboardingScreen
import com.alongside.feature.onboarding.presentation.OnboardingSideEffect
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectSideEffect

/**
 * rememberNavBackStack saves/restores the stack through SavedState serialization, which
 * needs every concrete [NavKey] registered for open polymorphism - forgetting a new
 * destination here crashes on first composition, not silently.
 */
private val NavKeySavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Login::class)
                    subclass(Onboarding::class)
                    subclass(Pairing::class)
                    subclass(Home::class)
                    subclass(Timeline::class)
                    subclass(Places::class)
                    subclass(Matcher::class)
                    subclass(MatchList::class)
                    subclass(Settings::class)
                    subclass(Recap::class)
                }
            }
    }

/**
 * The Navigation 3 backbone from `docs/navigation-flow.mermaid`: the one-time auth gate
 * (Login → Onboarding → Pairing) followed by the five-tab main app with Settings and Recap
 * stacked on top. Destinations whose feature module hasn't landed yet render
 * [PlaceholderScreen]s, so the whole graph is walkable today and features slot into their
 * entries milestone by milestone.
 *
 * [googleAuthProvider] and [permissionController] are the two Activity/platform-bound seams
 * the auth gate needs - constructed by the platform entry point (MainActivity / iOS host)
 * and passed down, the same wiring the pre-graph placeholders used.
 */
@Composable
public fun AlongsideApp(
    googleAuthProvider: GoogleAuthProvider,
    permissionController: PermissionController,
    modifier: Modifier = Modifier,
) {
    val backStack =
        rememberNavBackStack(
            configuration = NavKeySavedStateConfiguration,
            elements = arrayOf(Login),
        )

    AlongsideNavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Login> {
                    val container = koinViewModel<AuthContainer> { parametersOf(googleAuthProvider) }
                    container.collectSideEffect { effect ->
                        if (effect is AuthSideEffect.SignedIn) backStack.resetTo(Onboarding)
                    }
                    AuthScreen(container)
                }
                entry<Onboarding> {
                    val container = koinViewModel<OnboardingContainer> { parametersOf(permissionController) }
                    container.collectSideEffect { effect ->
                        if (effect is OnboardingSideEffect.Completed) backStack.resetTo(Pairing)
                    }
                    OnboardingScreen(container)
                }
                entry<Pairing> {
                    PlaceholderScreen(
                        title = "Every trip needs two",
                        note = "Create a trip or join your partner's - feature:pairing lands in M7.",
                        actionLabel = "Continue to Home",
                        onAction = { backStack.resetTo(Home) },
                    )
                }
                entry<Home> {
                    MainTabScreen(tab = MainTab.HOME, backStack = backStack) {
                        PlaceholderScreen(
                            title = "Home",
                            note = "Countdown to the meeting, today's diary day and fresh matches will gather here.",
                        ) {
                            AlongsideTextButton(text = "Settings", onClick = { backStack.add(Settings) })
                            AlongsideTextButton(text = "Recap", onClick = { backStack.add(Recap) })
                        }
                    }
                }
                entry<Timeline> {
                    MainTabScreen(tab = MainTab.TIMELINE, backStack = backStack) {
                        PlaceholderScreen(
                            title = "Timeline",
                            note = "The day-by-day carousel of your shared diary - feature:diary.",
                        )
                    }
                }
                entry<Places> {
                    MainTabScreen(tab = MainTab.PLACES, backStack = backStack) {
                        PlaceholderScreen(
                            title = "Places",
                            note = "Spots shared from Google Maps land here - feature:places.",
                        )
                    }
                }
                entry<Matcher> {
                    MainTabScreen(tab = MainTab.MATCHER, backStack = backStack) {
                        PlaceholderScreen(
                            title = "Matcher",
                            note = "Swipe on places until you both say yes - feature:matcher.",
                        )
                    }
                }
                entry<MatchList> {
                    MainTabScreen(tab = MainTab.MATCH_LIST, backStack = backStack) {
                        PlaceholderScreen(
                            title = "Matches",
                            note = "Every place you both said yes to - feature:matcher.",
                        )
                    }
                }
                entry<Settings> {
                    PlaceholderScreen(
                        title = "Settings",
                        note = "Profile, permissions, leave or delete trip - feature:settings.",
                    )
                }
                entry<Recap> {
                    PlaceholderScreen(
                        title = "Recap",
                        note = "The trip's story, told Stories-style - feature:recap.",
                    )
                }
            },
    )
}

/** Auth-gate transitions burn the bridge behind them: Back never re-enters a passed step. */
private fun NavBackStack<NavKey>.resetTo(key: NavKey) {
    clear()
    add(key)
}

@Composable
private fun MainTabScreen(
    tab: MainTab,
    backStack: NavBackStack<NavKey>,
    content: @Composable () -> Unit,
) {
    MainShell(
        currentTab = tab,
        // Tabs swap in place at the top of the stack - lateral moves, not a growing history.
        onTabSelect = { selected -> backStack[backStack.lastIndex] = selected.key },
        content = content,
    )
}
