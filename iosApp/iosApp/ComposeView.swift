import SwiftUI
import app

/// Bridges the Compose Multiplatform UI (`MainViewController()` in the `app` KMP module,
/// `app/src/iosMain/kotlin/com/alongside/app/MainViewController.kt`) into SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    let googleAuthProvider: any AuthGoogleAuthProvider

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(googleAuthProvider: googleAuthProvider)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
