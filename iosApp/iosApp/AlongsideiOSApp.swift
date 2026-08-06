import GoogleSignIn
import SwiftUI
import app
import os

// NSLog's dynamic (%@) content gets redacted as `<private>` in Console.app on a real device
// (iOS's unified logging privacy default, confirmed live 2026-08-06) - os.Logger with an explicit
// `privacy: .public` on every interpolated value is the only way to actually see this off-device.
private let appLogger = Logger(subsystem: "com.alongside.max", category: "AlongsideApp")

@main
struct AlongsideiOSApp: App {
    private let googleAuthProvider = GoogleSignInAuthProvider()
    @Environment(\.scenePhase) private var scenePhase

    private static let appGroupSuiteName = "group.com.alongside.max"
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
        appLogger.log("init (cold start)")
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
            appLogger.log("scenePhase changed to \(String(describing: newPhase), privacy: .public)")
            if newPhase == .active {
                consumeSharedText()
            }
        }
    }

    private func consumeSharedText() {
        guard let defaults = UserDefaults(suiteName: Self.appGroupSuiteName) else {
            appLogger.log(
                "UserDefaults(suiteName: \(Self.appGroupSuiteName, privacy: .public)) is NIL - app group entitlement missing/not provisioned"
            )
            return
        }
        let text = defaults.string(forKey: Self.pendingShareTextKey)
        appLogger.log("consumeSharedText: pendingShareText = \(text ?? "nil", privacy: .public)")
        guard let text else { return }
        defaults.removeObject(forKey: Self.pendingShareTextKey)
        IosPendingShareTextKt.setIosPendingShareText(text: text)
        appLogger.log("setIosPendingShareText called with received text")
    }
}
