import UIKit
import UniformTypeIdentifiers

/// The Share Extension's entire job (docs/roadmap.md M7): pull the shared URL/text out of the
/// extension item, hand it to the main app via the shared App Group container, and dismiss
/// immediately - no visible UI of its own (deliberately not `SLComposeServiceViewController`,
/// which would show Apple's "compose a post" chrome). This mirrors Android's share hand-off UX:
/// tapping "Alongside" in the share sheet just silently returns to the source app: the *main
/// app's* `PlaceImportScreen` is the real confirmation UI, shown next time Alongside is
/// foregrounded (see `IosPendingShareText.kt`/`AlongsideiOSApp.swift`).
final class ShareViewController: UIViewController {
    private static let appGroupSuiteName = "group.com.alongside.app"
    private static let pendingShareTextKey = "pendingShareText"

    override func viewDidLoad() {
        super.viewDidLoad()
        extractSharedText { [weak self] text in
            if let text {
                UserDefaults(suiteName: Self.appGroupSuiteName)?.set(text, forKey: Self.pendingShareTextKey)
            }
            self?.extensionContext?.completeRequest(returningItems: nil)
        }
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
