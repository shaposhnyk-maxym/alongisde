package com.alongside.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.alongside.app.capture.rememberPhotoPickerLauncher
import com.alongside.app.navigation.AlongsideNavDisplay
import com.alongside.app.navigation.Home
import com.alongside.app.navigation.Login
import com.alongside.app.navigation.MainShell
import com.alongside.app.navigation.MainTab
import com.alongside.app.navigation.MatchList
import com.alongside.app.navigation.Matcher
import com.alongside.app.navigation.Onboarding
import com.alongside.app.navigation.Pairing
import com.alongside.app.navigation.PlaceImport
import com.alongside.app.navigation.PlaceholderScreen
import com.alongside.app.navigation.Places
import com.alongside.app.navigation.Recap
import com.alongside.app.navigation.Settings
import com.alongside.app.navigation.Timeline
import com.alongside.core.domain.onboarding.OnboardingCompletionCache
import com.alongside.core.ui.component.AlongsideTextButton
import com.alongside.feature.auth.GoogleAuthProvider
import com.alongside.feature.auth.presentation.AuthContainer
import com.alongside.feature.auth.presentation.AuthScreen
import com.alongside.feature.auth.presentation.AuthSideEffect
import com.alongside.feature.diary.presentation.DiaryTimelineContainer
import com.alongside.feature.diary.presentation.DiaryTimelineIntent
import com.alongside.feature.diary.presentation.DiaryTimelineScreen
import com.alongside.feature.onboarding.PermissionController
import com.alongside.feature.onboarding.presentation.OnboardingContainer
import com.alongside.feature.onboarding.presentation.OnboardingScreen
import com.alongside.feature.onboarding.presentation.OnboardingSideEffect
import com.alongside.feature.pairing.presentation.PairingContainer
import com.alongside.feature.pairing.presentation.PairingScreen
import com.alongside.feature.pairing.presentation.PairingSideEffect
import com.alongside.feature.places.presentation.PlaceImportContainer
import com.alongside.feature.places.presentation.PlaceImportScreen
import com.alongside.feature.places.presentation.PlacesListContainer
import com.alongside.feature.places.presentation.PlacesListScreen
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject
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
                    subclass(PlaceImport::class)
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
 * The back stack always cold-starts at [Login], but each gate advances past itself the moment
 * its own condition is already satisfied: a cached session skips straight past Login,
 * [OnboardingCompletionCache] (checked here, once, right after sign-in) skips Onboarding once
 * it's ever been completed, and an existing trip record skips Pairing - so a normal relaunch
 * lands on [Home], not back at the start of the gate.
 *
 * [googleAuthProvider] and [permissionController] are the two Activity/platform-bound seams
 * the auth gate needs - constructed by the platform entry point (MainActivity / iOS host)
 * and passed down, the same wiring the pre-graph placeholders used.
 *
 * [pendingShareText] is the raw `ACTION_SEND` text the platform entry point is currently holding
 * (cold start via `getIntent()`, or a warm-restart update via `onNewIntent`) - every distinct
 * non-null value pushes a [PlaceImport] card on top of whatever's on screen, then
 * [onShareTextConsume] clears it so a later, different share can trigger the effect again.
 */
@Composable
public fun AlongsideApp(
    googleAuthProvider: GoogleAuthProvider,
    permissionController: PermissionController,
    modifier: Modifier = Modifier,
    pendingShareText: String? = null,
    onShareTextConsume: () -> Unit = {},
) {
    val backStack =
        rememberNavBackStack(
            configuration = NavKeySavedStateConfiguration,
            elements = arrayOf(Login),
        )

    LaunchedEffect(pendingShareText) {
        pendingShareText?.let { text ->
            backStack.add(PlaceImport(text))
            onShareTextConsume()
        }
    }

    AlongsideNavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Login> {
                    val container = koinViewModel<AuthContainer> { parametersOf(googleAuthProvider) }
                    val onboardingCompletionCache = koinInject<OnboardingCompletionCache>()
                    val scope = rememberCoroutineScope()
                    container.collectSideEffect { effect ->
                        if (effect is AuthSideEffect.SignedIn) {
                            scope.launch {
                                val target = if (onboardingCompletionCache.isCompleted()) Pairing else Onboarding
                                backStack.resetTo(target)
                            }
                        }
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
                    val container = koinViewModel<PairingContainer>()
                    container.collectSideEffect { effect ->
                        if (effect is PairingSideEffect.Paired) backStack.resetTo(Home)
                    }
                    PairingScreen(container)
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
                        val container = koinViewModel<DiaryTimelineContainer>()
                        var captureDate by remember { mutableStateOf<LocalDate?>(null) }
                        val launchPhotoPicker =
                            rememberPhotoPickerLauncher { uris ->
                                // Cleared immediately after use (not left holding the last value
                                // forever) so a later, unrelated event can never misattribute
                                // photos to a stale date - the underlying system picker is modal,
                                // so a second "Add Photos" tap can't race this in practice, but
                                // there's no reason to leave a stale date sitting in state either.
                                captureDate?.let { date ->
                                    container.onIntent(DiaryTimelineIntent.ProcessCapturedPhotos(date, uris))
                                }
                                captureDate = null
                            }
                        DiaryTimelineScreen(
                            container,
                            onAddPhotos = { date ->
                                captureDate = date
                                launchPhotoPicker()
                            },
                        )
                    }
                }
                entry<Places> {
                    MainTabScreen(tab = MainTab.PLACES, backStack = backStack) {
                        // Manual add/edit/delete is M16's job - this is the read-only list, city-
                        // grouped, synced from Firebase with Room as the source of truth. The
                        // incomplete-photo retry loop moved into PlacesListContainer's own
                        // onCreate (see PlaceRetryDataSource's kdoc for its documented gap).
                        val container = koinViewModel<PlacesListContainer>()
                        PlacesListScreen(container)
                    }
                }
                entry<PlaceImport> { placeImport ->
                    val container = koinViewModel<PlaceImportContainer> { parametersOf(placeImport.shareText) }
                    PlaceImportScreen(
                        container = container,
                        onImport = { backStack.removeLastOrNull() },
                        onDiscard = { backStack.removeLastOrNull() },
                    )
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
