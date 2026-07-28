import GoogleSignIn
import SwiftUI
import app

@main
struct AlongsideiOSApp: App {
    private let googleAuthProvider = GoogleSignInAuthProvider()
    @Environment(\.scenePhase) private var scenePhase

    private static let appGroupSuiteName = "group.com.alongside.app"
    private static let pendingShareTextKey = "pendingShareText"

    init() {
        // Kotlin/Native's ObjC export renames any `init*`-named function to `doInit*` - `init` is
        // a reserved initializer convention in ObjC/Swift, so this avoids colliding with it.
        MainViewControllerKt.doInitKoin()
        // No GoogleService-Info.plist (this project talks to Firebase over REST, not the native
        // SDK - CLAUDE.md ADR #3), so GIDSignIn needs its client ID set explicitly rather than
        // auto-discovered.
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: "965813009948-3eevg32ptnnth9kdtit8sin87tg0oq1r.apps.googleusercontent.com"
        )
        consumeSharedText()
    }

    var body: some Scene {
        WindowGroup {
            ContentView(googleAuthProvider: googleAuthProvider)
                .onOpenURL { url in
                    // The OAuth redirect back into the app after signIn(withPresenting:) - GIDSignIn
                    // needs this to complete the flow.
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
        .onChange(of: scenePhase) { newPhase in
            // Mirrors Android's onCreate + onNewIntent dual entry points - a warm relaunch (app
            // already running, user shares again, returns to it) needs this too, not just cold start.
            if newPhase == .active {
                consumeSharedText()
            }
        }
    }

    private func consumeSharedText() {
        guard let defaults = UserDefaults(suiteName: Self.appGroupSuiteName),
              let text = defaults.string(forKey: Self.pendingShareTextKey) else { return }
        defaults.removeObject(forKey: Self.pendingShareTextKey)
        IosPendingShareTextKt.setIosPendingShareText(text: text)
    }
}
