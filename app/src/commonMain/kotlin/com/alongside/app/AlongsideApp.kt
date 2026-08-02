package com.alongside.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.alongside.app.capture.rememberPhotoPickerLauncher
import com.alongside.app.home.HomeContainer
import com.alongside.app.home.HomeScreen
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
import com.alongside.app.navigation.Places
import com.alongside.app.navigation.Recap
import com.alongside.app.navigation.Settings
import com.alongside.app.navigation.Timeline
import com.alongside.core.domain.onboarding.OnboardingCompletionCache
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.auth.GoogleAuthProvider
import com.alongside.feature.auth.presentation.AuthContainer
import com.alongside.feature.auth.presentation.AuthScreen
import com.alongside.feature.auth.presentation.AuthSideEffect
import com.alongside.feature.diary.presentation.DiaryTimelineContainer
import com.alongside.feature.diary.presentation.DiaryTimelineIntent
import com.alongside.feature.diary.presentation.DiaryTimelineScreen
import com.alongside.feature.matcher.presentation.MatchListScreen
import com.alongside.feature.matcher.presentation.MatcherContainer
import com.alongside.feature.matcher.presentation.MatcherScreen
import com.alongside.feature.onboarding.PermissionController
import com.alongside.feature.onboarding.SharePlatform
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
import com.alongside.feature.recap.presentation.RecapContainer
import com.alongside.feature.recap.presentation.RecapScreen
import com.alongside.feature.settings.presentation.SettingsContainer
import com.alongside.feature.settings.presentation.SettingsScreen
import com.alongside.feature.settings.presentation.SettingsSideEffect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
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
 * stacked on top.
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
    sharePlatform: SharePlatform,
    modifier: Modifier = Modifier,
    pendingShareText: String? = null,
    onShareTextConsume: () -> Unit = {},
) {
    AlongsideTheme {
        val backStack =
            rememberNavBackStack(
                configuration = NavKeySavedStateConfiguration,
                elements = arrayOf(Login),
            )

        LaunchedEffect(pendingShareText) {
            pendingShareText?.let { text ->
                // Wait for the auth/onboarding/pairing gate to settle before pushing PlaceImport -
                // each gate step's own side effect does an unconditional backStack.resetTo(...) the
                // moment its condition is satisfied (e.g. session restore completing, already-paired
                // check), which would otherwise wipe out a PlaceImport entry pushed while a gate was
                // still resolving (confirmed live: a share landing during cold-start session restore
                // got silently discarded when SignedIn -> resetTo(Pairing) fired a moment later).
                // There's also nothing useful to import into before pairing anyway - PlaceImportContainer
                // itself requires an active trip.
                snapshotFlow { backStack.lastOrNull() }
                    .first { it != null && it !is Login && it !is Onboarding && it !is Pairing }
                backStack.add(PlaceImport(text))
                onShareTextConsume()
            }
        }

        // Only Login/Onboarding/Pairing/PlaceImport/Settings/Recap route through
        // AlongsideNavDisplay's entryProvider - the five tabs are rendered directly by
        // MainShell instead (docs/roadmap.md M21.4), so entryProvider is never actually asked
        // to resolve a tab key.
        val currentMainTab = MainTab.entries.firstOrNull { it.key == backStack.lastOrNull() }
        if (currentMainTab != null) {
            MainShell(
                currentTab = currentMainTab,
                onTabSelect = { selected -> backStack[backStack.lastIndex] = selected.key },
                modifier = modifier,
            ) { tab ->
                when (tab) {
                    MainTab.HOME -> HomeTabContent(backStack)
                    MainTab.TIMELINE -> TimelineTabContent()
                    MainTab.PLACES -> PlacesTabContent()
                    MainTab.MATCHER -> MatcherTabContent()
                    MainTab.MATCH_LIST -> MatchListTabContent()
                }
            }
        } else {
            AuthGateAndStackedScreens(
                backStack = backStack,
                googleAuthProvider = googleAuthProvider,
                permissionController = permissionController,
                sharePlatform = sharePlatform,
                modifier = modifier,
            )
        }
    }
}

/**
 * Everything outside the five main tabs: the one-time auth gate (Login/Onboarding/Pairing) and
 * the screens stacked on top of a tab (PlaceImport/Settings/Recap) - split out from
 * [AlongsideApp] purely to keep it under detekt's `CyclomaticComplexMethod` threshold once the
 * tab/non-tab branch was added (docs/roadmap.md M21.4), not because this is conceptually
 * separate from the rest of the nav graph.
 */
