package com.alongside.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Registered in androidApp's manifest (androidApp stays a thin Android-only shell per
 * docs/kmp-module-architecture.md; the actual FCM code lives here in `app`'s androidMain, since
 * that's where Koin/DI already lives, and is bundled into androidApp's APK regardless of which
 * module owns it).
 */
public class AlongsideFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO(M17): persist the token via FirestoreApi.upsertDocument(...) once the pushToken
        // domain model (core:model/core:domain/core:database) exists - see docs/roadmap.md M17.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // TODO(M17): render the notification (partner-day-ready / days-until-reunion payloads
        // per docs/roadmap.md M17) once the payload contract and notification UI are designed.
    }
}
