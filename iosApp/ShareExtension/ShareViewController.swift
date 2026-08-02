import UIKit
import UniformTypeIdentifiers
import UserNotifications

/// The Share Extension's entire job (docs/roadmap.md M7): pull the shared URL/text out of the
/// extension item, hand it to the main app via the shared App Group container, schedule a local
/// notification, and dismiss immediately - no visible UI of its own (deliberately not
/// `SLComposeServiceViewController`, which would show Apple's "compose a post" chrome).
///
/// The notification, not the extension's own UI, is what gets the user back into Alongside:
/// there is no supported way for a Share Extension to open its containing app directly
/// (`NSExtensionContext.open(_:)` is Today/widget-extension only per Apple's docs, confirmed live
/// 2026-08-02 - the call flashes an "Open in Alongside?" prompt that never completes, matching
/// openradar rdar://17551744; the classic `UIResponder`-chain `openURL:` private-API hack also
/// confirmed live to find no reachable `UIApplication` instance from this process). Tapping a
/// local notification, by contrast, is a fully standard, always-reliable way to foreground an
/// app - `AlongsideiOSApp.swift`'s existing `scenePhase == .active` handler already picks up
/// `pendingShareText` and navigates to `PlaceImportScreen` on any foreground, notification-tap
/// included, so no extra plumbing is needed on the app side. The actual link resolve/import
/// pipeline (`PlaceImportPipeline`) deliberately still runs only in the main app, not duplicated
/// here - this extension stays plain Swift/UIKit, no KMP/Compose/Koin dependency (extensions are
/// separate, memory-constrained processes).
final class ShareViewController: UIViewController {
    private static let appGroupSuiteName = "group.com.alongside.app"
    private static let pendingShareTextKey = "pendingShareText"

    override func viewDidLoad() {
        super.viewDidLoad()
        extractSharedText { [weak self] text in
            if let text {
                UserDefaults(suiteName: Self.appGroupSuiteName)?.set(text, forKey: Self.pendingShareTextKey)
                self?.scheduleImportReadyNotification()
            }
            self?.extensionContext?.completeRequest(returningItems: nil)
        }
    }

    private func scheduleImportReadyNotification() {
        let content = UNMutableNotificationContent()
        content.title = "Місце готове до імпорту"
        content.body = "Натисни, щоб додати його в Alongside"
        content.sound = .default
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }

    private func extractSharedText(completion: @escaping (String?) -> Void) {
        guard let item = extensionContext?.inputItems.first as? NSExtensionItem,
              let provider = item.attachments?.first else {
            completion(nil)
            return
        }

        if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) {
            provider.loadItem(forTypeIdentifier: UTType.url.identifier) { result, _ in
                completion((result as? URL)?.absoluteString)
            }
        } else if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
            provider.loadItem(forTypeIdentifier: UTType.plainText.identifier) { result, _ in
                completion(result as? String)
            }
        } else {
            completion(nil)
        }
    }
}