@Composable
private fun AuthGateAndStackedScreens(
    backStack: NavBackStack<NavKey>,
    googleAuthProvider: GoogleAuthProvider,
    permissionController: PermissionController,
    sharePlatform: SharePlatform,
    modifier: Modifier = Modifier,
) {
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
                    OnboardingScreen(container, platform = sharePlatform)
                }
                entry<Pairing> {
                    val container = koinViewModel<PairingContainer>()
                    container.collectSideEffect { effect ->
                        if (effect is PairingSideEffect.Paired) backStack.resetTo(Home)
                    }
                    PairingScreen(container)
                }
                entry<PlaceImport> { placeImport ->
                    // key = shareText: without a distinguishing key, koinViewModel() resolves by
                    // class name alone against this Activity's single ViewModelStore (Navigation3
                    // gives no per-entry ViewModelStoreOwner here) - every share after the first
                    // would silently get back the FIRST share's cached PlaceImportContainer,
                    // ignoring its own shareText entirely (confirmed live via debug logging).
                    val container =
                        koinViewModel<PlaceImportContainer>(key = placeImport.shareText) {
                            parametersOf(placeImport.shareText)
                        }
                    PlaceImportScreen(
                        container = container,
                        onImport = { backStack.removeLastOrNull() },
                        onDiscard = { backStack.removeLastOrNull() },
                    )
                }
                entry<Settings> {
                    val container = koinViewModel<SettingsContainer>()
                    container.collectSideEffect { effect ->
                        if (effect is SettingsSideEffect.LeftOrDeletedTrip) backStack.resetTo(Pairing)
                    }
                    SettingsScreen(container, onClose = { backStack.removeLastOrNull() })
                }
                entry<Recap> {
                    val container = koinViewModel<RecapContainer>()
                    RecapScreen(container, onFinish = { backStack.removeLastOrNull() })
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
private fun HomeTabContent(backStack: NavBackStack<NavKey>) {
    val homeContainer = koinViewModel<HomeContainer>()
    val homeState by homeContainer.collectAsState()
    HomeScreen(
        state = homeState,
        onOpenSettings = { backStack.add(Settings) },
        onOpenRecap = { backStack.add(Recap) },
        onOpenTimeline = { backStack[backStack.lastIndex] = Timeline },
        onOpenMatches = { backStack[backStack.lastIndex] = MatchList },
    )
}

@Composable
private fun TimelineTabContent() {
    val container = koinViewModel<DiaryTimelineContainer>()
    var captureDate by remember { mutableStateOf<LocalDate?>(null) }
    val launchPhotoPicker =
        rememberPhotoPickerLauncher { uris ->
            // Cleared immediately after use (not left holding the last value forever) so a
            // later, unrelated event can never misattribute photos to a stale date - the
            // underlying system picker is modal, so a second "Add Photos" tap can't race this
            // in practice, but there's no reason to leave a stale date sitting in state either.
            captureDate?.let { date ->
                container.onIntent(DiaryTimelineIntent.ProcessCapturedPhotos(date, uris))
            }
            captureDate = null
        }
    // A second, independent launcher instance (not reused with the one above) - pre-trip
    // photos have no `date` to capture at tap-time, so there's no "which mode was the picker
    // in" ambiguity to guard against (docs/roadmap.md M19.8).
    val launchPreTripPhotoPicker =
        rememberPhotoPickerLauncher { uris ->
            container.onIntent(DiaryTimelineIntent.ProcessPreTripPhotos(uris))
        }
    DiaryTimelineScreen(
        container,
        onAddPhotos = { date ->
            captureDate = date
            launchPhotoPicker()
        },
        onAddPreTripPhotos = { launchPreTripPhotoPicker() },
    )
}

@Composable
private fun PlacesTabContent() {
    // Manual add/edit/delete is M16's job - this is the read-only list, city-grouped, synced
    // from Firebase with Room as the source of truth. The incomplete-photo retry loop moved
    // into PlacesListContainer's own onCreate (see PlaceRetryDataSource's kdoc for its
    // documented gap).
    val container = koinViewModel<PlacesListContainer>()
    PlacesListScreen(container)
}

@Composable
private fun MatcherTabContent() {
    val container = koinViewModel<MatcherContainer>()
    MatcherScreen(container)
}

@Composable
private fun MatchListTabContent() {
    val container = koinViewModel<MatcherContainer>()
    MatchListScreen(container)
}
