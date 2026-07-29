import GoogleSignIn
import UIKit
import app

/// Real GIDSignIn-backed `GoogleAuthProvider` (Swift-visible as `AuthGoogleAuthProvider` - see
/// `MainViewController.kt`'s kdoc for why this lives in Swift rather than Kotlin: the interface is
/// deliberately callback-based, not `suspend`, so a Swift class can conform to it directly.
final class GoogleSignInAuthProvider: AuthGoogleAuthProvider {
    func signIn(onResult: @escaping (any AuthGoogleSignInResult) -> Void) {
        guard let presenter = Self.rootViewController() else {
            onResult(GoogleSignInResultFactoryKt.googleSignInFailure(message: "No presenting view controller"))
            return
        }
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error {
                print("GoogleSignInAuthProvider.signIn error: \(error) (domain: \((error as NSError).domain), code: \((error as NSError).code))")
                onResult(GoogleSignInResultFactoryKt.googleSignInFailure(message: error.localizedDescription))
                return
            }
            guard let idToken = result?.user.idToken?.tokenString else {
                onResult(GoogleSignInResultFactoryKt.googleSignInFailure(message: "Missing ID token"))
                return
            }
            onResult(GoogleSignInResultFactoryKt.googleSignInSuccess(idToken: idToken))
        }
    }

    func signInSilently(onResult: @escaping (any AuthGoogleSignInResult) -> Void) {
        GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
            if let error {
                print("GoogleSignInAuthProvider.signInSilently error: \(error) (domain: \((error as NSError).domain), code: \((error as NSError).code))")
                onResult(GoogleSignInResultFactoryKt.googleSignInFailure(message: error.localizedDescription))
                return
            }
            guard let idToken = user?.idToken?.tokenString else {
                onResult(GoogleSignInResultFactoryKt.googleSignInFailure(message: "Missing ID token"))
                return
            }
            onResult(GoogleSignInResultFactoryKt.googleSignInSuccess(idToken: idToken))
        }
    }

    private static func rootViewController() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?.rootViewController
    }
}
