import SwiftUI
import app

struct ContentView: View {
    let googleAuthProvider: any AuthGoogleAuthProvider

    var body: some View {
        ComposeView(googleAuthProvider: googleAuthProvider)
            .ignoresSafeArea(.all)
    }
}
